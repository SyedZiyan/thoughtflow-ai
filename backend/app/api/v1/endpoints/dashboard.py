from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Thought, Video, Job, Integration, ActivityLog
from app.schemas.schemas import DashboardResponse, VideoResponse, ActivityLogResponse
from app.api.v1.endpoints.videos import enrich_video_response

router = APIRouter()

@router.get("", response_model=DashboardResponse)
def get_dashboard_data(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    now_utc = datetime.now(timezone.utc)
    start_of_today = now_utc.replace(hour=0, minute=0, second=0, microsecond=0)

    # Counts
    total_thoughts = db.query(func.count(Thought.id)).filter(Thought.user_id == current_user.id).scalar() or 0
    total_videos = (
        db.query(func.count(Video.id))
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Thought.user_id == current_user.id)
        .scalar() or 0
    )
    total_published = (
        db.query(func.count(Video.id))
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Thought.user_id == current_user.id, Video.upload_status == "published")
        .scalar() or 0
    )
    failed_jobs = db.query(func.count(Job.id)).filter(Job.status == "failed").scalar() or 0

    # Today's status
    today_thought = (
        db.query(Thought)
        .filter(Thought.user_id == current_user.id, Thought.created_at >= start_of_today)
        .first()
    )
    todays_generation_status = "completed" if today_thought else "pending"

    today_video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Thought.user_id == current_user.id, Video.created_at >= start_of_today)
        .first()
    )
    if today_video:
        todays_publishing_status = today_video.upload_status
    else:
        todays_publishing_status = "pending"

    # YouTube connection status
    yt = (
        db.query(Integration)
        .filter(Integration.user_id == current_user.id, Integration.provider == "youtube")
        .first()
    )
    youtube_connected = bool(yt and yt.connection_status == "connected")

    # Settings & next schedule
    settings = current_user.settings
    automation_enabled = settings.automation_enabled if settings else False

    next_execution = None
    if automation_enabled and settings:
        gen_time = settings.generation_time or "07:00"
        next_execution = f"Tomorrow at {gen_time} {settings.timezone}"

    # Recent activity
    activities = (
        db.query(ActivityLog)
        .filter(ActivityLog.user_id == current_user.id)
        .order_by(ActivityLog.created_at.desc())
        .limit(10)
        .all()
    )

    # Latest videos
    videos = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Thought.user_id == current_user.id)
        .order_by(Video.created_at.desc())
        .limit(6)
        .all()
    )

    # Upcoming publishing calendar schedule preview
    themes = (settings.selected_themes if settings and settings.selected_themes else ["motivation", "discipline", "resilience", "consistency"])
    upcoming = []
    for i in range(1, 5):
        target_day = (now_utc + timedelta(days=i)).strftime("%Y-%m-%d")
        theme_pick = themes[i % len(themes)]
        upcoming.append({
            "date": target_day,
            "thought_title": f"Daily {theme_pick.capitalize()} Shift #Shorts",
            "status": "scheduled",
            "theme": theme_pick,
            "time": settings.publishing_time if settings else "09:00"
        })

    return DashboardResponse(
        todays_generation_status=todays_generation_status,
        todays_publishing_status=todays_publishing_status,
        next_scheduled_execution=next_execution,
        total_thoughts=total_thoughts,
        total_videos=total_videos,
        total_published=total_published,
        failed_jobs=failed_jobs,
        youtube_connected=youtube_connected,
        automation_enabled=automation_enabled,
        recent_activity=[ActivityLogResponse.model_validate(a) for a in activities],
        upcoming_schedule=upcoming,
        latest_videos=[enrich_video_response(v) for v in videos]
    )
