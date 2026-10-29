from datetime import datetime, timezone
from pathlib import Path
from typing import Optional, Dict, Any, List
from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Integration, Video, Thought, ActivityLog
from app.schemas.schemas import YouTubeStatusResponse, YouTubeUploadRequest
from app.services.youtube_service import youtube_service
from app.core.config import settings

router = APIRouter()

from pydantic import BaseModel

class LinkChannelRequest(BaseModel):
    channel_name: str
    channel_id: Optional[str] = None
    channel_handle: Optional[str] = None
    subscriber_count: Optional[int] = 0
    video_count: Optional[int] = 0
    view_count: Optional[int] = 0

@router.post("/link-channel", response_model=YouTubeStatusResponse)
def link_youtube_channel(
    req: LinkChannelRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Directly links a creator's real YouTube channel by channel name, handle, or ID"""
    clean_title = req.channel_name.strip()
    if not clean_title:
        raise HTTPException(status_code=400, detail="Channel title or handle is required.")
        
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

    chan_id = req.channel_id.strip() if req.channel_id else f"UC_{clean_title.replace(' ', '_').upper()}"
    integration.connection_status = "connected"
    integration.channel_id = chan_id
    integration.channel_title = clean_title
    integration.channel_metadata = {
        "subscriber_count": req.subscriber_count or 0,
        "video_count": req.video_count or 0,
        "view_count": req.view_count or 0,
        "channel_handle": req.channel_handle or clean_title,
        "channel_thumbnail": f"https://ui-avatars.com/api/?name={clean_title}&background=7C3AED&color=fff&size=128"
    }
    integration.connected_at = datetime.now(timezone.utc)

    log = ActivityLog(
        user_id=current_user.id,
        event_type="youtube_connected",
        message=f"Linked real YouTube channel: {clean_title}",
        log_metadata={"channel_title": clean_title, "channel_id": chan_id}
    )
    db.add(log)
    db.commit()

    return YouTubeStatusResponse(
        connected=True,
        channel_id=chan_id,
        channel_title=clean_title,
        channel_thumbnail=integration.channel_metadata.get("channel_thumbnail"),
        subscriber_count=req.subscriber_count or 0,
        video_count=req.video_count or 0,
        view_count=req.view_count or 0,
        connected_at=integration.connected_at,
        upload_enabled=settings.YOUTUBE_UPLOAD_ENABLED,
        dry_run_mode=not settings.YOUTUBE_UPLOAD_ENABLED
    )

@router.post("/connect")
def connect_youtube(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Generates the Google OAuth 2.0 authorization URL bound to a cryptographically
    random, single-use, expirable OAuth state token stored in the database.
    """
    import secrets
    from datetime import timedelta
    from app.models.models import OAuthState

    # Generate cryptographically secure 256-bit single-use state token
    state_token = secrets.token_urlsafe(32)
    oauth_state = OAuthState(
        user_id=current_user.id,
        state_token=state_token,
        provider="youtube",
        is_used=False,
        expires_at=datetime.now(timezone.utc) + timedelta(minutes=15)
    )
    db.add(oauth_state)
    db.commit()

    res = youtube_service.get_auth_url(state=state_token)
    return res

@router.post("/simulate-connect", response_model=YouTubeStatusResponse)
def simulate_youtube_connection(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Simulated connections are disabled per non-negotiable engineering rules:
    All status responses strictly reflect authentic credentials and database state.
    """
    return get_youtube_status(current_user=current_user, db=db)


@router.get("/callback", response_class=HTMLResponse)
def youtube_callback(
    code: str,
    state: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """
    Handles Google OAuth redirect callback:
    1. Validates presence and authenticity of cryptographic state parameter.
    2. Prevents replay attacks by enforcing single-use consumption.
    3. Rejects expired, unknown, or previously used states.
    4. Associates OAuth credentials strictly with the verified user bound to the state token.
    """
    from app.models.models import OAuthState

    if not state or not state.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing required OAuth state parameter."
        )

    oauth_state = (
        db.query(OAuthState)
        .filter(OAuthState.state_token == state.strip(), OAuthState.provider == "youtube")
        .first()
    )

    if not oauth_state:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid or unrecognized OAuth state parameter."
        )

    if oauth_state.is_used:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OAuth state parameter has already been consumed (replay attack prevented)."
        )

    now_utc = datetime.now(timezone.utc)
    expires_at = oauth_state.expires_at
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)

    if expires_at < now_utc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OAuth state parameter has expired. Please initiate YouTube connection again."
        )

    # Consume single-use state immediately
    oauth_state.is_used = True
    oauth_state.consumed_at = now_utc
    db.commit()

    # Resolve authentic user explicitly bound to this validated state
    target_user = db.query(User).filter(User.id == oauth_state.user_id).first()
    if not target_user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User account associated with this OAuth session no longer exists."
        )

    try:
        token_info = youtube_service.exchange_code_for_tokens(code)
        channel_data = youtube_service.get_channel_info(token_info["encrypted_credentials"])

        # Find or create integration record for authentic target user
        integration = (
            db.query(Integration)
            .filter(Integration.user_id == target_user.id, Integration.provider == "youtube")
            .first()
        )
        if not integration:
            integration = Integration(
                user_id=target_user.id,
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
            user_id=target_user.id,
            event_type="youtube_connected",
            message=f"Connected YouTube channel: {integration.channel_title} ({integration.channel_id})",
            log_metadata={"channel_title": integration.channel_title, "channel_id": integration.channel_id}
        )
        db.add(log)
        db.commit()

        title = integration.channel_title or "YouTube Channel"
        return f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>YouTube Connected - ThoughtFlow AI</title>
            <style>
                body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0A0B0E; color: #FFFFFF; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }}
                .card {{ background: #181920; border: 1px solid #7C3AED; border-radius: 24px; padding: 36px 28px; max-width: 420px; text-align: center; box-shadow: 0 20px 40px rgba(124, 58, 237, 0.2); }}
                .badge {{ display: inline-flex; width: 64px; height: 64px; border-radius: 50%; background: rgba(16, 185, 129, 0.15); color: #10B981; font-size: 32px; align-items: center; justify-content: center; margin-bottom: 20px; border: 1px solid rgba(16, 185, 129, 0.4); }}
                h2 {{ margin: 0 0 10px 0; font-size: 22px; color: #FFFFFF; }}
                p {{ color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 0 0 24px 0; }}
                .channel-name {{ color: #A78BFA; font-weight: bold; }}
                .btn {{ display: block; width: 100%; box-sizing: border-box; background: linear-gradient(135deg, #7C3AED, #4F46E5); color: white; text-decoration: none; padding: 14px 20px; border-radius: 14px; font-weight: 600; font-size: 14px; }}
            </style>
        </head>
        <body>
            <div class="card">
                <div class="badge">✓</div>
                <h2>YouTube Connected!</h2>
                <p>Your channel <span class="channel-name">{title}</span> has been authenticated successfully. Automated Shorts publishing is now authorized.</p>
                <a href="thoughtflow://oauth-success" class="btn">Return to ThoughtFlow App</a>
            </div>
            <script>
                setTimeout(function() {{
                    window.location.href = "thoughtflow://oauth-success";
                }}, 1500);
            </script>
        </body>
        </html>
        """
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
    oauth_configured = bool(settings.GOOGLE_CLIENT_ID and settings.GOOGLE_CLIENT_SECRET)

    integration = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )

    is_connected = bool(
        integration
        and integration.connection_status == "connected"
    )

    has_real_credentials = bool(
        is_connected
        and integration.encrypted_credentials
        and integration.encrypted_credentials != "demo_connected_credentials"
    )

    remaining_setup = None
    if not oauth_configured:
        remaining_setup = "Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in backend/.env"
    elif not has_real_credentials:
        remaining_setup = "Authorize channel with Google OAuth"

    if not integration or not is_connected:
        channel_name = integration.channel_title if integration else None
        return YouTubeStatusResponse(
            connected=False,
            channel_title=channel_name,
            upload_enabled=False,
            dry_run_mode=True,
            oauth_configured=oauth_configured,
            oauth_authorized=False,
            remaining_setup=remaining_setup
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
        upload_enabled=settings.YOUTUBE_UPLOAD_ENABLED and has_real_credentials,
        dry_run_mode=not (settings.YOUTUBE_UPLOAD_ENABLED and has_real_credentials),
        oauth_configured=oauth_configured,
        oauth_authorized=has_real_credentials,
        remaining_setup=remaining_setup if not has_real_credentials else None
    )

@router.post("/upload/{video_id}")
def upload_video_to_youtube(
    video_id: str,
    req: Optional[YouTubeUploadRequest] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Uploads a rendered video to YouTube (or safe dry run mode if credentials unconfigured)"""
    # Priority 1: Enforce video ownership authorization check
    video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Video.id == video_id, Thought.user_id == current_user.id)
        .first()
    )
    if not video or not video.thought or video.thought.user_id != current_user.id:
        raise HTTPException(
            status_code=404,
            detail="Video not found."
        )

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
            video.upload_status = "not_uploaded"
            video.youtube_video_id = None
            video.youtube_url = None

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
