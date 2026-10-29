import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.db.session import SessionLocal
from app.models.models import User, Thought, Video, Job
from app.core.config import settings
from app.core.security import create_access_token
from conftest import get_or_create_test_user, get_test_auth_headers

client = TestClient(app)

def test_health_check_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["database"] == "connected"
    assert "ffmpeg_installed" in data

def test_unauthenticated_requests_strictly_blocked():
    """Confirms all protected endpoints return 401 Unauthorized without credentials"""
    endpoints = [
        ("GET", "/api/v1/dashboard"),
        ("GET", "/api/v1/settings"),
        ("GET", "/api/v1/youtube/status"),
        ("GET", "/api/v1/analytics"),
        ("GET", "/api/v1/thoughts"),
        ("GET", "/api/v1/videos"),
        ("GET", "/api/v1/auth/me"),
    ]
    for method, path in endpoints:
        res = client.request(method, path)
        assert res.status_code == 401, f"{path} did not reject unauthenticated access"
        assert "authentication required" in res.json()["detail"].lower() or "unauthorized" in res.json()["detail"].lower()

def test_dashboard_endpoint(auth_client):
    response = auth_client.get("/api/v1/dashboard")
    assert response.status_code == 200
    data = response.json()
    assert "total_thoughts" in data
    assert "total_videos" in data
    assert "upcoming_schedule" in data

def test_generate_thought_endpoint(auth_client):
    response = auth_client.post("/api/v1/thoughts/generate", json={
        "theme": "discipline",
        "tone": "inspiring",
        "language": "en"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["theme"] == "discipline"
    assert len(data["text"]) > 10
    assert "shorts" in data["hashtags"]

def test_settings_retrieval_and_update(auth_client):
    get_res = auth_client.get("/api/v1/settings")
    assert get_res.status_code == 200
    init_settings = get_res.json()
    assert "timezone" in init_settings

    # Update settings
    patch_res = auth_client.patch("/api/v1/settings", json={
        "video_duration": 20,
        "automation_enabled": True
    })
    assert patch_res.status_code == 200
    updated = patch_res.json()
    assert updated["video_duration"] == 20
    assert updated["automation_enabled"] is True

def test_youtube_status_endpoint(auth_client):
    response = auth_client.get("/api/v1/youtube/status")
    assert response.status_code == 200
    data = response.json()
    assert "connected" in data
    assert "dry_run_mode" in data

def test_analytics_endpoint(auth_client):
    response = auth_client.get("/api/v1/analytics")
    assert response.status_code == 200
    data = response.json()
    assert "total_published_videos" in data
    assert "success_rate" in data

def test_authentication_enforcement_and_tokens():
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db, email="auth_test_user@thoughtflow.ai", role="admin")

        # 1. Missing credentials must return 401
        missing_resp = client.get("/api/v1/dashboard")
        assert missing_resp.status_code == 401
        assert "authentication required" in missing_resp.json()["detail"].lower()

        # 2. Invalid bearer token must return 401
        invalid_resp = client.get("/api/v1/dashboard", headers={"Authorization": "Bearer invalid_garbage_token_123"})
        assert invalid_resp.status_code == 401
        assert "invalid" in invalid_resp.json()["detail"].lower()

        # 3. Valid bearer token passes
        token = create_access_token(subject=user.id)
        valid_resp = client.get("/api/v1/dashboard", headers={"Authorization": f"Bearer {token}"})
        assert valid_resp.status_code == 200

        # 4. Valid programmatic X-API-Key passes
        api_key_resp = client.get("/api/v1/dashboard", headers={"X-API-Key": settings.APP_SECRET_KEY})
        assert api_key_resp.status_code == 200

        # 5. Invalid X-API-Key must return 401
        bad_api_resp = client.get("/api/v1/dashboard", headers={"X-API-Key": "wrong_key_xyz"})
        assert bad_api_resp.status_code == 401
    finally:
        db.close()


def test_login_rejects_unknown_email_without_creation():
    """Verify that unknown emails are rejected with 401 and NO account is created"""
    db = SessionLocal()
    non_existent_email = "definitely_does_not_exist_9876@example.com"
    try:
        # Confirm user does not exist
        assert db.query(User).filter(User.email == non_existent_email).first() is None

        # Attempt login
        resp = client.post("/api/v1/auth/login", json={
            "email": non_existent_email,
            "password": "SomePassword123!"
        })
        assert resp.status_code == 401
        assert "invalid email or password" in resp.json()["detail"].lower()

        # Confirm user was STILL NOT created
        assert db.query(User).filter(User.email == non_existent_email).first() is None
    finally:
        db.close()


def test_registration_password_validation_and_flow():
    """Verify separate registration endpoint enforces password strength"""
    # 1. Reject too short password (<8)
    short_pw = client.post("/api/v1/auth/register", json={
        "email": "pw_test_1@example.com",
        "password": "short"
    })
    assert short_pw.status_code == 400
    assert "8 characters" in short_pw.json()["detail"].lower()

    # 2. Reject password with only letters
    no_num_pw = client.post("/api/v1/auth/register", json={
        "email": "pw_test_2@example.com",
        "password": "onlylettershere"
    })
    assert no_num_pw.status_code == 400
    assert "number or special character" in no_num_pw.json()["detail"].lower()

    # 3. Successful registration with strong password
    valid_reg = client.post("/api/v1/auth/register", json={
        "email": "unique_register_user_2026@example.com",
        "password": "StrongPassword2026!"
    })
    assert valid_reg.status_code == 200
    data = valid_reg.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "unique_register_user_2026@example.com"

    # 4. Attempt duplicate registration fails
    dup_reg = client.post("/api/v1/auth/register", json={
        "email": "unique_register_user_2026@example.com",
        "password": "StrongPassword2026!"
    })
    assert dup_reg.status_code == 400

    # Cleanup
    db = SessionLocal()
    db.query(User).filter(User.email == "unique_register_user_2026@example.com").delete()
    db.commit()
    db.close()


def test_login_rate_limiting():
    """Verify that 5 failed attempts trigger 429 Too Many Requests"""
    from app.api.v1.endpoints.auth import login_tracker
    target_email = "brute_force_target@example.com"
    rate_key = f"testclient_{target_email}"
    login_tracker.reset(rate_key)

    # 5 failed attempts
    for _ in range(5):
        resp = client.post("/api/v1/auth/login", json={
            "email": target_email,
            "password": "WrongPassword123!"
        })
        assert resp.status_code == 401

    # 6th attempt should be blocked with 429
    blocked = client.post("/api/v1/auth/login", json={
        "email": target_email,
        "password": "WrongPassword123!"
    })
    assert blocked.status_code == 429
    assert "too many failed login attempts" in blocked.json()["detail"].lower()

    # Reset for other tests
    login_tracker.reset(rate_key)


def test_email_verification_endpoints():
    """Verify email verification lookup and token consumption"""
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db, email="verify_test@thoughtflow.ai")
        user.is_verified = False
        user.verification_token = "valid_test_token_12345"
        db.commit()

        # 1. Invalid token returns 400
        bad_token_res = client.get("/api/v1/auth/verify-email?token=invalid_token_xyz")
        assert bad_token_res.status_code == 400

        # 2. Valid token sets is_verified=True and clears token
        good_token_res = client.get("/api/v1/auth/verify-email?token=valid_test_token_12345")
        assert good_token_res.status_code == 200
        assert good_token_res.json()["status"] == "success"

        db.refresh(user)
        assert user.is_verified is True
        assert user.verification_token is None
    finally:
        db.close()
