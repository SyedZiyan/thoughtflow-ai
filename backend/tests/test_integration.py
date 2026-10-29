import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.db.session import SessionLocal
from app.models.models import User, Thought, Video, Job

client = TestClient(app)

def test_health_check_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["database"] == "connected"
    assert "ffmpeg_installed" in data

def test_dashboard_endpoint():
    response = client.get("/api/v1/dashboard")
    assert response.status_code == 200
    data = response.json()
    assert "total_thoughts" in data
    assert "total_videos" in data
    assert "upcoming_schedule" in data

def test_generate_thought_endpoint():
    response = client.post("/api/v1/thoughts/generate", json={
        "theme": "discipline",
        "tone": "inspiring",
        "language": "en"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["theme"] == "discipline"
    assert len(data["text"]) > 10
    assert "shorts" in data["hashtags"]

def test_settings_retrieval_and_update():
    get_res = client.get("/api/v1/settings")
    assert get_res.status_code == 200
    init_settings = get_res.json()
    assert "timezone" in init_settings

    # Update settings
    patch_res = client.patch("/api/v1/settings", json={
        "video_duration": 20,
        "automation_enabled": True
    })
    assert patch_res.status_code == 200
    updated = patch_res.json()
    assert updated["video_duration"] == 20
    assert updated["automation_enabled"] is True

def test_youtube_status_endpoint():
    response = client.get("/api/v1/youtube/status")
    assert response.status_code == 200
    data = response.json()
    assert "connected" in data
    assert "dry_run_mode" in data

def test_analytics_endpoint():
    response = client.get("/api/v1/analytics")
    assert response.status_code == 200
    data = response.json()
    assert "total_published_videos" in data
    assert "success_rate" in data

def test_authentication_enforcement_and_tokens():
    from app.core.config import settings
    from app.core.security import create_access_token
    from app.api.deps import get_or_create_default_user

    # 1. Invalid bearer token must return 401
    invalid_resp = client.get("/api/v1/dashboard", headers={"Authorization": "Bearer invalid_garbage_token_123"})
    assert invalid_resp.status_code == 401
    assert "invalid" in invalid_resp.json()["detail"].lower()

    # 2. When AUTH_REQUIRED is enabled, unauthenticated requests must be blocked with 401
    original_auth_req = settings.AUTH_REQUIRED
    try:
        settings.AUTH_REQUIRED = True
        blocked_resp = client.get("/api/v1/dashboard")
        assert blocked_resp.status_code == 401

        # 3. Valid bearer token passes
        db = SessionLocal()
        user = get_or_create_default_user(db)
        db.close()
        token = create_access_token(subject=user.id)
        valid_resp = client.get("/api/v1/dashboard", headers={"Authorization": f"Bearer {token}"})
        assert valid_resp.status_code == 200

        # 4. Valid programmatic X-API-Key passes
        api_key_resp = client.get("/api/v1/dashboard", headers={"X-API-Key": settings.APP_SECRET_KEY})
        assert api_key_resp.status_code == 200
    finally:
        settings.AUTH_REQUIRED = original_auth_req
