import logging
from typing import Generator, Optional
from fastapi import Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from app.db.session import get_db, SessionLocal
from app.core.security import decode_access_token
from app.core.config import settings
from app.models.models import User

logger = logging.getLogger("thoughtflow.auth")

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)


def get_current_user(
    request: Request,
    token: Optional[str] = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    """
    Authenticates and resolves the current user with strict mandatory authentication:
    1. Authorization: Bearer <jwt_token>
    2. X-API-Key: <APP_SECRET_KEY> header (for authorized background/service operations)
    
    If token is missing, invalid, expired, or user not found:
    Raises HTTP 401 Unauthorized immediately.
    There is ZERO fallback to any default user.
    """
    # 1. Bearer Token
    if token:
        user_id = decode_access_token(token)
        if not user_id:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid, expired, or malformed authentication token.",
                headers={"WWW-Authenticate": "Bearer"},
            )
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User account associated with this token no longer exists.",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return user

    # 2. Programmatic API Key Header (Authorized internal/service access)
    api_key = request.headers.get("X-API-Key")
    if api_key and api_key == settings.APP_SECRET_KEY:
        # Resolve an administrator or the primary active registered user
        admin_user = db.query(User).filter(User.role == "admin").first()
        if not admin_user:
            admin_user = db.query(User).first()
        if admin_user:
            return admin_user
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No registered user account found for API key authorization. Initialize administrator first.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 3. Missing credentials -> Strict 401 Unauthorized
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Authentication required. Please provide a valid Bearer token.",
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_current_admin_user(
    current_user: User = Depends(get_current_user)
) -> User:
    """Requires the authenticated user to hold the 'admin' role"""
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Forbidden: Administrator privileges required.",
        )
    return current_user
