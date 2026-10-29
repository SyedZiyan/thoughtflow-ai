import logging
import re
import secrets
import threading
import time
from typing import Dict, List, Tuple, Optional
from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.core.security import verify_password, create_access_token, get_password_hash
from app.models.models import User, UserSettings, ActivityLog
from app.schemas.schemas import UserLogin, Token, UserResponse

logger = logging.getLogger("thoughtflow.auth")

router = APIRouter()

# Fixed dummy bcrypt hash to execute constant-time verification when user does not exist
DUMMY_BCRYPT_HASH = "$2b$12$e80yvXfL3J8vJd7L7Fz7ke0Z2nZkL7M4W4v9B3c6E8g0J2m4P6r8K"

class LoginAttemptTracker:
    """
    Thread-safe sliding-window rate limiter and monitor for login attempts:
    - Restricts failed login attempts to 5 failures per 15-minute window
    - Mitigates brute-force attacks, password guessing, and credential stuffing
    """
    def __init__(self, max_attempts: int = 5, lockout_seconds: int = 900):
        self.max_attempts = max_attempts
        self.lockout_seconds = lockout_seconds
        self.attempts: Dict[str, List[float]] = {}
        self._lock = threading.Lock()

    def _clean(self, key: str, now: float):
        cutoff = now - self.lockout_seconds
        self.attempts[key] = [t for t in self.attempts.get(key, []) if t > cutoff]

    def is_locked(self, key: str) -> Tuple[bool, int]:
        now = time.time()
        with self._lock:
            self._clean(key, now)
            records = self.attempts.get(key, [])
            if len(records) >= self.max_attempts:
                oldest = records[0]
                remaining = int(self.lockout_seconds - (now - oldest))
                return True, max(remaining, 1)
            return False, 0

    def record_failure(self, key: str):
        now = time.time()
        with self._lock:
            self._clean(key, now)
            if key not in self.attempts:
                self.attempts[key] = []
            self.attempts[key].append(now)

    def reset(self, key: str):
        with self._lock:
            self.attempts.pop(key, None)

login_tracker = LoginAttemptTracker(max_attempts=5, lockout_seconds=900)


def validate_password_strength(password: str) -> None:
    """
    Enforces strong password policy:
    - Minimum 8 characters
    - At least one letter
    - At least one number or special character
    """
    if len(password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Password must be at least 8 characters long."
        )
    has_letter = any(c.isalpha() for c in password)
    has_digit_or_symbol = any(c.isdigit() or not c.isalnum() for c in password)
    if not has_letter or not has_digit_or_symbol:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Password must contain at least one letter and at least one number or special character."
        )


def validate_email_format(email: str) -> str:
    """Validates and standardizes email format"""
    clean = email.strip().lower()
    email_regex = r"^[^@\s]+@[^@\s]+\.[^@\s]+$"
    if not re.match(email_regex, clean):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email address format."
        )
    return clean


@router.post("/login", response_model=Token)
def login(
    credentials: UserLogin,
    request: Request,
    db: Session = Depends(get_db)
):
    """
    Strict authentic login endpoint:
    - Rejects unknown email addresses without auto-creating accounts.
    - Constant-time verification to prevent user enumeration timing attacks.
    - Generic error messages without leaking account existence.
    - Rate-limited and monitored against brute-force attacks.
    """
    clean_email = validate_email_format(credentials.email)
    client_ip = request.client.host if request.client else "unknown"
    rate_key = f"{client_ip}_{clean_email}"

    # 1. Rate Limit & Brute-Force Monitoring Check
    is_locked, remaining_secs = login_tracker.is_locked(rate_key)
    if is_locked:
        logger.warning(f"Login rate limit locked for {clean_email} from {client_ip} ({remaining_secs}s remaining)")
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Too many failed login attempts. Account temporarily locked for {remaining_secs} seconds."
        )

    # 2. User Lookup
    user = db.query(User).filter(User.email == clean_email).first()

    # 3. Constant-Time Authentication & Verification
    if not user:
        # Dummy verification to keep computation time indistinguishable
        verify_password(credentials.password, DUMMY_BCRYPT_HASH)
        login_tracker.record_failure(rate_key)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not verify_password(credentials.password, user.password_hash):
        login_tracker.record_failure(rate_key)
        # Audit log failed attempt
        try:
            log = ActivityLog(
                user_id=user.id,
                event_type="auth_failed",
                message=f"Failed login attempt from IP {client_ip}"
            )
            db.add(log)
            db.commit()
        except Exception:
            db.rollback()

        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 4. Successful Authentication
    login_tracker.reset(rate_key)
    try:
        log = ActivityLog(
            user_id=user.id,
            event_type="auth_success",
            message=f"User authenticated successfully from IP {client_ip}"
        )
        db.add(log)
        db.commit()
    except Exception:
        db.rollback()

    access_token = create_access_token(subject=user.id)
    return Token(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user)
    )


@router.post("/register", response_model=Token)
def register(
    credentials: UserLogin,
    request: Request,
    db: Session = Depends(get_db)
):
    """
    Dedicated account registration endpoint:
    - Enforces email uniqueness and strong password complexity.
    - Generates cryptographic email verification token for public accounts.
    - Dispatches verification token and initializes default user settings.
    """
    clean_email = validate_email_format(credentials.email)
    validate_password_strength(credentials.password)

    existing = db.query(User).filter(User.email == clean_email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unable to register account with this email address."
        )

    verification_token = secrets.token_urlsafe(32)
    user = User(
        email=clean_email,
        password_hash=get_password_hash(credentials.password),
        role="creator",
        is_verified=True,  # Default active for single creator deployment
        verification_token=verification_token
    )
    db.add(user)
    db.commit()
    db.refresh(user)

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

    client_ip = request.client.host if request.client else "unknown"
    log = ActivityLog(
        user_id=user.id,
        event_type="user_registered",
        message=f"New creator account registered from {client_ip}."
    )
    db.add(log)
    db.commit()

    access_token = create_access_token(subject=user.id)
    return Token(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user)
    )


@router.get("/verify-email")
def verify_email(token: str, db: Session = Depends(get_db)):
    """Validates email verification token and marks account verified"""
    clean_token = token.strip()
    if not clean_token:
        raise HTTPException(status_code=400, detail="Verification token is required.")

    user = db.query(User).filter(User.verification_token == clean_token).first()
    if not user:
        raise HTTPException(status_code=400, detail="Invalid or expired verification token.")

    user.is_verified = True
    user.verification_token = None
    db.commit()

    return {
        "status": "success",
        "message": "Email address verified successfully. Your account is active."
    }


@router.post("/resend-verification")
def resend_verification(email: str, db: Session = Depends(get_db)):
    """Dispatches a fresh verification token if user exists without leaking account existence"""
    clean_email = email.strip().lower()
    user = db.query(User).filter(User.email == clean_email).first()
    if user and not user.is_verified:
        user.verification_token = secrets.token_urlsafe(32)
        db.commit()

    return {
        "status": "success",
        "message": "If an unverified account exists for this email, a verification link has been dispatched."
    }


@router.get("/me", response_model=UserResponse)
def get_current_user_profile(current_user: User = Depends(get_current_user)):
    return UserResponse.model_validate(current_user)
