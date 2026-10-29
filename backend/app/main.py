import os
import json
import logging
import shutil
from pathlib import Path
from contextlib import asynccontextmanager
from typing import Optional
from fastapi import FastAPI, Depends, HTTPException, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, HTMLResponse
from sqlalchemy.orm import Session
from sqlalchemy import text
from app.core.config import settings
from app.db.session import engine, Base, SessionLocal, get_db
from app.api.v1.api import api_router

logger = logging.getLogger("thoughtflow.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Safely manage application startup and shutdown lifecycle"""
    try:
        # Create database tables automatically
        Base.metadata.create_all(bind=engine)
        # Perform idempotent schema alignment for newly added columns
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
        logger.info("Database schema verified and aligned.")
    except Exception as e:
        logger.error(f"Startup database initialization error: {e}")
        if settings.ENVIRONMENT == "production":
            raise RuntimeError(f"Critical database initialization failure in production: {e}") from e
    yield

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="ThoughtFlow AI - Daily Motivational Thought Generation, Animated YouTube Shorts Automation & Publishing API",
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    lifespan=lifespan
)

# Tightened CORS Middleware with explicit trusted origins
cors_origins = [str(origin).strip() for origin in settings.CORS_ORIGINS if origin != "*"]
if not cors_origins and settings.ENVIRONMENT == "development":
    cors_origins = ["http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_origin_regex=r"^https?://(localhost|127\.0\.0\.1|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+)(:\d+)?$" if settings.ENVIRONMENT == "development" else None,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"],
)

# Standard Security Headers Middleware
@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    return response

# Mount video storage for browser and mobile media playback
video_dir = Path(settings.VIDEO_STORAGE_DIR)
video_dir.mkdir(parents=True, exist_ok=True)
app.mount("/storage/videos", StaticFiles(directory=str(video_dir)), name="videos")

