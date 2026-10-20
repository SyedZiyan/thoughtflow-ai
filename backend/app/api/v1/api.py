from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth,
    dashboard,
    thoughts,
    videos,
    automation,
    settings_ep,
    youtube,
    analytics,
    logs
)

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["Authentication"])
api_router.include_router(dashboard.router, prefix="/dashboard", tags=["Dashboard"])
api_router.include_router(thoughts.router, prefix="/thoughts", tags=["Thoughts"])
api_router.include_router(videos.router, prefix="/videos", tags=["Videos"])
api_router.include_router(automation.router, prefix="/automation", tags=["Automation"])
api_router.include_router(settings_ep.router, prefix="/settings", tags=["Settings"])
api_router.include_router(youtube.router, prefix="/youtube", tags=["YouTube Integration"])
api_router.include_router(analytics.router, prefix="/analytics", tags=["Analytics"])
api_router.include_router(logs.router, prefix="/logs", tags=["Logs"])
