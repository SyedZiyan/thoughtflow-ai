import os
from pathlib import Path
from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

BASE_DIR = Path(__file__).resolve().parent.parent.parent
STORAGE_DEFAULT = BASE_DIR.parent / "storage"

import shutil

def _find_ffmpeg() -> str:
    if shutil.which("ffmpeg"):
        return "ffmpeg"
    local_app_data = os.environ.get("LOCALAPPDATA", "")
    if local_app_data:
        winget_pattern = Path(local_app_data) / "Microsoft" / "WinGet" / "Packages"
        if winget_pattern.exists():
            for p in winget_pattern.glob("**/ffmpeg.exe"):
                return str(p)
    return "ffmpeg"

def _find_ffprobe() -> str:
    if shutil.which("ffprobe"):
        return "ffprobe"
    local_app_data = os.environ.get("LOCALAPPDATA", "")
    if local_app_data:
        winget_pattern = Path(local_app_data) / "Microsoft" / "WinGet" / "Packages"
        if winget_pattern.exists():
            for p in winget_pattern.glob("**/ffprobe.exe"):
                return str(p)
    return "ffprobe"

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # App
    PROJECT_NAME: str = "ThoughtFlow AI"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    AUTH_REQUIRED: bool = True
    APP_SECRET_KEY: str = "dev_secret_thoughtflow_change_in_production_key_12345"
    CORS_ORIGINS: List[str] = [
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000",
        "http://localhost:8081",
        "http://localhost:19006",
        "exp://localhost:8081"
    ]

    # Database
    DATABASE_URL: str = "sqlite:///./thoughtflow.db"

    # Storage
    STORAGE_DIR: str = str(STORAGE_DEFAULT)
    VIDEO_STORAGE_DIR: str = str(STORAGE_DEFAULT / "videos")
    TEMP_STORAGE_DIR: str = str(STORAGE_DEFAULT / "temp")

    # Media & Rendering
    FFMPEG_PATH: str = Field(default_factory=_find_ffmpeg)
    FFPROBE_PATH: str = Field(default_factory=_find_ffprobe)
    DEFAULT_VIDEO_WIDTH: int = 1080
    DEFAULT_VIDEO_HEIGHT: int = 1920
    DEFAULT_FPS: int = 30
    DEFAULT_VIDEO_DURATION_SECONDS: int = 15

    # AI Provider
    AI_PROVIDER: str = "mock"
    GEMINI_API_KEY: Optional[str] = None
    OPENAI_API_KEY: Optional[str] = None
    AI_MODEL_NAME: str = "gemini-1.5-flash"

    # Text-to-Speech
    TTS_PROVIDER: str = "none"
    ELEVENLABS_API_KEY: Optional[str] = None
    TTS_VOICE_ID: Optional[str] = None

    # YouTube Data API
    GOOGLE_CLIENT_ID: Optional[str] = None
    GOOGLE_CLIENT_SECRET: Optional[str] = None
    GOOGLE_REDIRECT_URI: str = "http://localhost:8000/api/v1/youtube/callback"
    YOUTUBE_UPLOAD_ENABLED: bool = False
    DEFAULT_VIDEO_PRIVACY: str = "private"
    AUTO_PUBLISH_ENABLED: bool = False

    # Automation Scheduler
    AUTOMATION_ENABLED: bool = False
    DAILY_GENERATION_TIME: str = "07:00"
    DAILY_PUBLISHING_TIME: str = "09:00"
    DEFAULT_TIMEZONE: str = "UTC"
    MAX_DAILY_GENERATIONS: int = 5
    MAX_RETRY_ATTEMPTS: int = 3

settings = Settings()

os.makedirs(settings.STORAGE_DIR, exist_ok=True)
os.makedirs(settings.VIDEO_STORAGE_DIR, exist_ok=True)
os.makedirs(settings.TEMP_STORAGE_DIR, exist_ok=True)
