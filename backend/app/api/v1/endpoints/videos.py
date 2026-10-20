from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Thought, Video, ActivityLog
from app.schemas.schemas import VideoRenderRequest, VideoResponse
from app.services.video_renderer import video_renderer

router = APIRouter()

def enrich_video_response(video: Video) -> VideoResponse:
    res = VideoResponse.from_orm(video)
    res.video_url = f"/storage/videos/{video.storage_key}"
    return res

@router.get("", response_model=List[VideoResponse])
def list_videos(
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = 50,
    offset: int = 0,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    query = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Thought.user_id == current_user.id)
    )
    if status_filter:
        query = query.filter(Video.render_status == status_filter)

    videos = query.order_by(Video.created_at.desc()).offset(offset).limit(limit).all()
    return [enrich_video_response(v) for v in videos]

@router.post("/render", response_model=VideoResponse)
def render_video(
    req: VideoRenderRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    thought = db.query(Thought).filter(Thought.id == req.thought_id, Thought.user_id == current_user.id).first()
    if not thought:
        raise HTTPException(status_code=404, detail="Thought not found.")

    try:
        render_res = video_renderer.render_animated_video(
            thought_id=thought.id,
            thought_text=thought.text,
            theme=thought.theme,
            visual_theme=req.visual_theme or "gradient_purple",
            animation_style=req.animation_style or "smooth_zoom",
            duration_seconds=req.duration_seconds or 15
        )

        video = Video(
            thought_id=thought.id,
            storage_key=render_res["storage_key"],
            title=thought.title,
            description=thought.description,
            duration_seconds=render_res["duration_seconds"],
            width=render_res["width"],
            height=render_res["height"],
            render_status="completed",
            upload_status="not_uploaded"
        )
        db.add(video)
        thought.status = "rendered"

        # Log activity
        log = ActivityLog(
            user_id=current_user.id,
            event_type="video_rendered",
            message=f"Rendered animated video for: \"{thought.title}\"",
            log_metadata={"video_id": video.id, "duration": render_res["duration_seconds"]}
        )
        db.add(log)
        db.commit()
        db.refresh(video)

        return enrich_video_response(video)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Rendering failed: {str(e)}")

@router.get("/{id}", response_model=VideoResponse)
def get_video(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Video.id == id, Thought.user_id == current_user.id)
        .first()
    )
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")
    return enrich_video_response(video)
