import logging
from typing import Generator, Optional
from fastapi import Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from app.db.session import get_db, SessionLocal
from app.core.security import decode_access_token
from app.core.config import settings
from app.models.models import User, UserSettings

logger = logging.getLogger("thoughtflow.auth")

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
    request: Request,
    token: Optional[str] = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    """
    Authenticates and resolves the current user:
    1. Authorization: Bearer <jwt_token>
    2. X-API-Key: <APP_SECRET_KEY> header
    3. If unauthenticated:
       - In production OR when AUTH_REQUIRED=True: raises HTTP 401 Unauthorized
       - In development/testing: falls back to default creator user with explicit audit log
    """
    # 1. Bearer Token
    if token:
        user_id = decode_access_token(token)
        if user_id:
            user = db.query(User).filter(User.id == user_id).first()
            if user:
                return user
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid, expired, or malformed authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 2. Programmatic API Key Header
    api_key = request.headers.get("X-API-Key")
    if api_key and api_key == settings.APP_SECRET_KEY:
        return get_or_create_default_user(db)

    # 3. Production & Auth-Enforced Mode Check
    if settings.AUTH_REQUIRED or settings.ENVIRONMENT == "production":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required. Please provide a valid Bearer token or API key.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 4. Safe Development fallback with audit warning
    if settings.ENVIRONMENT != "test":
        logger.debug(f"Unauthenticated request to {request.url.path} served via development fallback.")
    return get_or_create_default_user(db)
