import json
import pytest
from pathlib import Path
from datetime import datetime, timezone
from fastapi.testclient import TestClient
from app.main import app
from app.db.session import SessionLocal
from app.models.models import User, Thought, Video, Integration, ActivityLog
from conftest import get_or_create_test_user, get_test_auth_headers
from app.services.youtube_service import youtube_service
from app.core.security import encrypt_token, decrypt_token
from app.core.config import settings

client = TestClient(app)

# ==========================================
# 1. OAUTH2 FLOW & CREDENTIAL ENCRYPTION
# ==========================================

def test_oauth_url_generation_unconfigured(monkeypatch):
    monkeypatch.setattr(youtube_service, "client_id", None)
    monkeypatch.setattr(youtube_service, "client_secret", None)

    res = youtube_service.get_auth_url()
    assert res["configured"] is False
    assert res["auth_url"] == ""
    assert "GOOGLE_CLIENT_ID" in res["message"]

def test_oauth_url_generation_configured(monkeypatch):
    monkeypatch.setattr(youtube_service, "client_id", "test_client_id_123.apps.googleusercontent.com")
    monkeypatch.setattr(youtube_service, "client_secret", "GOCSPX-test_client_secret_xyz")

    res = youtube_service.get_auth_url(state="test_state_123")
    assert res["configured"] is True
    assert "accounts.google.com" in res["auth_url"]
    assert "access_type=offline" in res["auth_url"]
    assert "prompt=consent" in res["auth_url"]
    assert "state=test_state_123" in res["auth_url"]

def test_token_encryption_and_decryption():
    raw_tokens = {
        "token": "ya29.a0AfH6SMD_TEST_ACCESS_TOKEN",
        "refresh_token": "1//04_TEST_REFRESH_TOKEN",
        "token_uri": "https://oauth2.googleapis.com/token",
        "client_id": "test_client_id.apps.googleusercontent.com",
        "client_secret": "test_secret",
        "scopes": ["https://www.googleapis.com/auth/youtube.upload"]
    }

    encrypted = encrypt_token(json.dumps(raw_tokens))
    assert isinstance(encrypted, str)
    # The raw tokens must NEVER appear in the encrypted ciphertext
    assert "ya29.a0AfH6SMD_TEST_ACCESS_TOKEN" not in encrypted
    assert "1//04_TEST_REFRESH_TOKEN" not in encrypted

    decrypted_str = decrypt_token(encrypted)
    decrypted = json.loads(decrypted_str)
    assert decrypted["token"] == raw_tokens["token"]
    assert decrypted["refresh_token"] == raw_tokens["refresh_token"]


# ==========================================
# 2. DRY-RUN SAFETY & FILE VALIDATION
# ==========================================

def test_dry_run_upload_safety(tmp_path):
    test_video = tmp_path / "sample_short.mp4"
    # Write a valid dummy video payload (>10KB)
    test_video.write_bytes(b"\x00" * 15000)

    # Calling upload without credentials or with upload disabled enters safe dry-run mode
    res = youtube_service.upload_video(
        video_path=str(test_video),
        title="1% Better Everyday",
        description="Daily reminder to compound positive habits.",
        tags=["mindset", "habits"],
        privacy_status="private",
        encrypted_credentials_str=None
    )

    assert res["dry_run"] is True
    assert res["status"] in ("dry_run_validated", "dry_run_pending_credentials")
    assert "dryrun_" in res["youtube_video_id"]
    assert "youtube.com" in res["youtube_url"]
    assert "Dry-run mode active" in res["message"]

def test_upload_missing_file():
    with pytest.raises(FileNotFoundError, match="Video file not found"):
        youtube_service.upload_video(
            video_path="storage/videos/non_existent_thought_999.mp4",
            title="Non-existent",
            description="desc",
            tags=["test"]
        )

def test_upload_corrupt_file_too_small(tmp_path):
    corrupt_video = tmp_path / "tiny_corrupt.mp4"
    corrupt_video.write_bytes(b"too small")

    with pytest.raises(ValueError, match="Video file too small"):
        youtube_service.upload_video(
            video_path=str(corrupt_video),
            title="Corrupt Video",
            description="desc",
            tags=["test"]
        )


