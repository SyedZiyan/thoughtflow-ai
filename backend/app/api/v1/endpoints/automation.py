import json
from pathlib import Path
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, Job
from app.schemas.schemas import JobResponse
from app.services.pipeline_service import PipelineService
from app.core.config import settings

router = APIRouter()

def get_worker_heartbeat() -> Dict[str, Any]:
    """Inspects the worker heartbeat file written by the standalone worker daemon"""
    heartbeat_path = Path(settings.STORAGE_DIR) / "worker_heartbeat.json"
    if not heartbeat_path.exists():
        return {
            "worker_running": False,
            "status": "not_started",
            "message": "Worker daemon has not run yet. Run 'python worker/scheduler.py'."
        }

    try:
        data = json.loads(heartbeat_path.read_text(encoding="utf-8"))
        hb_time = datetime.fromisoformat(data.get("timestamp", ""))
        age_seconds = (datetime.now(timezone.utc) - hb_time).total_seconds()
        is_alive = age_seconds < 1800  # Alive if written within last 30 minutes

        return {
            "worker_running": is_alive,
            "status": data.get("status", "unknown"),
            "last_heartbeat": data.get("timestamp"),
            "age_seconds": round(age_seconds, 1),
            "last_action": data.get("last_action")
        }
    except Exception as e:
        return {
            "worker_running": False,
            "status": "error",
            "message": f"Could not read heartbeat: {e}"
        }

@router.post("/run")
async def trigger_pipeline(
    payload: Optional[Dict[str, Any]] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manually triggers the complete thought generation, video rendering, and publishing pipeline"""
    theme = payload.get("theme") if payload else None
    prompt = payload.get("custom_prompt") if payload else None

    try:
        result = await PipelineService.execute_pipeline(
            db=db,
            user=current_user,
            manual_theme=theme,
            manual_prompt=prompt
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/status")
def get_automation_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Returns worker daemon health, next scheduled time window, and latest job status"""
    user_settings = current_user.settings
    latest_job = db.query(Job).order_by(Job.created_at.desc()).first()
    heartbeat = get_worker_heartbeat()

    return {
        "automation_enabled": user_settings.automation_enabled if user_settings else False,
        "generation_time": user_settings.generation_time if user_settings else "07:00",
        "publishing_time": user_settings.publishing_time if user_settings else "09:00",
        "timezone": user_settings.timezone if user_settings else "UTC",
        "worker": heartbeat,
        "latest_job": JobResponse.model_validate(latest_job) if latest_job else None
    }

@router.get("/jobs", response_model=List[JobResponse])
def list_jobs(
    limit: int = 50,
    offset: int = 0,
    status_filter: Optional[str] = Query(None, alias="status"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Lists recent pipeline and background execution jobs"""
    query = db.query(Job)
    if status_filter:
        query = query.filter(Job.status == status_filter)
    jobs = query.order_by(Job.created_at.desc()).offset(offset).limit(limit).all()
    return [JobResponse.model_validate(j) for j in jobs]

@router.get("/jobs/{id}", response_model=JobResponse)
def get_job(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Inspects a specific background job by ID"""
    job = db.query(Job).filter(Job.id == id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found.")
    return JobResponse.model_validate(job)

@router.post("/jobs/{id}/retry", response_model=JobResponse)
async def retry_failed_job(
    id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Retries a failed pipeline job"""
    job = db.query(Job).filter(Job.id == id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found.")

    if job.status not in ["failed", "cancelled", "pending"]:
        raise HTTPException(status_code=400, detail=f"Job with status '{job.status}' does not need retry.")

    try:
        # Re-execute pipeline with same job key
        res = await PipelineService.execute_pipeline(
            db=db,
            user=current_user,
            job_key=job.job_key
        )
        db.refresh(job)
        return JobResponse.model_validate(job)
    except Exception as e:
        db.refresh(job)
        raise HTTPException(status_code=500, detail=f"Retry failed: {str(e)}")
