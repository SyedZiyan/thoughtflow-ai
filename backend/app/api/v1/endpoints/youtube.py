from datetime import datetime, timezone
from pathlib import Path
from typing import Optional, Dict, Any, List
from fastapi import APIRouter, Depends, HTTPException, Query, status
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
    """Generates the Google OAuth 2.0 authorization URL for YouTube channel access"""
    res = youtube_service.get_auth_url(state=f"user_{current_user.id}")
    return res

@router.post("/simulate-connect", response_model=YouTubeStatusResponse)
def simulate_youtube_connection(
    channel_name: Optional[str] = Query(default="ThoughtFlow Motivation Official"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Connects or simulates a connected YouTube channel for testing and instant preview"""
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

    integration.encrypted_credentials = "demo_connected_credentials"
    integration.connection_status = "connected"
    integration.channel_id = "UC_THOUGHTFLOW_OFFICIAL"
    integration.channel_title = channel_name or "ThoughtFlow Motivation Official"
    integration.channel_metadata = {
        "subscriber_count": 1240,
        "video_count": 18,
        "view_count": 48200,
        "channel_thumbnail": "https://picsum.photos/seed/thoughtflow_yt/120/120"
    }
    integration.connected_at = datetime.now(timezone.utc)

    log = ActivityLog(
        user_id=current_user.id,
        event_type="youtube_connected",
        message=f"Connected YouTube channel: {integration.channel_title}",
        log_metadata={"channel_title": integration.channel_title}
    )
    db.add(log)
    db.commit()

    return YouTubeStatusResponse(
        connected=True,
        channel_id=integration.channel_id,
        channel_title=integration.channel_title,
        channel_thumbnail="https://picsum.photos/seed/thoughtflow_yt/120/120",
        subscriber_count=1240,
        video_count=18,
        view_count=48200,
        connected_at=integration.connected_at,
        upload_enabled=True,
        dry_run_mode=False
    )


@router.get("/callback")
def youtube_callback(
    code: str,
    state: Optional[str] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Handles Google OAuth redirect callback, stores encrypted tokens and channel info"""
    try:
        token_info = youtube_service.exchange_code_for_tokens(code)
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

        # Log connection event
        log = ActivityLog(
            user_id=current_user.id,
            event_type="youtube_connected",
            message=f"Connected YouTube channel: {integration.channel_title} ({integration.channel_id})",
            log_metadata={"channel_title": integration.channel_title, "channel_id": integration.channel_id}
        )
        db.add(log)
        db.commit()

        return {
            "status": "success",
            "message": f"Successfully connected channel: {integration.channel_title}",
            "channel_id": integration.channel_id,
            "channel_title": integration.channel_title
        }
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=f"OAuth connection failed: {str(e)}")

@router.post("/disconnect")
async def disconnect_youtube(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Revokes token with Google and disconnects the channel integration"""
    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )
    if not integration or integration.connection_status != "connected":
        return {"status": "success", "message": "No active YouTube channel connection found."}

    # Attempt safe revocation
    if integration.encrypted_credentials:
        await youtube_service.revoke_token(integration.encrypted_credentials)

    integration.encrypted_credentials = None
    integration.connection_status = "disconnected"
    integration.channel_metadata = {}

    log = ActivityLog(
        user_id=current_user.id,
        event_type="youtube_disconnected",
        message=f"Disconnected YouTube channel: {integration.channel_title}",
        log_metadata={"channel_title": integration.channel_title}
    )
    db.add(log)
    db.commit()

    return {"status": "success", "message": "Successfully disconnected YouTube channel."}

@router.get("/status", response_model=YouTubeStatusResponse)
def get_youtube_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Returns live connection status, channel metadata, and quota information"""
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
    """Uploads a rendered video to YouTube (or safe dry run mode if credentials unconfigured)"""
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    if video.render_status != "completed":
        raise HTTPException(status_code=400, detail=f"Cannot upload video in '{video.render_status}' status. Render must be completed.")

    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )

    creds_str = integration.encrypted_credentials if (integration and integration.connection_status == "connected") else None
    video_path = Path(settings.VIDEO_STORAGE_DIR) / video.storage_key

    if not video_path.exists():
        raise HTTPException(status_code=404, detail=f"Video file {video.storage_key} not found on disk.")

    try:
        title = (req.title if req and req.title else video.title)
        desc = (req.description if req and req.description else video.description)
        tags = (req.tags if req and req.tags else (video.thought.hashtags if video.thought else ["shorts"]))
        privacy = (req.privacy_status if req and req.privacy_status else "private")
        publish_at = (req.publish_at if req else None)

        res = youtube_service.upload_video(
            video_path=str(video_path),
            title=title,
            description=desc,
            tags=tags,
            privacy_status=privacy,
            publish_at=publish_at,
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
            message=f"YouTube video \"{title}\" processed ({'Live' if not res.get('dry_run') else 'Dry Run'}).",
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

@router.post("/sync-metrics")
def sync_youtube_metrics(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Syncs live YouTube view counts, like counts, and comments for all user videos"""
    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )
    if not integration or integration.connection_status != "connected" or not integration.encrypted_credentials:
        return {"status": "skipped", "message": "No connected YouTube channel to sync metrics from."}

    # Find videos with youtube_video_id
    videos = (
        db.query(Video)
        .join(Video.thought)
        .filter(Video.youtube_video_id != None, Video.thought.has(user_id=current_user.id))
        .all()
    )
    video_ids = [v.youtube_video_id for v in videos if v.youtube_video_id and not v.youtube_video_id.startswith("dryrun_")]

    if not video_ids:
        return {"status": "success", "message": "No published YouTube videos found to sync.", "synced_count": 0}

    stats = youtube_service.sync_video_statistics(video_ids, integration.encrypted_credentials)
    updated = 0
    for v in videos:
        if v.youtube_video_id in stats:
            st = stats[v.youtube_video_id]
            # Store updated metrics in activity log or metadata
            updated += 1

    return {
        "status": "success",
        "synced_count": updated,
        "metrics": stats
    }
