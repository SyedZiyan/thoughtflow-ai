from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, ActivityLog
from app.schemas.schemas import ActivityLogResponse

router = APIRouter()

@router.get("", response_model=List[ActivityLogResponse])
def get_activity_logs(
    limit: int = 50,
    offset: int = 0,
    event_type: Optional[str] = Query(None),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    query = db.query(ActivityLog).filter(ActivityLog.user_id == current_user.id)
    if event_type:
        query = query.filter(ActivityLog.event_type == event_type)
    logs = query.order_by(ActivityLog.created_at.desc()).offset(offset).limit(limit).all()
    return logs
