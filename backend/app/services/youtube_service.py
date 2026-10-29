import json
import os
import time
import socket
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, Optional, List
import httpx
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError
from app.core.config import settings
from app.core.security import encrypt_token, decrypt_token

logger = logging.getLogger("thoughtflow.youtube")

SCOPES = [
    "https://www.googleapis.com/auth/youtube.upload",
    "https://www.googleapis.com/auth/youtube.readonly"
]

# Exponential backoff parameters for resumable upload
RETRIABLE_STATUS_CODES = [500, 502, 503, 504]
MAX_RETRIES = 5

class YouTubeService:
    def __init__(self):
        self.client_id = settings.GOOGLE_CLIENT_ID
        self.client_secret = settings.GOOGLE_CLIENT_SECRET
        self.redirect_uri = settings.GOOGLE_REDIRECT_URI
        self.upload_enabled = settings.YOUTUBE_UPLOAD_ENABLED

    def get_auth_url(self, state: str = "state_thoughtflow") -> Dict[str, Any]:
        """Generate Google OAuth 2.0 authorization URL for YouTube access with offline refresh token"""
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
        """Exchange authorization code for access & refresh tokens and encrypt securely"""
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

    async def revoke_token(self, encrypted_credentials_str: str) -> bool:
        """Safely revokes Google OAuth token with Google revocation endpoint"""
        try:
            plain_json = decrypt_token(encrypted_credentials_str)
            if not plain_json:
                return False
            data = json.loads(plain_json)
            token = data.get("token") or data.get("refresh_token")
            if not token:
                return False

            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.post(
                    "https://oauth2.googleapis.com/revoke",
                    params={"token": token},
                    headers={"content-type": "application/x-www-form-urlencoded"}
                )
                return resp.status_code == 200
        except Exception as e:
            logger.warning(f"Token revocation error (ignoring during disconnect): {e}")
            return False

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

        return build("youtube", "v3", credentials=credentials, cache_discovery=False)

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

    def sync_video_statistics(
        self,
        video_ids: List[str],
        encrypted_credentials_str: str
    ) -> Dict[str, Dict[str, int]]:
        """
        Queries YouTube Data API v3 for live view, like, and comment metrics for specific video IDs.
        Batches up to 50 IDs per API request to preserve quota.
        """
        if not video_ids or not encrypted_credentials_str:
            return {}

        youtube = self.get_authenticated_service(encrypted_credentials_str)
        results: Dict[str, Dict[str, int]] = {}

        # Chunk into 50 IDs per call
        chunk_size = 50
        for i in range(0, len(video_ids), chunk_size):
            chunk = video_ids[i:i + chunk_size]
            try:
                response = youtube.videos().list(
                    part="statistics",
                    id=",".join(chunk)
                ).execute()

                for item in response.get("items", []):
                    vid = item.get("id")
                    st = item.get("statistics", {})
                    results[vid] = {
                        "views": int(st.get("viewCount", 0)),
                        "likes": int(st.get("likeCount", 0)),
                        "comments": int(st.get("commentCount", 0))
                    }
            except Exception as e:
                logger.error(f"Error fetching YouTube video statistics: {e}")

        return results

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
        Uploads video to YouTube via resumable chunked upload with exponential backoff.
        If credentials are not present or YOUTUBE_UPLOAD_ENABLED=False, operates in safe dry-run mode.
        """
        path = Path(video_path)
        if not path.exists():
            raise FileNotFoundError(f"Video file not found at: {video_path}")

        file_size = path.stat().st_size
        if file_size < 10000:
            raise ValueError(f"Video file too small ({file_size} bytes), likely incomplete or corrupt.")

        # Ensure #Shorts tag is present for YouTube algorithm discovery
        cleaned_tags = list(tags) if tags else []
        if "shorts" not in [t.lower() for t in cleaned_tags]:
            cleaned_tags.append("shorts")
        if "#Shorts" not in title and "#shorts" not in title.lower():
            title = f"{title.strip()} #Shorts"

        # Safe dry-run check: When no credentials or upload disabled
        if not self.upload_enabled or not encrypted_credentials_str:
            reason = "YOUTUBE_UPLOAD_ENABLED=False" if not self.upload_enabled else "missing Google OAuth credentials"
            logger.info(f"Operating in dry-run mode for YouTube upload ({reason}). No video was uploaded to YouTube.")
            dry_id = f"dryrun_{int(datetime.now(timezone.utc).timestamp())}"
            return {
                "dry_run": True,
                "status": "dry_run_pending_credentials" if not encrypted_credentials_str else "dry_run_validated",
                "youtube_video_id": dry_id,
                "youtube_url": f"https://www.youtube.com/shorts/{dry_id}",
                "message": f"Dry-run mode active ({reason}). Video rendered and saved locally, but YouTube upload requires Google OAuth credentials (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET) in .env and channel authorization."
            }

        youtube = self.get_authenticated_service(encrypted_credentials_str)

        body = {
            "snippet": {
                "title": title[:100],
                "description": description[:5000],
                "tags": cleaned_tags[:20],
                "categoryId": "27"  # Education / Mindset
            },
            "status": {
                "privacyStatus": privacy_status,
                "selfDeclaredMadeForKids": False
            }
        }

        # Scheduled publication requires privacyStatus="private"
        if publish_at:
            now_utc = datetime.now(timezone.utc)
            if publish_at > now_utc:
                body["status"]["privacyStatus"] = "private"
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

        logger.info(f"Initiating resumable chunked upload to YouTube for '{title}' ({file_size} bytes)")
        response = None
        retry = 0

        while response is None:
            try:
                status, response = request.next_chunk()
                if status:
                    progress = int(status.progress() * 100)
                    logger.info(f"YouTube upload progress: {progress}%")
            except HttpError as e:
                if e.resp.status in RETRIABLE_STATUS_CODES and retry < MAX_RETRIES:
                    retry += 1
                    sleep_sec = 2 ** retry
                    logger.warning(f"Retriable HTTP error {e.resp.status}. Retrying in {sleep_sec}s...")
                    time.sleep(sleep_sec)
                else:
                    if e.resp.status == 403 and "quotaExceeded" in str(e):
                        raise RuntimeError(
                            "YouTube Data API daily quota limit of 10,000 units exceeded. "
                            "Quota automatically resets at midnight Pacific Time (07:00 UTC)."
                        )
                    raise RuntimeError(f"YouTube API upload error: {e}")
            except (socket.error, socket.timeout) as sock_err:
                if retry < MAX_RETRIES:
                    retry += 1
                    sleep_sec = 2 ** retry
                    logger.warning(f"Socket connection error {sock_err}. Retrying in {sleep_sec}s...")
                    time.sleep(sleep_sec)
                else:
                    raise RuntimeError(f"Network failure during YouTube upload: {sock_err}")

        video_id = response.get("id")
        logger.info(f"YouTube upload completed successfully: video ID {video_id}")

        return {
            "dry_run": False,
            "status": "published",
            "youtube_video_id": video_id,
            "youtube_url": f"https://youtu.be/{video_id}",
            "message": "Successfully uploaded to YouTube!"
        }

youtube_service = YouTubeService()
