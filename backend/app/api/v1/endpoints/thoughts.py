from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Thought, Video
from app.schemas.schemas import ThoughtGenerateRequest, ThoughtResponse, ThoughtUpdate
from app.services.ai_provider import get_ai_provider, compute_content_hash
from app.services.video_renderer import video_renderer

router = APIRouter()

@router.get("", response_model=List[ThoughtResponse])
def list_thoughts(
    theme: Optional[str] = None,
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = 50,
    offset: int = 0,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    query = db.query(Thought).filter(Thought.user_id == current_user.id)
    if theme:
        query = query.filter(Thought.theme == theme.lower())
    if status_filter:
        query = query.filter(Thought.status == status_filter)
    
    thoughts = query.order_by(Thought.created_at.desc()).offset(offset).limit(limit).all()
    return thoughts

from app.services.thought_service import ThoughtService, DuplicateThoughtError

@router.post("/generate", response_model=ThoughtResponse)
async def generate_thought(
    req: ThoughtGenerateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    try:
        thought = await ThoughtService.generate_and_save_thought(
            db=db,
            user=current_user,
            theme=req.theme,
            tone=req.tone or "inspiring",
            language=req.language or "en",
            custom_prompt=req.custom_prompt or "",
            max_attempts=3,
            status="approved"
        )
    except DuplicateThoughtError as e:
        raise HTTPException(status_code=409, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Generation failed: {str(e)}")

    # Optional auto-render if requested
    if req.auto_render:
        try:
            render_res = video_renderer.render_animated_video(
                thought_id=thought.id,
                thought_text=thought.text,
                theme=thought.theme
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
            db.commit()
        except Exception as e:
            # Keep thought approved even if video render fails
            pass

    return thought

@router.get("/{id}", response_model=ThoughtResponse)
def get_thought(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    thought = db.query(Thought).filter(Thought.id == id, Thought.user_id == current_user.id).first()
    if not thought:
        raise HTTPException(status_code=404, detail="Thought not found.")
    return thought

@router.patch("/{id}", response_model=ThoughtResponse)
def update_thought(
    id: str,
    req: ThoughtUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    thought = db.query(Thought).filter(Thought.id == id, Thought.user_id == current_user.id).first()
    if not thought:
        raise HTTPException(status_code=404, detail="Thought not found.")

    if req.text is not None:
        thought.text = req.text
        thought.content_hash = compute_content_hash(req.text)
    if req.theme is not None:
        thought.theme = req.theme
    if req.title is not None:
        thought.title = req.title
    if req.description is not None:
        thought.description = req.description
    if req.hashtags is not None:
        thought.hashtags = req.hashtags
    if req.narration_script is not None:
        thought.narration_script = req.narration_script
    if req.status is not None:
        thought.status = req.status

    db.commit()
    db.refresh(thought)
    return thought

@router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_thought(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    thought = db.query(Thought).filter(Thought.id == id, Thought.user_id == current_user.id).first()
    if not thought:
        raise HTTPException(status_code=404, detail="Thought not found.")
    db.delete(thought)
    db.commit()
    return None
