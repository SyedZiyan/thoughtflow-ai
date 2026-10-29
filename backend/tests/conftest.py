import sys
from pathlib import Path
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

# Add backend and root directory to sys.path so 'import app...' and 'import worker...' work anywhere
backend_dir = Path(__file__).resolve().parent.parent
root_dir = backend_dir.parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))
if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))

from app.main import app
from app.db.session import SessionLocal, Base, engine
from app.models.models import User, UserSettings
from app.core.security import get_password_hash, create_access_token


def get_or_create_test_user(
    db: Session,
    email: str = "test_creator@thoughtflow.ai",
    role: str = "creator"
) -> User:
    """Helper for test suites to ensure a test user with settings exists"""
    user = db.query(User).filter(User.email == email).first()
    if not user:
        user = User(
            email=email,
            password_hash=get_password_hash("test_password_12345"),
            role=role
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
        db.refresh(user)
    return user


def get_test_auth_headers(user: User) -> dict:
    """Generates valid Bearer authentication headers for a given user"""
    token = create_access_token(subject=user.id)
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="session", autouse=True)
def setup_test_tables():
    from sqlalchemy import text
    Base.metadata.create_all(bind=engine)
    with engine.connect() as conn:
        for sql in [
            "ALTER TABLE jobs ADD COLUMN user_id VARCHAR(36)",
            "ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT 1",
            "ALTER TABLE users ADD COLUMN verification_token VARCHAR(255)"
        ]:
            try:
                conn.execute(text(sql))
                conn.commit()
            except Exception:
                pass
    yield


@pytest.fixture
def db():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def test_user(db: Session) -> User:
    return get_or_create_test_user(db)


@pytest.fixture
def auth_headers(test_user: User) -> dict:
    return get_test_auth_headers(test_user)


@pytest.fixture
def auth_client(auth_headers: dict) -> TestClient:
    client = TestClient(app)
    client.headers.update(auth_headers)
    return client
