import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, Optional, List
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError
from app.core.config import settings
from app.core.security import encrypt_token, decrypt_token

SCOPES = [
    "https://www.googleapis.com/auth/youtube.upload",
    "https://www.googleapis.com/auth/youtube.readonly"
]

class YouTubeService:
    def __init__(self):
        self.client_id = settings.GOOGLE_CLIENT_ID
        self.client_secret = settings.GOOGLE_CLIENT_SECRET
        self.redirect_uri = settings.GOOGLE_REDIRECT_URI
        self.upload_enabled = settings.YOUTUBE_UPLOAD_ENABLED

    def get_auth_url(self, state: str = "state_thoughtflow") -> Dict[str, str]:
        """Generate Google OAuth 2.0 authorization URL for YouTube access"""
        if not self.client_id or not self.client_secret:
            return {
                "auth_url": "",
                "configured": False,
                "message": "Google Client ID and Client Secret not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env."
            }

        client_config = {
            "web": {
                "client_id": self.client_id,
                "client_secret": self.client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [self.redirect_uri]
            }
        }

        flow = Flow.from_client_config(
            client_config,
            scopes=SCOPES,
            redirect_uri=self.redirect_uri
        )

        auth_url, _ = flow.authorization_url(
            access_type="offline",
            include_granted_scopes="true",
            prompt="consent",
            state=state
        )

        return {
            "auth_url": auth_url,
            "configured": True,
            "message": "Authorization URL created successfully."
        }

    def exchange_code_for_tokens(self, code: str) -> Dict[str, Any]:
        """Exchange authorization code for access & refresh tokens"""
        if not self.client_id or not self.client_secret:
            raise ValueError("Google OAuth credentials are not configured in settings.")

        client_config = {
            "web": {
                "client_id": self.client_id,
                "client_secret": self.client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [self.redirect_uri]
            }
        }

        flow = Flow.from_client_config(
            client_config,
            scopes=SCOPES,
            redirect_uri=self.redirect_uri
        )

        flow.fetch_token(code=code)
        credentials = flow.credentials

        token_data = {
            "token": credentials.token,
            "refresh_token": credentials.refresh_token,
            "token_uri": credentials.token_uri,
            "client_id": credentials.client_id,
            "client_secret": credentials.client_secret,
            "scopes": credentials.scopes
        }

        return {
            "encrypted_credentials": encrypt_token(json.dumps(token_data)),
            "has_refresh_token": bool(credentials.refresh_token)
        }

    def get_authenticated_service(self, encrypted_credentials_str: str):
        """Construct Google API YouTube client using decrypted credentials and auto-refresh"""
        plain_json = decrypt_token(encrypted_credentials_str)
        if not plain_json:
            raise ValueError("Invalid or unreadable YouTube credentials.")

        token_data = json.loads(plain_json)
        credentials = Credentials(
            token=token_data.get("token"),
            refresh_token=token_data.get("refresh_token"),
            token_uri=token_data.get("token_uri", "https://oauth2.googleapis.com/token"),
            client_id=token_data.get("client_id", self.client_id),
            client_secret=token_data.get("client_secret", self.client_secret),
            scopes=token_data.get("scopes", SCOPES)
        )

        return build("youtube", "v3", credentials=credentials)

    def get_channel_info(self, encrypted_credentials_str: str) -> Dict[str, Any]:
        """Fetch channel metadata, title, and statistics"""
        youtube = self.get_authenticated_service(encrypted_credentials_str)
        response = youtube.channels().list(mine=True, part="snippet,statistics").execute()

        items = response.get("items", [])
        if not items:
            return {"connected": False, "error": "No YouTube channel found for authenticated account."}

        ch = items[0]
        snippet = ch.get("snippet", {})
        stats = ch.get("statistics", {})

        return {
            "connected": True,
            "channel_id": ch.get("id"),
            "channel_title": snippet.get("title"),
            "channel_thumbnail": snippet.get("thumbnails", {}).get("default", {}).get("url"),
            "subscriber_count": int(stats.get("subscriberCount", 0)),
            "video_count": int(stats.get("videoCount", 0)),
            "view_count": int(stats.get("viewCount", 0))
        }

    def upload_video(
        self,
        video_path: str,
        title: str,
        description: str,
        tags: List[str],
        privacy_status: str = "private",
        publish_at: Optional[datetime] = None,
        encrypted_credentials_str: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Uploads video to YouTube via resumable upload.
        If credentials are not present or YOUTUBE_UPLOAD_ENABLED=False, operates in safe dry-run mode.
        """
        path = Path(video_path)
        if not path.exists():
            raise FileNotFoundError(f"Video file not found at: {video_path}")

        # Dry-run check
        if not self.upload_enabled or not encrypted_credentials_str:
            return {
                "dry_run": True,
                "status": "dry_run_validated",
                "youtube_video_id": f"dryrun_{int(datetime.now(timezone.utc).timestamp())}",
                "youtube_url": "https://www.youtube.com/watch?v=DRY_RUN_PREVIEW",
                "message": "Dry-run mode active. Video successfully generated, validated, and ready for live YouTube upload once authorized."
            }

        youtube = self.get_authenticated_service(encrypted_credentials_str)

        body = {
            "snippet": {
                "title": title[:100],
                "description": description[:5000],
                "tags": tags[:20],
                "categoryId": "27"  # Education / How-to / Mindset
            },
            "status": {
                "privacyStatus": privacy_status,
                "selfDeclaredMadeForKids": False
            }
        }

        # If scheduling for future publication
        if publish_at and privacy_status == "private":
            body["status"]["publishAt"] = publish_at.isoformat()

        media = MediaFileUpload(
            str(path),
            mimetype="video/mp4",
            resumable=True,
            chunksize=1024 * 1024 * 4  # 4MB chunks
        )

        request = youtube.videos().insert(
            part=",".join(body.keys()),
            body=body,
            media_body=media
        )

        response = None
        while response is None:
            status, response = request.next_chunk()

        video_id = response.get("id")
        return {
            "dry_run": False,
            "status": "published",
            "youtube_video_id": video_id,
            "youtube_url": f"https://youtu.be/{video_id}",
            "message": "Successfully uploaded to YouTube!"
        }

youtube_service = YouTubeService()
