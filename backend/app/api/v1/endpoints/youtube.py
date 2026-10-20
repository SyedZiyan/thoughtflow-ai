from datetime import datetime, timezone
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Integration, Video, ActivityLog
from app.schemas.schemas import YouTubeStatusResponse, YouTubeUploadRequest
from app.services.youtube_service import youtube_service
from app.core.config import settings

router = APIRouter()

@router.post("/connect")
def connect_youtube(
    current_user: User = Depends(get_current_user)
):
    """Generates the Google OAuth authorization URL"""
    res = youtube_service.get_auth_url(state=f"user_{current_user.id}")
    return res

@router.get("/callback")
def youtube_callback(
    code: str,
    state: Optional[str] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Handle Google OAuth redirect callback, store encrypted tokens"""
    try:
        token_info = youtube_service.exchange_code_for_tokens(code)
        
        # Check channel info
        channel_data = youtube_service.get_channel_info(token_info["encrypted_credentials"])

        # Find or create integration record
        integration = (
            db.query(Integration)
            .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
            .first()
        )
        if not integration:
            integration = Integration(
                user_id=current_user.id,
                provider="youtube"
            )
            db.add(integration)

        integration.encrypted_credentials = token_info["encrypted_credentials"]
        integration.connection_status = "connected"
        integration.channel_id = channel_data.get("channel_id")
        integration.channel_title = channel_data.get("channel_title")
        integration.channel_metadata = channel_data
        integration.connected_at = datetime.now(timezone.utc)

        db.commit()
        return {"status": "success", "message": f"Successfully connected channel: {integration.channel_title}"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=f"OAuth connection failed: {str(e)}")

@router.get("/status", response_model=YouTubeStatusResponse)
def get_youtube_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )

    if not integration or integration.connection_status != "connected":
        return YouTubeStatusResponse(
            connected=False,
            upload_enabled=settings.YOUTUBE_UPLOAD_ENABLED,
            dry_run_mode=not settings.YOUTUBE_UPLOAD_ENABLED
        )

    meta = integration.channel_metadata or {}
    return YouTubeStatusResponse(
        connected=True,
        channel_id=integration.channel_id,
        channel_title=integration.channel_title,
        channel_thumbnail=meta.get("channel_thumbnail"),
        subscriber_count=meta.get("subscriber_count"),
        video_count=meta.get("video_count"),
        view_count=meta.get("view_count"),
        connected_at=integration.connected_at,
        upload_enabled=settings.YOUTUBE_UPLOAD_ENABLED,
        dry_run_mode=not settings.YOUTUBE_UPLOAD_ENABLED
    )

@router.post("/upload/{video_id}")
def upload_video_to_youtube(
    video_id: str,
    req: Optional[YouTubeUploadRequest] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )

    creds_str = integration.encrypted_credentials if integration else None
    video_path = Path(settings.VIDEO_STORAGE_DIR) / video.storage_key

    try:
        title = (req.title if req and req.title else video.title)
        desc = (req.description if req and req.description else video.description)
        tags = (req.tags if req and req.tags else (video.thought.hashtags if video.thought else ["shorts"]))
        privacy = (req.privacy_status if req and req.privacy_status else "private")

        res = youtube_service.upload_video(
            video_path=str(video_path),
            title=title,
            description=desc,
            tags=tags,
            privacy_status=privacy,
            encrypted_credentials_str=creds_str
        )

        if not res.get("dry_run"):
            video.upload_status = "published"
            video.youtube_video_id = res.get("youtube_video_id")
            video.youtube_url = res.get("youtube_url")
            video.published_at = datetime.now(timezone.utc)
        else:
            video.youtube_url = res.get("youtube_url")

        # Log activity
        log = ActivityLog(
            user_id=current_user.id,
            event_type="upload_success" if not res.get("dry_run") else "upload_dry_run",
            message=f"YouTube video {title} processed ({'Live' if not res.get('dry_run') else 'Dry Run'}).",
            log_metadata=res
        )
        db.add(log)
        db.commit()

        return res
    except Exception as e:
        db.rollback()
        video.upload_status = "failed"
        db.commit()
        raise HTTPException(status_code=500, detail=f"Upload failed: {str(e)}")
