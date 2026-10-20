from typing import Generator, Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from app.db.session import get_db, SessionLocal
from app.core.security import decode_access_token
from app.models.models import User, UserSettings

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)

def get_or_create_default_user(db: Session) -> User:
    """Helper to ensure a default user exists for personal creator deployment"""
    user = db.query(User).filter(User.email == "creator@thoughtflow.local").first()
    if not user:
        from app.core.security import get_password_hash
        user = User(
            email="creator@thoughtflow.local",
            password_hash=get_password_hash("thoughtflow2026"),
            role="admin"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

        # Create initial default settings
        settings = UserSettings(
            user_id=user.id,
            timezone="UTC",
            generation_time="07:00",
            publishing_time="09:00",
            selected_themes=["motivation", "discipline", "consistency", "resilience"],
            language="en",
            video_duration=15,
            narration_enabled=False,
            auto_publish_enabled=False,
            automation_enabled=False,
            daily_limit=5,
            visual_theme="gradient_purple",
            animation_style="smooth_zoom"
        )
        db.add(settings)
        db.commit()
    return user

def get_current_user(
    token: Optional[str] = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    if token:
        user_id = decode_access_token(token)
        if user_id:
            user = db.query(User).filter(User.id == user_id).first()
            if user:
                return user
    
    # In personal single-user creator deployment mode, fallback to default creator account
    return get_or_create_default_user(db)
