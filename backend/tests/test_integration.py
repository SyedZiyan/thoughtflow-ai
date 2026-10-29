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