# Include API Router
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/download", response_class=HTMLResponse, tags=["App Distribution"])
def download_page():
    """Beautiful web landing page to download the latest Android APK directly on mobile devices"""
    apk_path = Path(__file__).resolve().parent.parent.parent / "apps" / "android" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    size_mb = f"{apk_path.stat().st_size / (1024 * 1024):.1f} MB" if apk_path.exists() else "17.1 MB"
    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Download ThoughtFlow AI</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {{ margin: 0; padding: 0; box-sizing: border-box; }}
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 100%);
                color: #FFFFFF;
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                padding: 24px;
            }}
            .card {{
                background: rgba(30, 41, 59, 0.9);
                backdrop-filter: blur(20px);
                border: 1px solid rgba(255, 255, 255, 0.12);
                border-radius: 28px;
                padding: 40px 28px;
                text-align: center;
                max-width: 420px;
                width: 100%;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            }}
            .logo {{
                width: 72px;
                height: 72px;
                background: linear-gradient(135deg, #06B6D4 0%, #10B981 100%);
                border-radius: 22px;
                margin: 0 auto 20px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 34px;
                box-shadow: 0 10px 25px rgba(6, 182, 212, 0.35);
            }}
            h1 {{ font-size: 26px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; }}
            .subtitle {{ color: #10B981; font-weight: 600; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; }}
            p {{ color: #94A3B8; font-size: 14px; line-height: 1.6; margin-bottom: 28px; }}
            .btn {{
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 10px;
                background: linear-gradient(135deg, #10B981 0%, #059669 100%);
                color: #FFFFFF;
                text-decoration: none;
                padding: 18px 24px;
                border-radius: 20px;
                font-weight: 700;
                font-size: 16px;
                box-shadow: 0 8px 20px rgba(16, 185, 129, 0.35);
                transition: transform 0.2s, box-shadow 0.2s;
            }}
            .btn:active {{ transform: scale(0.98); }}
            .steps {{
                margin-top: 28px;
                text-align: left;
                background: rgba(15, 23, 42, 0.6);
                padding: 16px;
                border-radius: 16px;
                border: 1px solid rgba(255, 255, 255, 0.05);
            }}
            .steps h3 {{ font-size: 12px; color: #94A3B8; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 0.5px; }}
            .steps ol {{ padding-left: 20px; font-size: 13px; color: #CBD5E1; line-height: 1.6; }}
            .footer {{ margin-top: 20px; font-size: 12px; color: #64748B; }}
        </style>
    </head>
    <body>
        <div class="card">
            <div class="logo">⚡</div>
            <h1>ThoughtFlow AI</h1>
            <div class="subtitle">Android App Updates</div>
            <p>Download the latest build directly to your phone. Updates install in-place and keep all your saved thoughts and settings intact.</p>
            <a href="/api/v1/app/download" class="btn">
                <span>⬇ Download Latest APK</span>
                <span style="opacity: 0.8; font-size: 13px;">({size_mb})</span>
            </a>
            <div class="steps">
                <h3>Installation Steps:</h3>
                <ol>
                    <li>Tap <b>Download</b> above.</li>
                    <li>Open notification or Downloads folder.</li>
                    <li>Tap <b>Update</b> when prompted by Android.</li>
                </ol>
            </div>
            <div class="footer">Version 1.3.7 • Direct In-App APK Download</div>
        </div>
    </body>
    </html>
    """

def resolve_latest_apk_path() -> Optional[Path]:
    """
    Resolves the latest compiled APK across multiple production & development locations:
    1. Explicit environment variable APK_FILE_PATH
    2. Project root ThoughtFlow-AI-Latest.apk
    3. Storage directory ThoughtFlow-AI-Latest.apk
    4. Local backend directory ThoughtFlow-AI-Latest.apk
    5. Local Android build output directory
    """
    custom_env = os.environ.get("APK_FILE_PATH")
    candidates = [
        Path(custom_env) if custom_env else None,
        Path(__file__).resolve().parent.parent.parent / "ThoughtFlow-AI-Latest.apk",
        Path(__file__).resolve().parent.parent / "ThoughtFlow-AI-Latest.apk",
        Path(settings.STORAGE_DIR) / "ThoughtFlow-AI-Latest.apk",
        Path(__file__).resolve().parent.parent.parent / "apps" / "android" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk",
    ]
    for p in candidates:
        if p and p.exists() and p.is_file() and p.stat().st_size > 1000000:
            return p
    return None

@app.api_route("/api/v1/app/download", methods=["GET", "HEAD"], tags=["App Distribution"])
def download_latest_apk():
    """Serves the latest compiled Android debug APK for over-the-air updates"""
    apk_path = resolve_latest_apk_path()
    if not apk_path:
        raise HTTPException(
            status_code=404,
            detail="APK build not found on server. Ensure ThoughtFlow-AI-Latest.apk is present or configure APK_FILE_PATH."
        )
    return FileResponse(
        path=str(apk_path),
        filename="ThoughtFlow-AI-Latest.apk",
        media_type="application/vnd.android.package-archive"
    )

@app.get("/api/v1/app/version", tags=["App Distribution"])
def get_app_version():
    """Returns metadata about the latest compiled APK on the server"""
    apk_path = resolve_latest_apk_path()
    file_size_mb = round(apk_path.stat().st_size / (1024 * 1024), 1) if apk_path else 25.4
    
    version_file = Path(__file__).resolve().parent / "version_info.json"
    if version_file.exists():
        try:
            with open(version_file, "r", encoding="utf-8-sig") as f:
                data = json.load(f)
                data["file_size_mb"] = file_size_mb
                return data
        except Exception:
            pass

    return {
        "version_name": "1.3.7",
        "version_code": 20,
        "release_notes": "Lock backend server URL to prevent accidental typos and update check failures (v1.3.7)",
        "download_url": "/api/v1/app/download",
        "file_size_mb": file_size_mb
    }

@app.get("/health", tags=["Health"])
def health_check(response: Response, db: Session = Depends(get_db)):
    """Truthful health check verifying database connectivity, storage writability, and FFmpeg"""
    db_healthy = False
    try:
        db.execute(text("SELECT 1"))
        db_healthy = True
    except Exception as e:
        logger.error(f"Health check DB probe failed: {e}")

    ffmpeg_found = shutil.which(settings.FFMPEG_PATH) is not None
    ffprobe_found = shutil.which(settings.FFPROBE_PATH) is not None

    storage_writable = False
    try:
        probe_path = Path(settings.STORAGE_DIR)
        probe_path.mkdir(parents=True, exist_ok=True)
        probe_file = probe_path / ".health_check_probe"
        probe_file.write_text("ok")
        probe_file.unlink()
        storage_writable = True
    except Exception as e:
        logger.warning(f"Storage writability probe failed: {e}")

    is_healthy = db_healthy and storage_writable
    if not is_healthy:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE

    return {
        "status": "healthy" if is_healthy else "degraded",
        "service": "ThoughtFlow AI Backend",
        "version": settings.VERSION,
        "environment": settings.ENVIRONMENT,
        "database": "connected" if db_healthy else "disconnected",
        "storage_writable": storage_writable,
        "ffmpeg_installed": ffmpeg_found,
        "ffprobe_installed": ffprobe_found,
        "ai_provider": settings.AI_PROVIDER,
        "youtube_upload_enabled": settings.YOUTUBE_UPLOAD_ENABLED
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
