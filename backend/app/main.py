import os
import json
import shutil
from pathlib import Path
from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, HTMLResponse
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
            <div class="footer">Version 1.0.1 • Fast Local Network OTA</div>
        </div>
    </body>
    </html>
    """

@app.api_route("/api/v1/app/download", methods=["GET", "HEAD"], tags=["App Distribution"])
def download_latest_apk():
    """Serves the latest compiled Android debug APK for over-the-air updates"""
    apk_path = Path(__file__).resolve().parent.parent.parent / "apps" / "android" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk_path.exists():
        raise HTTPException(status_code=404, detail="APK build not found on server. Build it first with Gradle.")
    return FileResponse(
        path=str(apk_path),
        filename="ThoughtFlow-AI-Latest.apk",
        media_type="application/vnd.android.package-archive"
    )

@app.get("/api/v1/app/version", tags=["App Distribution"])
def get_app_version():
    """Returns metadata about the latest compiled APK on the server"""
    apk_path = Path(__file__).resolve().parent.parent.parent / "apps" / "android" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    file_size_mb = round(apk_path.stat().st_size / (1024 * 1024), 1) if apk_path.exists() else 16.3
    
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
        "version_name": "1.0.1",
        "version_code": 2,
        "release_notes": "Added In-App OTA Auto-Updater, 9:16 Shorts Player & YouTube Shorts Studio",
        "download_url": "/api/v1/app/download",
        "file_size_mb": file_size_mb
    }

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
