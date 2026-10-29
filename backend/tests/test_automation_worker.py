import asyncio
import os
import json
import pytest
from datetime import datetime, timezone, timedelta
from fastapi.testclient import TestClient
from app.main import app
from app.db.session import SessionLocal
from app.models.models import User, Thought, Video, Job, UserSettings, ActivityLog
from app.api.deps import get_or_create_default_user
from app.services.pipeline_service import PipelineService, PipelineExecutionError
from worker.scheduler import is_time_to_generate, is_time_to_publish, write_heartbeat, HEARTBEAT_FILE
from app.api.v1.endpoints.automation import get_worker_heartbeat

client = TestClient(app)

# ==========================================
# 1. IDEMPOTENCY & DUPLICATE PREVENTION
# ==========================================

@pytest.mark.asyncio
async def test_job_idempotency():
    db = SessionLocal()
    original_limit = 5
    job_to_clean = None
    try:
        user = get_or_create_default_user(db)
        original_limit = user.settings.daily_limit
        user.settings.daily_limit = 50
        db.commit()
        test_job_key = f"test_idem_key_{int(datetime.now(timezone.utc).timestamp())}"

        # First run: Executes pipeline
        res1 = await PipelineService.execute_pipeline(
            db=db,
            user=user,
            job_key=test_job_key,
            manual_theme="discipline",
            skip_upload=True
        )
        assert res1["success"] is True
        assert "thought" in res1
        assert "video" in res1
        job_to_clean = test_job_key

        # Second run with exact same job key: must short-circuit idempotently
        res2 = await PipelineService.execute_pipeline(
            db=db,
            user=user,
            job_key=test_job_key,
            manual_theme="discipline",
            skip_upload=True
        )
        assert "already completed" in res2["message"]
    finally:
        user = get_or_create_default_user(db)
        user.settings.daily_limit = original_limit
        if job_to_clean:
            db.query(Job).filter(Job.job_key == job_to_clean).delete()
            db.query(Thought).filter(Thought.text.like("%Discipline%")).delete()
        db.commit()
        db.close()


# ==========================================
# 2. DAILY BUDGET LIMIT ENFORCEMENT
# ==========================================

@pytest.mark.asyncio
async def test_daily_budget_limit_enforcement():
    db = SessionLocal()
    t = None
    try:
        user = get_or_create_default_user(db)
        user.settings.daily_limit = 1
        db.commit()

        # Seed one thought created today
        t = Thought(
            user_id=user.id,
            text="Existing thought created today for testing daily budget limit.",
            theme="discipline",
            language="en",
            title="Budget Title #Shorts",
            description="Testing budget desc.",
            hashtags=["shorts"],
            content_hash="dummy_hash_for_budget_test_12345",
            status="approved",
            created_at=datetime.now(timezone.utc)
        )
        db.add(t)
        db.commit()

        # Next pipeline run must be rejected at limits stage
        job_key = f"test_budget_limit_{int(datetime.now(timezone.utc).timestamp())}"
        with pytest.raises(PipelineExecutionError) as exc_info:
            await PipelineService.execute_pipeline(
                db=db,
                user=user,
                job_key=job_key,
                manual_theme="motivation"
            )
        assert exc_info.value.stage == "limits"
        assert "Daily generation limit" in exc_info.value.message
    finally:
        user.settings.daily_limit = 5
        if t and t.id:
            db.query(Thought).filter(Thought.id == t.id).delete()
        db.commit()
        db.close()


# ==========================================
# 3. TIMEZONE GENERATION SCHEDULE
# ==========================================

def test_timezone_generation_schedule_check():
    settings = UserSettings(
        user_id="test_tz_user",
        timezone="UTC",
        generation_time=datetime.now(timezone.utc).strftime("%H:%M")
    )
    # Target time matches current UTC time -> should be True
    assert is_time_to_generate(settings) is True

    # Far future target time -> should be False
    settings.generation_time = "23:59"
    now_utc = datetime.now(timezone.utc)
    if now_utc.hour != 23:
        assert is_time_to_generate(settings) is False

def test_timezone_publishing_schedule_check():
    settings = UserSettings(
        user_id="test_tz_user",
        timezone="UTC",
        publishing_time=datetime.now(timezone.utc).strftime("%H:%M")
    )
    assert is_time_to_publish(settings) is True


# ==========================================
# 4. CRASH RECOVERY & STUCK JOB RECLAMATION
# ==========================================

def test_stuck_job_reclamation():
    db = SessionLocal()
    stuck_job = None
    try:
        user = get_or_create_default_user(db)
        stuck_job_key = f"stuck_job_crash_test_{int(datetime.now(timezone.utc).timestamp())}"

        # Create a simulated crashed job in 'rendering' status started 45 minutes ago
        stuck_job = Job(
            job_key=stuck_job_key,
            job_type="daily_pipeline",
            status="rendering",
            current_stage="video_rendering",
            attempts=1,
            max_attempts=3,
            started_at=datetime.now(timezone.utc) - timedelta(minutes=45)
        )
        db.add(stuck_job)
        db.commit()
        db.refresh(stuck_job)

        # Run recovery
        reclaimed = PipelineService.reclaim_stuck_jobs(db, max_age_minutes=30)
        assert reclaimed >= 1

        db.refresh(stuck_job)
        # Reclaimed job should be reset to 'pending' ready for retry
        assert stuck_job.status == "pending"
        assert "Reclaimed after worker stall" in stuck_job.error_message
    finally:
        if stuck_job:
            db.delete(stuck_job)
            db.commit()
        db.close()


# ==========================================
# 5. WORKER HEARTBEAT LOGIC
# ==========================================

def test_worker_heartbeat_generation():
    write_heartbeat(status="healthy", last_action="test_cycle_complete")
    assert HEARTBEAT_FILE.exists()

    hb = get_worker_heartbeat()
    assert hb["worker_running"] is True
    assert hb["status"] == "healthy"
    assert hb["last_action"] == "test_cycle_complete"


# ==========================================
# 6. AUTOMATION API ENDPOINTS
# ==========================================

def test_api_automation_status():
    resp = client.get("/api/v1/automation/status")
    assert resp.status_code == 200
    data = resp.json()
    assert "automation_enabled" in data
    assert "generation_time" in data
    assert "timezone" in data
    assert "worker" in data

def test_api_list_jobs():
    resp = client.get("/api/v1/automation/jobs")
    assert resp.status_code == 200
    data = resp.json()
    assert isinstance(data, list)

def test_api_retry_non_existent_job():
    resp = client.post("/api/v1/automation/jobs/non_existent_job_id_123/retry")
    assert resp.status_code == 404
