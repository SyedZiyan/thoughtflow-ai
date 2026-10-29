import argparse
import asyncio
import json
import logging
import signal
import sys
import time
from datetime import datetime, timezone, date
from pathlib import Path
from zoneinfo import ZoneInfo

# Add backend directory to path
backend_path = Path(__file__).resolve().parent.parent / "backend"
sys.path.insert(0, str(backend_path))

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger
from app.db.session import SessionLocal
from app.models.models import User, UserSettings, Job
from app.services.pipeline_service import PipelineService
from app.core.config import settings

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)s] [ThoughtFlow-Worker] %(message)s"
)
logger = logging.getLogger("thoughtflow.worker")

STORAGE_PATH = Path(settings.STORAGE_DIR)
HEARTBEAT_FILE = STORAGE_PATH / "worker_heartbeat.json"

def write_heartbeat(status: str = "healthy", last_action: str = "cycle_completed"):
    """Writes heartbeat file for API healthcheck and container liveness probes"""
    try:
        STORAGE_PATH.mkdir(parents=True, exist_ok=True)
        data = {
            "status": status,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "pid": sys.platform,
            "last_action": last_action
        }
        with open(HEARTBEAT_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
    except Exception as e:
        logger.warning(f"Could not write worker heartbeat file: {e}")

def is_time_to_generate(user_settings: UserSettings) -> bool:
    """
    Checks if the current moment matches the user's configured generation time in their specific timezone.
    Matches within a 15-minute tolerance window.
    """
    try:
        user_tz_name = user_settings.timezone or "UTC"
        user_tz = ZoneInfo(user_tz_name)
    except Exception:
        user_tz = timezone.utc

    now_user_time = datetime.now(user_tz)
    gen_time_str = user_settings.generation_time or "07:00"

    try:
        target_hour, target_min = map(int, gen_time_str.split(":"))
    except ValueError:
        target_hour, target_min = 7, 0

    # Match if current hour == target_hour and minute is within ±15 min window
    if now_user_time.hour == target_hour and abs(now_user_time.minute - target_min) <= 15:
        return True
    return False

def is_time_to_publish(user_settings: UserSettings) -> bool:
    """
    Checks if the current moment matches the user's configured publishing time in their specific timezone.
    Matches within a 15-minute tolerance window.
    """
    try:
        user_tz_name = user_settings.timezone or "UTC"
        user_tz = ZoneInfo(user_tz_name)
    except Exception:
        user_tz = timezone.utc

    now_user_time = datetime.now(user_tz)
    pub_time_str = user_settings.publishing_time or "09:00"

    try:
        target_hour, target_min = map(int, pub_time_str.split(":"))
    except ValueError:
        target_hour, target_min = 9, 0

    if now_user_time.hour == target_hour and abs(now_user_time.minute - target_min) <= 15:
        return True
    return False

def run_worker_cycle(force: bool = False):
    """
    Executes a single pass of the automation worker:
    1. Reclaims any crashed / stalled jobs from previous worker runs.
    2. Iterates over active users and checks timezone generation and publishing schedules.
    3. Runs pipeline and publishing idempotently with database-backed state protection.
    4. Records heartbeat.
    """
    logger.info("Starting automation worker cycle...")
    db = SessionLocal()
    processed_count = 0

    try:
        # Step 1: Reclaim stuck jobs
        reclaimed = PipelineService.reclaim_stuck_jobs(db, max_age_minutes=30)
        if reclaimed > 0:
            logger.info(f"Reclaimed {reclaimed} stalled jobs.")

        # Step 2: Iterate over users
        users = db.query(User).all()
        for user in users:
            user_settings = user.settings
            if not user_settings or not user_settings.automation_enabled:
                logger.info(f"Automation disabled for {user.email}. Skipping.")
                continue

            # Determine user's local date for idempotency key
            try:
                user_tz = ZoneInfo(user_settings.timezone or "UTC")
            except Exception:
                user_tz = timezone.utc

            user_now = datetime.now(user_tz)
            user_date_str = user_now.date().isoformat()

            time_for_gen = force or is_time_to_generate(user_settings)
            time_for_pub = force or is_time_to_publish(user_settings)

            if not time_for_gen and not time_for_pub:
                logger.info(
                    f"User {user.email}: Local time {user_now.strftime('%H:%M %Z')} matches neither "
                    f"generation ({user_settings.generation_time}) nor publishing ({user_settings.publishing_time}). Skipping."
                )
                continue

            # Publishing phase: if scheduled for publishing, publish any rendered unuploaded videos for today
            if time_for_pub and user_settings.auto_publish_enabled:
                from app.models.models import Video, Thought, Integration
                from app.services.youtube_service import youtube_service
                pending_videos = (
                    db.query(Video)
                    .join(Thought, Video.thought_id == Thought.id)
                    .filter(
                        Thought.user_id == user.id,
                        Video.render_status == "completed",
                        Video.upload_status == "not_uploaded"
                    )
                    .all()
                )
                for v in pending_videos:
                    logger.info(f"Publishing pending video '{v.title}' for {user.email}...")
                    yt_integration = (
                        db.query(Integration)
                        .filter(Integration.user_id == user.id, Integration.provider == "youtube")
                        .first()
                    )
                    creds_str = yt_integration.encrypted_credentials if yt_integration else None
                    video_file = Path(settings.VIDEO_STORAGE_DIR) / v.storage_key
                    if video_file.exists():
                        res = youtube_service.upload_video(
                            video_path=str(video_file),
                            title=v.title,
                            description=v.description,
                            tags=v.thought.hashtags if v.thought else ["shorts"],
                            privacy_status="private",
                            encrypted_credentials_str=creds_str
                        )
                        if not res.get("dry_run"):
                            v.upload_status = "published"
                            v.youtube_video_id = res.get("youtube_video_id")
                            v.youtube_url = res.get("youtube_url")
                            v.published_at = datetime.now(timezone.utc)
                            db.commit()

            # Generation phase: if scheduled for generation, run daily pipeline
            if time_for_gen:
                job_key = f"daily_pipeline_{user.id}_{user_date_str}"
                logger.info(f"Executing daily automation pipeline for {user.email} (key: {job_key})...")

                try:
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    # Skip upload during generation if publishing time is configured for a later hour
                    is_same_hour = user_settings.generation_time == user_settings.publishing_time
                    res = loop.run_until_complete(
                        PipelineService.execute_pipeline(
                            db=db,
                            user=user,
                            job_key=job_key,
                            skip_upload=not (user_settings.auto_publish_enabled and (force or is_same_hour))
                        )
                    )
                    loop.close()
                    processed_count += 1
                    logger.info(f"Successfully processed pipeline for {user.email}: {res.get('message', 'Completed')}")
                except Exception as pipe_err:
                    logger.error(f"Pipeline error for {user.email}: {pipe_err}")

        write_heartbeat(status="healthy", last_action=f"processed_{processed_count}_users")
        logger.info(f"Automation worker cycle finished. Processed {processed_count} scheduled pipelines.")
    except Exception as e:
        logger.exception(f"Unexpected error during worker cycle: {e}")
        write_heartbeat(status="degraded", last_action=f"error_{str(e)[:50]}")
    finally:
        db.close()

def start_worker():
    parser = argparse.ArgumentParser(description="ThoughtFlow AI Automation Worker Daemon")
    parser.add_argument("--run-once", action="store_true", help="Execute a single pass and exit (for cron or testing)")
    parser.add_argument("--force", action="store_true", help="Force execute pipeline ignoring time schedule check")
    args = parser.parse_args()

    if args.run_once:
        logger.info("Running single worker cycle (--run-once)...")
        run_worker_cycle(force=args.force)
        logger.info("Single worker cycle complete.")
        return

    logger.info("Starting ThoughtFlow AI Standalone Background Worker Daemon...")
    scheduler = BlockingScheduler()

    # Run every 10 minutes to check time windows
    scheduler.add_job(
        run_worker_cycle,
        trigger=CronTrigger(minute="*/10"),
        id="thoughtflow_daily_cycle",
        name="ThoughtFlow Daily Generation Cycle",
        replace_existing=True
    )

    def shutdown_handler(signum, frame):
        logger.info("Received termination signal. Shutting down worker gracefully...")
        write_heartbeat(status="stopped", last_action="clean_shutdown")
        scheduler.shutdown(wait=False)
        sys.exit(0)

    signal.signal(signal.SIGINT, shutdown_handler)
    signal.signal(signal.SIGTERM, shutdown_handler)

    write_heartbeat(status="running", last_action="daemon_started")
    logger.info("Worker scheduler initialized. Listening for jobs...")

    try:
        # Run one immediate check on startup
        run_worker_cycle(force=False)
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("Worker stopped.")

if __name__ == "__main__":
    start_worker()
