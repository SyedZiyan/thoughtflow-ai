import asyncio
import pytest
from datetime import datetime, timezone, timedelta
from pydantic import ValidationError
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from app.models.models import Base, User, Thought, UserSettings, ActivityLog
from app.services.ai_provider import (
    BaseAIProvider,
    MockAIProvider,
    StructuredThought,
    TokenUsage,
    normalize_text,
    compute_content_hash,
    check_near_duplicate,
    get_ai_provider
)
from app.services.thought_service import ThoughtService, DuplicateThoughtError

# Create an isolated in-memory test database for reliable, lightning-fast testing
test_engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False})
TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)

@pytest.fixture(scope="function")
def db_session():
    Base.metadata.create_all(bind=test_engine)
    session = TestSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=test_engine)

@pytest.fixture
def test_user(db_session: Session):
    user = User(
        email="test_creator@thoughtflow.ai",
        password_hash="test_password_hash",
        role="creator"
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)

    settings = UserSettings(
        user_id=user.id,
        selected_themes=["discipline", "resilience", "consistency"]
    )
    db_session.add(settings)
    db_session.commit()
    db_session.refresh(user)
    return user


# ==========================================
# 1. PROVIDER INTERFACES
# ==========================================

def test_mock_ai_provider_all_themes():
    provider = MockAIProvider()
    themes = [
        "motivation", "discipline", "consistency", "resilience",
        "success", "mindfulness", "productivity", "confidence",
        "self-improvement", "life lessons", "relationships", "personal growth"
    ]
    for theme in themes:
        thought = asyncio.run(provider.generate_thought(theme=theme))
        assert isinstance(thought, StructuredThought)
        assert thought.theme == theme
        assert len(thought.thought) >= 10
        assert "#Shorts" in thought.title or "#shorts" in thought.title.lower()
        assert "shorts" in thought.hashtags
        assert thought.token_usage is not None
        assert thought.token_usage.total_tokens > 0

def test_get_ai_provider_factory():
    provider = get_ai_provider("mock")
    assert isinstance(provider, MockAIProvider)


# ==========================================
# 2. STRUCTURED OUTPUT VALIDATION
# ==========================================

def test_structured_thought_validation_valid():
    st = StructuredThought(
        theme="discipline",
        thought="Small daily habits build invincible character over time.",
        title="Invincible Habits",
        description="A daily reminder that discipline always triumphs over fleeting enthusiasm.",
        hashtags=["#Mindset!", "discipline", "HABITS"],
        language="en"
    )
    assert st.theme == "discipline"
    assert st.title.endswith("#Shorts")
    assert "mindset" in st.hashtags
    assert "shorts" in st.hashtags

def test_structured_thought_validation_thought_too_short():
    with pytest.raises(ValidationError):
        StructuredThought(
            theme="discipline",
            thought="Too short",
            title="Short Title #Shorts",
            description="Valid long description for testing."
        )

def test_structured_thought_validation_empty_theme():
    with pytest.raises(ValidationError):
        StructuredThought(
            theme="   ",
            thought="A valid inspiring thought that has sufficient character length.",
            title="Valid Title #Shorts",
            description="Valid long description for testing."
        )

def test_structured_thought_validation_empty_description():
    with pytest.raises(ValidationError):
        StructuredThought(
            theme="motivation",
            thought="A valid inspiring thought that has sufficient character length.",
            title="Valid Title #Shorts",
            description=""
        )


# ==========================================
# 3. TEXT NORMALIZATION & DUPLICATE CHECKS
# ==========================================

def test_normalization_and_content_hash():
    text_a = "Discipline is choosing between what you want NOW, and what you want MOST!"
    text_b = "  discipline is choosing between what you want now and what you want most.  "
    
    assert normalize_text(text_a) == normalize_text(text_b)
    assert compute_content_hash(text_a) == compute_content_hash(text_b)

def test_near_duplicate_overlap():
    existing_list = [
        "Small steps taken consistently create extraordinary long-term transformations.",
        "Your future is built by what you do today not tomorrow."
    ]

    candidate_near = "Small steps taken consistently create extraordinary transformations in life."
    is_dup, match = check_near_duplicate(candidate_near, existing_list, threshold=0.70)
    assert is_dup is True
    assert match == existing_list[0]

    candidate_unique = "Courage is the quiet voice at the end of the day saying I will try again tomorrow."
    is_dup2, _ = check_near_duplicate(candidate_unique, existing_list, threshold=0.70)
    assert is_dup2 is False


# ==========================================
# 4. THEME ROTATION
# ==========================================

def test_theme_rotation_least_recently_used(db_session: Session, test_user: User):
    pool = ["discipline", "resilience", "consistency"]

    # Initially, none have been generated -> should pick the first unused theme
    next_theme = ThoughtService.select_next_theme(test_user.id, db_session, pool)
    assert next_theme in pool

    # Add a thought for "discipline"
    t1 = Thought(
        user_id=test_user.id,
        text="Discipline test thought.",
        theme="discipline",
        language="en",
        title="Title 1 #Shorts",
        description="Desc 123456789012345",
        hashtags=["shorts"],
        content_hash=compute_content_hash("Discipline test thought."),
        status="approved"
    )
    db_session.add(t1)
    db_session.commit()

    # Now "resilience" and "consistency" are unused -> next selection must be one of them
    next_theme_2 = ThoughtService.select_next_theme(test_user.id, db_session, pool)
    assert next_theme_2 in ["resilience", "consistency"]


# ==========================================
# 5. DATABASE PERSISTENCE & SERVICE ENGINE
# ==========================================

def test_generate_and_save_thought_to_db(db_session: Session, test_user: User):
    thought = asyncio.run(ThoughtService.generate_and_save_thought(
        db=db_session,
        user=test_user,
        theme="resilience",
        tone="stoic",
        language="en"
    ))
    assert thought.id is not None
    assert thought.user_id == test_user.id
    assert thought.theme == "resilience"
    assert len(thought.content_hash) == 64
    assert thought.status == "approved"

    # Verify persistence in database
    queried = db_session.query(Thought).filter(Thought.id == thought.id).first()
    assert queried is not None
    assert queried.text == thought.text

    # Verify ActivityLog entry was created
    activity = (
        db_session.query(ActivityLog)
        .filter(ActivityLog.user_id == test_user.id, ActivityLog.event_type == "thought_generated")
        .order_by(ActivityLog.created_at.desc())
        .first()
    )
    assert activity is not None
    assert "resilience" in activity.message

def test_duplicate_thought_detection_in_db(db_session: Session, test_user: User):
    thought1 = asyncio.run(ThoughtService.generate_and_save_thought(
        db=db_session,
        user=test_user,
        theme="consistency"
    ))

    is_dup, reason, matched = ThoughtService.check_is_duplicate(thought1.text, test_user.id, db_session)
    assert is_dup is True
    assert reason == "exact_duplicate"
    assert matched == thought1.text
