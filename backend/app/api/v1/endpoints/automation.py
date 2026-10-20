from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Job
from app.schemas.schemas import JobResponse
from app.services.pipeline_service import PipelineService

router = APIRouter()

@router.post("/run")
async def trigger_pipeline(
    theme: Optional[str] = None,
    custom_prompt: Optional[str] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manually run the complete thought generation, video rendering, and publishing pipeline"""
    try:
        result = await PipelineService.execute_pipeline(
            db=db,
            user=current_user,
            manual_theme=theme,
            manual_prompt=custom_prompt
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/status")
def get_automation_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    settings = current_user.settings
    latest_job = db.query(Job).order_by(Job.created_at.desc()).first()
    return {
        "automation_enabled": settings.automation_enabled if settings else False,
        "generation_time": settings.generation_time if settings else "07:00",
        "publishing_time": settings.publishing_time if settings else "09:00",
        "timezone": settings.timezone if settings else "UTC",
        "latest_job": JobResponse.from_orm(latest_job) if latest_job else None
    }

@router.get("/jobs", response_model=List[JobResponse])
def list_jobs(
    limit: int = 50,
    offset: int = 0,
    status_filter: Optional[str] = Query(None, alias="status"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    query = db.query(Job)
    if status_filter:
        query = query.filter(Job.status == status_filter)
    jobs = query.order_by(Job.created_at.desc()).offset(offset).limit(limit).all()
    return jobs

@router.get("/jobs/{id}", response_model=JobResponse)
def get_job(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    job = db.query(Job).filter(Job.id == id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found.")
    return job
