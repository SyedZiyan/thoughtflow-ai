from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.api.deps import get_current_user
from app.core.security import verify_password, create_access_token, get_password_hash
from app.models.models import User, UserSettings
from app.schemas.schemas import UserLogin, Token, UserResponse

router = APIRouter()

@router.post("/login", response_model=Token)
def login(credentials: UserLogin, db: Session = Depends(get_db)):
    clean_email = credentials.email.strip().lower()
    user = db.query(User).filter(User.email == clean_email).first()
    if not user or not verify_password(credentials.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = create_access_token(subject=user.id)
    return Token(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.from_orm(user)
    )

@router.post("/register", response_model=Token)
def register(credentials: UserLogin, db: Session = Depends(get_db)):
    clean_email = credentials.email.strip().lower()
    existing = db.query(User).filter(User.email == clean_email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User with this email already exists."
        )

    user = User(
        email=clean_email,
        password_hash=get_password_hash(credentials.password),
        role="creator"
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
    db.commit()

    access_token = create_access_token(subject=user.id)
    return Token(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.from_orm(user)
    )

@router.get("/me", response_model=UserResponse)
def get_current_user_profile(current_user: User = Depends(get_current_user)):
    return UserResponse.from_orm(current_user)
