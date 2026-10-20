import asyncio
import logging
import signal
import sys
import time
from datetime import datetime, timezone, date
from pathlib import Path

# Add backend directory to path
backend_path = Path(__file__).resolve().parent.parent / "backend"
sys.path.insert(0, str(backend_path))

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger
from app.db.session import SessionLocal
from app.models.models import User, UserSettings
from app.services.pipeline_service import PipelineService

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)s] [ThoughtFlow-Worker] %(message)s"
)
logger = logging.getLogger("thoughtflow.worker")

def run_daily_pipeline_job():
    """Triggered by scheduler to execute the automated daily thought and video generation"""
    logger.info("Executing scheduled daily pipeline check...")
    db = SessionLocal()
    try:
        users = db.query(User).all()
        for user in users:
            settings = user.settings
            if not settings or not settings.automation_enabled:
                logger.info(f"Automation is disabled for user {user.email}. Skipping.")
                continue

            today_str = date.today().isoformat()
            job_key = f"daily_pipeline_{user.id}_{today_str}"
            logger.info(f"Running automated pipeline for {user.email} (key: {job_key})...")

            # Run async pipeline synchronously in worker thread
            try:
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                result = loop.run_until_complete(
                    PipelineService.execute_pipeline(
                        db=db,
                        user=user,
                        job_key=job_key,
                        skip_upload=not settings.auto_publish_enabled
                    )
                )
                loop.close()
                logger.info(f"Scheduled pipeline completed successfully for {user.email}: {result}")
            except Exception as e:
                logger.error(f"Scheduled pipeline failed for {user.email}: {e}")
    finally:
        db.close()

def start_worker():
    logger.info("Starting ThoughtFlow AI Standalone Automation Worker...")
    scheduler = BlockingScheduler()

    # Run every minute check for configured execution window, or set standard daily cron
    scheduler.add_job(
        run_daily_pipeline_job,
        trigger=CronTrigger(minute="*/15"),  # Regular check window
        id="thoughtflow_daily_cycle",
        name="ThoughtFlow Daily Generation Cycle",
        replace_existing=True
    )

    def shutdown_handler(signum, frame):
        logger.info("Worker received shutdown signal. Stopping scheduler cleanly...")
        scheduler.shutdown(wait=False)
        sys.exit(0)

    signal.signal(signal.SIGINT, shutdown_handler)
    signal.signal(signal.SIGTERM, shutdown_handler)

    logger.info("Worker scheduler initialized and listening for jobs.")
    try:
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("Worker stopped.")

if __name__ == "__main__":
    start_worker()
