from typing import List, Optional, Dict, Any
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, Query, status, Response
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Thought, Video, ActivityLog
from app.schemas.schemas import VideoRenderRequest, VideoPreviewRequest, VideoResponse
from app.services.video_renderer import video_renderer, VideoValidationError
from app.services.audio_service import EdgeTTSProvider

router = APIRouter()

def enrich_video_response(video: Video) -> VideoResponse:
    res = VideoResponse.model_validate(video)
    res.video_url = f"/storage/videos/{video.storage_key}"
    return res

@router.get("/styles", response_model=Dict[str, Any])
def get_video_styles():
    """Returns available visual themes, animation effects, typography options, and TTS voices"""
    return {
        "visual_themes": [
            {"id": k, "name": v["name"], "accent": v["accent"]}
            for k, v in video_renderer.THEMES.items()
        ],
        "animation_styles": [
            {"id": "smooth_zoom", "name": "Slow Cinematic Zoom", "desc": "Continuous punch-in with centered framing"},
            {"id": "kinetic_pan", "name": "Kinetic Vertical Pan", "desc": "Subtle downward framed tracking"},
            {"id": "subtle_pulse", "name": "Subtle Breathing Pulse", "desc": "Gentle oscillating zoom rhythm"},
            {"id": "gentle_drift", "name": "Gentle Diagonal Drift", "desc": "Slow diagonal panning motion"}
        ],
        "typography_options": [
            {"id": "modern_sans", "name": "Modern Sans (Segoe UI / Arial)", "style": "Clean, bold, punchy"},
            {"id": "editorial_serif", "name": "Editorial Serif (Georgia)", "style": "Literary, classical elegance"},
            {"id": "clean_mono", "name": "Clean Monospace (Consolas)", "style": "Minimalist, tech aesthetic"}
        ],
        "text_placements": [
            {"id": "center", "name": "Centered (Classic)"},
            {"id": "upper_third", "name": "Upper Third"},
            {"id": "lower_third", "name": "Lower Third"}
        ],
        "tts_voices": EdgeTTSProvider.SUPPORTED_VOICES
    }

@router.post("/preview-frame")
def preview_frame(
    req: VideoPreviewRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Renders an instant 1080x1920 PNG preview of the slide typography and palette
    allowing sub-second frontend preview before executing full video compilation.
    """
    try:
        png_bytes = video_renderer.compose_slide_preview(
            thought_text=req.thought_text,
            theme=req.theme or "motivation",
            visual_theme=req.visual_theme or "gradient_purple",
            typography=req.typography or "modern_sans",
            text_placement=req.text_placement or "center"
        )
        return Response(content=png_bytes, media_type="image/png")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate slide preview: {str(e)}")

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
async def render_video(
    req: VideoRenderRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    thought = db.query(Thought).filter(Thought.id == req.thought_id, Thought.user_id == current_user.id).first()
    if not thought:
        raise HTTPException(status_code=404, detail="Thought not found.")

    try:
        render_res = await video_renderer.render_animated_video(
            thought_id=thought.id,
            thought_text=thought.text,
            theme=thought.theme,
            visual_theme=req.visual_theme or "gradient_purple",
            animation_style=req.animation_style or "smooth_zoom",
            typography=req.typography or "modern_sans",
            text_placement=req.text_placement or "center",
            duration_seconds=req.duration_seconds or 15,
            narration_enabled=bool(req.narration_enabled),
            ambient_enabled=bool(req.ambient_enabled if req.ambient_enabled is not None else True),
            narration_voice=req.narration_voice
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
            message=f"Rendered animated video for: \"{thought.title}\" ({render_res['duration_seconds']}s)",
            log_metadata={
                "video_id": video.id,
                "duration": render_res["duration_seconds"],
                "has_narration": render_res.get("has_narration", False),
                "has_ambient": render_res.get("has_ambient", False)
            }
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

@router.post("/{id}/retry", response_model=VideoResponse)
async def retry_render_video(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Retries a failed rendering job"""
    video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Video.id == id, Thought.user_id == current_user.id)
        .first()
    )
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    thought = video.thought
    try:
        render_res = await video_renderer.render_animated_video(
            thought_id=thought.id,
            thought_text=thought.text,
            theme=thought.theme,
            duration_seconds=video.duration_seconds or 15
        )
        video.storage_key = render_res["storage_key"]
        video.duration_seconds = render_res["duration_seconds"]
        video.width = render_res["width"]
        video.height = render_res["height"]
        video.render_status = "completed"

        db.commit()
        db.refresh(video)
        return enrich_video_response(video)
    except Exception as e:
        video.render_status = "failed"
        db.commit()
        raise HTTPException(status_code=500, detail=f"Retry rendering failed: {str(e)}")

@router.get("/{id}/download")
def download_video(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Serves the rendered MP4 file with attachment disposition for direct download"""
    video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Video.id == id, Thought.user_id == current_user.id)
        .first()
    )
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    file_path = Path(settings.VIDEO_STORAGE_DIR) / video.storage_key
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Video file not found on disk.")

    clean_filename = f"thoughtflow_{video.storage_key}"
    return FileResponse(
        path=str(file_path),
        media_type="video/mp4",
        filename=clean_filename
    )

@router.post("/{id}/validate", response_model=Dict[str, Any])
def validate_video_file(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Runs FFprobe inspection on the video file to verify compliance before YouTube upload"""
    video = (
        db.query(Video)
        .join(Thought, Video.thought_id == Thought.id)
        .filter(Video.id == id, Thought.user_id == current_user.id)
        .first()
    )
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    file_path = Path(settings.VIDEO_STORAGE_DIR) / video.storage_key
    try:
        validation = video_renderer.validate_video(str(file_path), expected_duration=video.duration_seconds)
        return validation
    except VideoValidationError as ve:
        raise HTTPException(status_code=422, detail=f"Video validation failed: {str(ve)}")
