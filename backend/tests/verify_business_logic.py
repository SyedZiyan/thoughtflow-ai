import asyncio
import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from datetime import datetime, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.models.models import Base, User, Thought, Video, Job, Integration, UserSettings
from app.services.ai_provider import (
    MockAIProvider,
    normalize_text,
    compute_content_hash,
    check_near_duplicate,
    StructuredThought
)
from app.services.youtube_service import YouTubeService
from app.schemas.schemas import YouTubeStatusResponse
from app.core.config import settings

class TestBusinessLogic(unittest.TestCase):
    def setUp(self):
        self.engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(bind=self.engine)
        self.Session = sessionmaker(bind=self.engine)
        self.db = self.Session()

    def tearDown(self):
        self.db.close()
        Base.metadata.drop_all(bind=self.engine)

    def test_text_normalization_and_hash(self):
        """Rule 10: Input validation and content hashing"""
        t1 = "Discipline is choosing between what you want now, and what you want most!"
        t2 = "  discipline is CHOOSING between what you want now and what you want most   "
        self.assertEqual(compute_content_hash(t1), compute_content_hash(t2))

    def test_near_duplicate_detection(self):
        """Rule 10 & Critical logic: Prevent near duplicate thoughts"""
        existing = ["Motivation gets you started, but discipline brings you to the finish line."]
        candidate = "Motivation gets you started, discipline brings you to the finish line."
        is_dup, match = check_near_duplicate(candidate, existing, threshold=0.70)
        self.assertTrue(is_dup)
        self.assertIsNotNone(match)

        different = "Silence is not empty; it is full of the answers your mind needs."
        is_dup2, _ = check_near_duplicate(different, existing, threshold=0.70)
        self.assertFalse(is_dup2)

    def test_mock_ai_provider_offline_free(self):
        """Rule 12 & 13: Local free provider operates without API keys"""
        provider = MockAIProvider()
        thought = asyncio.run(provider.generate_thought(theme="discipline"))
        self.assertIsInstance(thought, StructuredThought)
        self.assertEqual(thought.theme, "discipline")
        self.assertIn("#Shorts", thought.title)
        self.assertGreaterEqual(len(thought.thought), 10)
        self.assertEqual(thought.model_name, "mock-provider-v1")

    def test_youtube_safe_dry_run_no_fake_uploads(self):
        """Rule 2, 3 & 14: Never claim upload or fake URLs when credentials missing"""
        yt = YouTubeService()
        yt.upload_enabled = False
        import tempfile, os
        with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as f:
            f.write(b"x" * 20000) # dummy valid size
            tmp_path = f.name
        try:
            res = yt.upload_video(
                video_path=tmp_path,
                title="Test Mindset #Shorts",
                description="Test description",
                tags=["shorts", "motivation"],
                encrypted_credentials_str=None
            )
            self.assertTrue(res["dry_run"])
            self.assertEqual(res["status"], "dry_run_pending_credentials")
            self.assertIsNone(res["youtube_video_id"])
            self.assertIsNone(res["youtube_url"])
            self.assertIn("Dry-run mode", res["message"])
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    def test_youtube_auth_url_unconfigured_response(self):
        """Rule 14: Clear setup instructions when credentials missing"""
        yt = YouTubeService()
        yt.client_id = None
        yt.client_secret = None
        res = yt.get_auth_url()
        self.assertFalse(res["configured"])
        self.assertIn("not configured", res["message"])
        self.assertEqual(res["auth_url"], "")

    def test_real_database_persistence_and_relationships(self):
        """Rule 4: Real database persistence using SQLAlchemy"""
        user = User(email="creator@thoughtflow.ai", password_hash="secret_hash", role="creator")
        self.db.add(user)
        self.db.commit()

        thought = Thought(
            user_id=user.id,
            text="Small steps create extraordinary change.",
            theme="consistency",
            title="Small Steps #Shorts",
            description="Compounding daily habits.",
            content_hash=compute_content_hash("Small steps create extraordinary change."),
            status="approved"
        )
        self.db.add(thought)
        self.db.commit()

        video = Video(
            thought_id=thought.id,
            storage_key="test_video_1.mp4",
            title=thought.title,
            description=thought.description,
            render_status="completed",
            upload_status="not_uploaded"
        )
        self.db.add(video)
        self.db.commit()

        # Verify query and relations
        fetched_thought = self.db.query(Thought).filter(Thought.user_id == user.id).first()
        self.assertIsNotNone(fetched_thought)
        self.assertEqual(len(fetched_thought.videos), 1)
        self.assertEqual(fetched_thought.videos[0].upload_status, "not_uploaded")
        self.assertIsNone(fetched_thought.videos[0].youtube_url)

if __name__ == "__main__":
    unittest.main()
