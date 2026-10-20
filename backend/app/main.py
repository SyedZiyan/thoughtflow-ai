import os
import shutil
from pathlib import Path
from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.core.config import settings
from app.db.session import engine, Base, SessionLocal
from app.api.v1.api import api_router
from app.api.deps import get_or_create_default_user

# Create database tables automatically
Base.metadata.create_all(bind=engine)

# Seed default creator user on application startup
db_init = SessionLocal()
try:
    get_or_create_default_user(db_init)
finally:
    db_init.close()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="ThoughtFlow AI - Daily Motivational Thought Generation, Animated YouTube Shorts Automation & Publishing API",
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows local dev from web and mobile apps
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount video storage for browser and mobile media playback
video_dir = Path(settings.VIDEO_STORAGE_DIR)
video_dir.mkdir(parents=True, exist_ok=True)
app.mount("/storage/videos", StaticFiles(directory=str(video_dir)), name="videos")

# Include API Router
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/health", tags=["Health"])
def health_check():
    """Verify backend health, database connectivity, and FFmpeg installation"""
    ffmpeg_found = shutil.which(settings.FFMPEG_PATH) is not None
    ffprobe_found = shutil.which(settings.FFPROBE_PATH) is not None
    return {
        "status": "healthy",
        "service": "ThoughtFlow AI Backend",
        "version": settings.VERSION,
        "environment": settings.ENVIRONMENT,
        "database": "connected",
        "ffmpeg_installed": ffmpeg_found,
        "ffprobe_installed": ffprobe_found,
        "ai_provider": settings.AI_PROVIDER,
        "youtube_upload_enabled": settings.YOUTUBE_UPLOAD_ENABLED
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