# ==========================================
# 3. YOUTUBE API ENDPOINTS
# ==========================================

def test_api_youtube_status_disconnected():
    # 1. Unauthenticated request must return 401
    unauth_resp = client.get("/api/v1/youtube/status")
    assert unauth_resp.status_code == 401

    # 2. Authenticated user without connected channel returns 200 with connected=False
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db, email="disconnected_user@thoughtflow.ai")
        # Ensure no integration exists
        db.query(Integration).filter(Integration.user_id == user.id).delete()
        db.commit()

        headers = get_test_auth_headers(user)
        resp = client.get("/api/v1/youtube/status", headers=headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["connected"] is False
        assert "upload_enabled" in data
        assert "dry_run_mode" in data
    finally:
        db.close()

def test_api_youtube_status_connected():
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db)
        headers = get_test_auth_headers(user)
        integration = (
            db.query(Integration)
            .filter(Integration.user_id == user.id, Integration.provider == "youtube")
            .first()
        )
        if not integration:
            integration = Integration(user_id=user.id, provider="youtube")
            db.add(integration)

        integration.connection_status = "connected"
        integration.channel_id = "UC_THOUGHTFLOW_TEST_CHANNEL"
        integration.channel_title = "ThoughtFlow Motivation Official"
        integration.channel_metadata = {
            "channel_thumbnail": "https://yt3.ggpht.com/test_avatar.jpg",
            "subscriber_count": 14200,
            "video_count": 87,
            "view_count": 940000
        }
        integration.connected_at = datetime.now(timezone.utc)
        db.commit()

        resp = client.get("/api/v1/youtube/status", headers=headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["connected"] is True
        assert data["channel_title"] == "ThoughtFlow Motivation Official"
        assert data["channel_id"] == "UC_THOUGHTFLOW_TEST_CHANNEL"
        assert data["subscriber_count"] == 14200
        assert data["view_count"] == 940000
    finally:
        db.close()

def test_api_youtube_disconnect():
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db)
        headers = get_test_auth_headers(user)
        resp = client.post("/api/v1/youtube/disconnect", headers=headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "success"
    finally:
        db.close()

def test_api_youtube_sync_metrics_no_channel():
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db)
        headers = get_test_auth_headers(user)
        resp = client.post("/api/v1/youtube/sync-metrics", headers=headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] in ["skipped", "success"]
    finally:
        db.close()

def test_upload_ownership_authorization():
    db = SessionLocal()
    try:
        user = get_or_create_test_user(db)
        headers = get_test_auth_headers(user)

        # Create a thought & video belonging to another user
        other_user = User(
            email="other_creator@thoughtflow.ai",
            password_hash="dummy_hash_other"
        )
        db.add(other_user)
        db.commit()
        db.refresh(other_user)

        other_thought = Thought(
            user_id=other_user.id,
            text="Private thought of another creator.",
            theme="mindfulness",
            title="Private Short #Shorts",
            description="Other user short",
            content_hash="dummy_hash_other_123",
            status="rendered"
        )
        db.add(other_thought)
        db.commit()
        db.refresh(other_thought)

        other_video = Video(
            thought_id=other_thought.id,
            storage_key="dummy_other_video.mp4",
            title="Other Title",
            description="Other Desc",
            render_status="completed"
        )
        db.add(other_video)
        db.commit()
        db.refresh(other_video)

        # When current user tries to upload other user's video -> must reject
        resp = client.post(f"/api/v1/youtube/upload/{other_video.id}", headers=headers)
        assert resp.status_code == 404
        assert "permission" in resp.json()["detail"].lower()
    finally:
        db.query(Video).filter(Video.storage_key == "dummy_other_video.mp4").delete()
        db.query(Thought).filter(Thought.text == "Private thought of another creator.").delete()
        db.query(User).filter(User.email == "other_creator@thoughtflow.ai").delete()
        db.commit()
        db.close()
