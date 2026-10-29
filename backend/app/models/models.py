import uuid
from datetime import datetime, timezone
from sqlalchemy import (
    Column, String, Text, Integer, Boolean, DateTime, ForeignKey, JSON, Index, UniqueConstraint
)
from sqlalchemy.orm import relationship
from app.db.session import Base

def generate_uuid():
    return str(uuid.uuid4())

def utc_now():
    return datetime.now(timezone.utc)

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    email = Column(String(255), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    role = Column(String(50), default="admin", nullable=False)
    is_verified = Column(Boolean, default=True, nullable=False)
    verification_token = Column(String(255), nullable=True, index=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)
    updated_at = Column(DateTime(timezone=True), default=utc_now, onupdate=utc_now, nullable=False)

    thoughts = relationship("Thought", back_populates="user", cascade="all, delete-orphan")
    settings = relationship("UserSettings", back_populates="user", uselist=False, cascade="all, delete-orphan")
    integrations = relationship("Integration", back_populates="user", cascade="all, delete-orphan")
    activity_logs = relationship("ActivityLog", back_populates="user", cascade="all, delete-orphan")
    jobs = relationship("Job", back_populates="user", cascade="all, delete-orphan")
    oauth_states = relationship("OAuthState", back_populates="user", cascade="all, delete-orphan")

class Thought(Base):
    __tablename__ = "thoughts"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    text = Column(Text, nullable=False)
    theme = Column(String(100), nullable=False, index=True)
    language = Column(String(10), default="en", nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=False)
    hashtags = Column(JSON, default=list, nullable=False)
    narration_script = Column(Text, nullable=True)
    content_hash = Column(String(64), index=True, nullable=False)
    prompt_version = Column(String(50), default="v1.0", nullable=False)
    status = Column(String(50), default="draft", nullable=False)  # draft, approved, rendered, rejected
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    user = relationship("User", back_populates="thoughts")
    videos = relationship("Video", back_populates="thought", cascade="all, delete-orphan")

class Video(Base):
    __tablename__ = "videos"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    thought_id = Column(String(36), ForeignKey("thoughts.id", ondelete="CASCADE"), nullable=False, index=True)
    storage_key = Column(String(500), nullable=False)
    thumbnail_key = Column(String(500), nullable=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=False)
    duration_seconds = Column(Integer, default=15, nullable=False)
    width = Column(Integer, default=1080, nullable=False)
    height = Column(Integer, default=1920, nullable=False)
    render_status = Column(String(50), default="pending", nullable=False)  # pending, rendering, completed, failed
    upload_status = Column(String(50), default="not_uploaded", nullable=False)  # not_uploaded, uploading, published, scheduled, failed
    youtube_video_id = Column(String(100), nullable=True, index=True)
    youtube_url = Column(String(500), nullable=True)
    scheduled_at = Column(DateTime(timezone=True), nullable=True)
    published_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    thought = relationship("Thought", back_populates="videos")

class Job(Base):
    __tablename__ = "jobs"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=True, index=True)
    job_key = Column(String(255), unique=True, index=True, nullable=False)  # guarantees idempotency
    job_type = Column(String(100), nullable=False)  # daily_pipeline, manual_generation, manual_render, youtube_upload
    status = Column(String(50), default="pending", nullable=False)  # pending, generating, rendering, validating, uploading, scheduled, published, failed, cancelled
    current_stage = Column(String(100), default="initialized", nullable=False)
    attempts = Column(Integer, default=0, nullable=False)
    max_attempts = Column(Integer, default=3, nullable=False)
    error_message = Column(Text, nullable=True)
    job_metadata = Column(JSON, default=dict, nullable=True)
    started_at = Column(DateTime(timezone=True), nullable=True)
    finished_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    user = relationship("User", back_populates="jobs")

class UserSettings(Base):
    __tablename__ = "user_settings"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False)
    timezone = Column(String(100), default="UTC", nullable=False)
    generation_time = Column(String(10), default="07:00", nullable=False)
    publishing_time = Column(String(10), default="09:00", nullable=False)
    selected_themes = Column(JSON, default=lambda: ["motivation", "discipline", "consistency", "resilience"], nullable=False)
    language = Column(String(10), default="en", nullable=False)
    video_duration = Column(Integer, default=15, nullable=False)
    narration_enabled = Column(Boolean, default=False, nullable=False)
    auto_publish_enabled = Column(Boolean, default=False, nullable=False)
    automation_enabled = Column(Boolean, default=False, nullable=False)
    daily_limit = Column(Integer, default=5, nullable=False)
    visual_theme = Column(String(50), default="gradient_purple", nullable=False)
    animation_style = Column(String(50), default="smooth_zoom", nullable=False)

    user = relationship("User", back_populates="settings")

class Integration(Base):
    __tablename__ = "integrations"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    provider = Column(String(50), default="youtube", nullable=False)
    encrypted_credentials = Column(Text, nullable=True)
    connection_status = Column(String(50), default="disconnected", nullable=False)  # connected, disconnected, expired
    channel_id = Column(String(100), nullable=True)
    channel_title = Column(String(255), nullable=True)
    channel_metadata = Column(JSON, default=dict, nullable=True)
    connected_at = Column(DateTime(timezone=True), nullable=True)
    updated_at = Column(DateTime(timezone=True), default=utc_now, onupdate=utc_now, nullable=False)

    user = relationship("User", back_populates="integrations")

    __table_args__ = (
        UniqueConstraint('user_id', 'provider', name='uq_user_provider'),
    )

class ActivityLog(Base):
    __tablename__ = "activity_logs"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    job_id = Column(String(36), nullable=True, index=True)
    event_type = Column(String(100), nullable=False, index=True)
    message = Column(Text, nullable=False)
    log_metadata = Column(JSON, default=dict, nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    user = relationship("User", back_populates="activity_logs")

class OAuthState(Base):
    __tablename__ = "oauth_states"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    state_token = Column(String(255), unique=True, index=True, nullable=False)
    provider = Column(String(50), default="youtube", nullable=False)
    is_used = Column(Boolean, default=False, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    consumed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    user = relationship("User", back_populates="oauth_states")
