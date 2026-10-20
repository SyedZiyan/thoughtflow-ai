from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.models.models import User, UserSettings, ActivityLog
from app.schemas.schemas import SettingsResponse, SettingsUpdate

router = APIRouter()

@router.get("", response_model=SettingsResponse)
def get_settings(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    settings = current_user.settings
    if not settings:
        settings = UserSettings(user_id=current_user.id)
        db.add(settings)
        db.commit()
        db.refresh(settings)
    return settings

@router.patch("", response_model=SettingsResponse)
def update_settings(
    req: SettingsUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    settings = current_user.settings
    if not settings:
        settings = UserSettings(user_id=current_user.id)
        db.add(settings)

    if req.timezone is not None:
        settings.timezone = req.timezone
    if req.generation_time is not None:
        settings.generation_time = req.generation_time
    if req.publishing_time is not None:
        settings.publishing_time = req.publishing_time
    if req.selected_themes is not None:
        settings.selected_themes = req.selected_themes
    if req.language is not None:
        settings.language = req.language
    if req.video_duration is not None:
        settings.video_duration = req.video_duration
    if req.narration_enabled is not None:
        settings.narration_enabled = req.narration_enabled
    if req.auto_publish_enabled is not None:
        settings.auto_publish_enabled = req.auto_publish_enabled
    if req.automation_enabled is not None:
        settings.automation_enabled = req.automation_enabled
    if req.daily_limit is not None:
        settings.daily_limit = req.daily_limit
    if req.visual_theme is not None:
        settings.visual_theme = req.visual_theme
    if req.animation_style is not None:
        settings.animation_style = req.animation_style

    log = ActivityLog(
        user_id=current_user.id,
        event_type="settings_updated",
        message="Automation and publishing settings updated."
    )
    db.add(log)
    db.commit()
    db.refresh(settings)
    return settings
