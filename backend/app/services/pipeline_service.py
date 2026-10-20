import logging
from datetime import datetime, timezone, date
from typing import Dict, Any, Optional
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.models.models import User, Thought, Video, Job, UserSettings, Integration, ActivityLog
from app.services.ai_provider import get_ai_provider, compute_content_hash, check_near_duplicate
from app.services.video_renderer import video_renderer
from app.services.youtube_service import youtube_service

logger = logging.getLogger("thoughtflow.pipeline")

class PipelineExecutionError(Exception):
    def __init__(self, stage: str, message: str, original_error: Optional[Exception] = None):
        self.stage = stage
        self.message = message
        self.original_error = original_error
        super().__init__(f"[{stage}] {message}")

class PipelineService:
    @staticmethod
    def get_or_create_job(db: Session, job_key: str, job_type: str = "daily_pipeline") -> Job:
        """Atomically find or initialize job record with idempotency protection"""
        job = db.query(Job).filter(Job.job_key == job_key).first()
        if not job:
            job = Job(
                job_key=job_key,
                job_type=job_type,
                status="pending",
                current_stage="initialized",
                attempts=0,
                max_attempts=3
            )
            db.add(job)
            db.commit()
            db.refresh(job)
        return job

    @staticmethod
    def select_theme_rotation(user_settings: UserSettings, db: Session, user_id: str) -> str:
        """Select theme by rotating through selected themes, prioritizing least recently used"""
        themes = user_settings.selected_themes or ["motivation", "discipline", "consistency", "resilience"]
        if not themes:
            return "motivation"

        # Find recent thought themes
        recent_thoughts = (
            db.query(Thought.theme)
            .filter(Thought.user_id == user_id)
            .order_by(Thought.created_at.desc())
            .limit(len(themes))
            .all()
        )
        recent_list = [r[0].lower() for r in recent_thoughts]

        for theme in themes:
            if theme.lower() not in recent_list:
                return theme.lower()
        return themes[0].lower()

    @classmethod
    async def execute_pipeline(
        cls,
        db: Session,
        user: User,
        job_key: Optional[str] = None,
        manual_theme: Optional[str] = None,
        manual_prompt: Optional[str] = None,
        skip_upload: bool = False
    ) -> Dict[str, Any]:
        """
        Executes the complete end-to-end pipeline:
        1. Claim job
        2. Check daily limits
        3. Select theme
        4. Generate AI thought
        5. Duplicate check
        6. Persist thought
        7. Render animated video
        8. Validate video
        9. YouTube upload / dry run
        10. Update job and log activity
        """
        today_str = date.today().isoformat()
        if not job_key:
            job_key = f"pipeline_{user.id}_{today_str}_{int(datetime.now(timezone.utc).timestamp())}"

        job = cls.get_or_create_job(db, job_key=job_key)

        # Check if already completed
        if job.status in ("published", "completed"):
            return {
                "job_id": job.id,
                "status": job.status,
                "message": "Job already completed successfully for this cycle (idempotent)."
            }

        job.status = "generating"
        job.current_stage = "starting"
        job.attempts += 1
        job.started_at = datetime.now(timezone.utc)
        db.commit()

        user_settings = user.settings
        if not user_settings:
            from app.models.models import UserSettings
            user_settings = UserSettings(user_id=user.id)
            db.add(user_settings)
            db.commit()
            db.refresh(user_settings)

        try:
            # Stage 1: Daily limit check
            job.current_stage = "checking_limits"
            start_of_day = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
            today_generations = (
                db.query(func.count(Thought.id))
                .filter(Thought.user_id == user.id, Thought.created_at >= start_of_day)
                .scalar() or 0
            )

            if today_generations >= user_settings.daily_limit:
                raise PipelineExecutionError("limits", f"Daily generation limit of {user_settings.daily_limit} reached.")

            # Stage 2 to 5: AI Thought generation, duplicate check & persistence
            job.current_stage = "ai_generation"
            from app.services.thought_service import ThoughtService
            thought = await ThoughtService.generate_and_save_thought(
                db=db,
                user=user,
                theme=manual_theme,
                language=user_settings.language,
                custom_prompt=manual_prompt or "",
                max_attempts=3,
                status="approved"
            )
            selected_theme = thought.theme

            # Stage 6: Video rendering
            job.status = "rendering"
            job.current_stage = "video_rendering"
            db.commit()

            render_res = video_renderer.render_animated_video(
                thought_id=thought.id,
                thought_text=thought.text,
                theme=thought.theme,
                visual_theme=user_settings.visual_theme,
                animation_style=user_settings.animation_style,
                duration_seconds=user_settings.video_duration
            )

            # Stage 7: Persist Video record
            job.status = "validating"
            job.current_stage = "video_validation"
            video = Video(
                thought_id=thought.id,
                storage_key=render_res["storage_key"],
                title=thought.title,
                description=thought.description,
                duration_seconds=render_res["duration_seconds"],
                width=render_res["width"],
                height=render_res["height"],
                render_status="completed",
                upload_status="not_uploaded"
            )
            db.add(video)
            thought.status = "rendered"
            db.commit()
            db.refresh(video)

            # Stage 8: YouTube upload / dry-run
            upload_res = {"dry_run": True, "status": "dry_run_validated"}
            if not skip_upload:
                job.status = "uploading"
                job.current_stage = "youtube_upload"
                db.commit()

                yt_integration = (
                    db.query(Integration)
                    .filter(Integration.user_id == user.id, Integration.provider == "youtube")
                    .first()
                )
                creds_str = yt_integration.encrypted_credentials if yt_integration else None

                upload_res = youtube_service.upload_video(
                    video_path=render_res["absolute_path"],
                    title=video.title,
                    description=video.description,
                    tags=thought.hashtags,
                    privacy_status="private",
                    encrypted_credentials_str=creds_str
                )

                if upload_res.get("dry_run"):
                    video.upload_status = "not_uploaded"
                    video.youtube_url = upload_res.get("youtube_url")
                else:
                    video.upload_status = "published"
                    video.youtube_video_id = upload_res.get("youtube_video_id")
                    video.youtube_url = upload_res.get("youtube_url")
                    video.published_at = datetime.now(timezone.utc)
                db.commit()

            # Stage 9: Finalize Job and Activity Log
            job.status = "published" if not upload_res.get("dry_run") else "completed"
            job.current_stage = "completed"
            job.finished_at = datetime.now(timezone.utc)
            job.job_metadata = {
                "thought_id": thought.id,
                "video_id": video.id,
                "theme": selected_theme,
                "dry_run": upload_res.get("dry_run", False),
                "youtube_url": video.youtube_url
            }

            # Create activity log
            activity = ActivityLog(
                user_id=user.id,
                job_id=job.id,
                event_type="video_rendered",
                message=f"Generated and rendered video for theme '{selected_theme}': \"{thought.text[:60]}...\"",
                log_metadata=job.job_metadata
            )
            db.add(activity)
            db.commit()

            return {
                "success": True,
                "job_id": job.id,
                "thought": {
                    "id": thought.id,
                    "text": thought.text,
                    "theme": thought.theme,
                    "title": thought.title
                },
                "video": {
                    "id": video.id,
                    "storage_key": video.storage_key,
                    "duration": video.duration_seconds,
                    "upload_status": video.upload_status,
                    "youtube_url": video.youtube_url
                },
                "dry_run": upload_res.get("dry_run", True)
            }

        except Exception as e:
            db.rollback()
            stage = getattr(e, "stage", job.current_stage)
            error_msg = str(e)
            logger.exception(f"Pipeline error at stage {stage}: {error_msg}")

            job.status = "failed"
            job.current_stage = stage
            job.error_message = error_msg[:1000]
            job.finished_at = datetime.now(timezone.utc)

            # Log activity failure
            activity = ActivityLog(
                user_id=user.id,
                job_id=job.id,
                event_type="pipeline_error",
                message=f"Pipeline failed at stage '{stage}': {error_msg[:200]}",
                log_metadata={"stage": stage, "error": error_msg}
            )
            db.add(activity)
            db.commit()

            raise
