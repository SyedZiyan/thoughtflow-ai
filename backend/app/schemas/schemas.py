from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, EmailStr, Field, ConfigDict

# User
class UserBase(BaseModel):
    email: EmailStr

class UserCreate(UserBase):
    password: str

class UserResponse(UserBase):
    id: str
    role: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class UserLogin(BaseModel):
    email: EmailStr
    password: str

# Thought
class ThoughtGenerateRequest(BaseModel):
    theme: Optional[str] = None
    tone: Optional[str] = "inspiring"
    language: Optional[str] = "en"
    custom_prompt: Optional[str] = None
    auto_render: Optional[bool] = False

class ThoughtCreate(BaseModel):
    text: str
    theme: str
    language: str = "en"
    title: str
    description: str
    hashtags: List[str] = []
    narration_script: Optional[str] = None
    prompt_version: str = "v1.0"
    status: str = "draft"

class ThoughtUpdate(BaseModel):
    text: Optional[str] = None
    theme: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    hashtags: Optional[List[str]] = None
    narration_script: Optional[str] = None
    status: Optional[str] = None

class ThoughtResponse(BaseModel):
    id: str
    user_id: str
    text: str
    theme: str
    language: str
    title: str
    description: str
    hashtags: List[str]
    narration_script: Optional[str] = None
    content_hash: str
    prompt_version: str
    status: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

# Video
class VideoRenderRequest(BaseModel):
    thought_id: str
    duration_seconds: Optional[int] = 15
    visual_theme: Optional[str] = "gradient_purple"
    animation_style: Optional[str] = "smooth_zoom"
    typography: Optional[str] = "inter"
    narration_enabled: Optional[bool] = False

class VideoResponse(BaseModel):
    id: str
    thought_id: str
    storage_key: str
    video_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    title: str
    description: str
    duration_seconds: int
    width: int
    height: int
    render_status: str
    upload_status: str
    youtube_video_id: Optional[str] = None
    youtube_url: Optional[str] = None
    scheduled_at: Optional[datetime] = None
    published_at: Optional[datetime] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

# Job
class JobResponse(BaseModel):
    id: str
    job_key: str
    job_type: str
    status: str
    current_stage: str
    attempts: int
    max_attempts: int
    error_message: Optional[str] = None
    job_metadata: Optional[Dict[str, Any]] = None
    started_at: Optional[datetime] = None
    finished_at: Optional[datetime] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

# Settings
class SettingsUpdate(BaseModel):
    timezone: Optional[str] = None
    generation_time: Optional[str] = None
    publishing_time: Optional[str] = None
    selected_themes: Optional[List[str]] = None
    language: Optional[str] = None
    video_duration: Optional[int] = None
    narration_enabled: Optional[bool] = None
    auto_publish_enabled: Optional[bool] = None
    automation_enabled: Optional[bool] = None
    daily_limit: Optional[int] = None
    visual_theme: Optional[str] = None
    animation_style: Optional[str] = None

class SettingsResponse(BaseModel):
    id: str
    user_id: str
    timezone: str
    generation_time: str
    publishing_time: str
    selected_themes: List[str]
    language: str
    video_duration: int
    narration_enabled: bool
    auto_publish_enabled: bool
    automation_enabled: bool
    daily_limit: int
    visual_theme: str
    animation_style: str

    model_config = ConfigDict(from_attributes=True)

# YouTube
class YouTubeStatusResponse(BaseModel):
    connected: bool
    channel_id: Optional[str] = None
    channel_title: Optional[str] = None
    channel_thumbnail: Optional[str] = None
    subscriber_count: Optional[int] = None
    video_count: Optional[int] = None
    view_count: Optional[int] = None
    connected_at: Optional[datetime] = None
    upload_enabled: bool
    dry_run_mode: bool

class YouTubeUploadRequest(BaseModel):
    video_id: str
    title: Optional[str] = None
    description: Optional[str] = None
    tags: Optional[List[str]] = None
    privacy_status: Optional[str] = "private"
    publish_at: Optional[datetime] = None

# Activity Log
class ActivityLogResponse(BaseModel):
    id: str
    user_id: str
    job_id: Optional[str] = None
    event_type: str
    message: str
    log_metadata: Optional[Dict[str, Any]] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

# Dashboard
class DashboardResponse(BaseModel):
    todays_generation_status: str
    todays_publishing_status: str
    next_scheduled_execution: Optional[str] = None
    total_thoughts: int
    total_videos: int
    total_published: int
    failed_jobs: int
    youtube_connected: bool
    automation_enabled: bool
    recent_activity: List[ActivityLogResponse]
    upcoming_schedule: List[Dict[str, Any]]
    latest_videos: List[VideoResponse]

# Analytics
class AnalyticsResponse(BaseModel):
    total_published_videos: int
    total_views: int
    total_likes: int
    total_comments: int
    success_rate: float
    performance_by_theme: List[Dict[str, Any]]
    recent_uploads: List[Dict[str, Any]]
