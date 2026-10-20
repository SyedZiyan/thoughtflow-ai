import logging
from datetime import datetime, timezone
from typing import List, Optional, Tuple, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.models.models import User, Thought, UserSettings, ActivityLog
from app.services.ai_provider import (
    get_ai_provider,
    compute_content_hash,
    check_near_duplicate,
    StructuredThought,
    AIProviderError
)

logger = logging.getLogger("thoughtflow.thought_service")

class ThoughtServiceError(Exception):
    pass

class DuplicateThoughtError(ThoughtServiceError):
    pass

class ThoughtService:
    DEFAULT_THEMES = [
        "discipline",
        "motivation",
        "consistency",
        "resilience",
        "success",
        "mindfulness",
        "productivity",
        "confidence",
        "self-improvement",
        "life lessons",
        "personal growth",
        "relationships"
    ]

    @classmethod
    def select_next_theme(
        cls,
        user_id: str,
        db: Session,
        allowed_themes: Optional[List[str]] = None
    ) -> str:
        """
        Intelligently rotates through allowed themes using Least-Recently-Used (LRU) selection:
        - If an allowed theme has never been used, pick it immediately.
        - Otherwise, pick the theme that was used the furthest in the past.
        """
        pool = allowed_themes or cls.DEFAULT_THEMES
        if not pool:
            return "motivation"

        clean_pool = [t.strip().lower() for t in pool if t.strip()]

        # Query the latest thought date for each theme
        theme_recency = (
            db.query(Thought.theme, func.max(Thought.created_at).label("last_used"))
            .filter(Thought.user_id == user_id)
            .group_by(Thought.theme)
            .all()
        )
        recency_map = {row[0].lower(): row[1] for row in theme_recency}

        # Check for un-used themes first
        unused_themes = [t for t in clean_pool if t not in recency_map]
        if unused_themes:
            return unused_themes[0]

        # Otherwise pick the least recently used
        sorted_by_oldest = sorted(clean_pool, key=lambda t: recency_map.get(t, datetime.min.replace(tzinfo=timezone.utc)))
        return sorted_by_oldest[0]

    @classmethod
    def check_is_duplicate(
        cls,
        candidate_text: str,
        user_id: str,
        db: Session,
        near_threshold: float = 0.70
    ) -> Tuple[bool, str, Optional[str]]:
        """
        Check both exact hash collision and semantic lexical token overlap.
        Returns: (is_duplicate, reason, matched_text_or_none)
        """
        # 1. Exact hash check
        content_hash = compute_content_hash(candidate_text)
        exact_match = (
            db.query(Thought)
            .filter(Thought.user_id == user_id, Thought.content_hash == content_hash)
            .first()
        )
        if exact_match:
            return True, "exact_duplicate", exact_match.text

        # 2. Near-duplicate overlap check against past 100 thoughts
        past_thoughts = (
            db.query(Thought.text)
            .filter(Thought.user_id == user_id)
            .order_by(Thought.created_at.desc())
            .limit(100)
            .all()
        )
        past_texts = [p[0] for p in past_thoughts]
        is_near, matched_text = check_near_duplicate(candidate_text, past_texts, threshold=near_threshold)
        if is_near:
            return True, "near_duplicate_overlap", matched_text

        return False, "unique", None

    @classmethod
    async def generate_and_save_thought(
        cls,
        db: Session,
        user: User,
        theme: Optional[str] = None,
        tone: str = "inspiring",
        language: str = "en",
        custom_prompt: Optional[str] = None,
        max_attempts: int = 3,
        status: str = "approved"
    ) -> Thought:
        """
        End-to-end engine to generate, validate, verify uniqueness, and persist a structured thought.
        Retries with altered guidance if a duplicate is detected.
        """
        # Resolve user settings
        user_settings = user.settings
        allowed_themes = user_settings.selected_themes if user_settings and user_settings.selected_themes else cls.DEFAULT_THEMES

        # 1. Theme rotation if not explicitly passed
        selected_theme = theme.lower().strip() if theme else cls.select_next_theme(user.id, db, allowed_themes)

        provider = get_ai_provider()
        last_duplicate_reason = None

        # 2. Generation & Duplicate check loop
        for attempt in range(1, max_attempts + 1):
            guidance = custom_prompt or ""
            if attempt > 1:
                guidance = f"{guidance} Provide an entirely distinct, original insight. Seed: {attempt}".strip()

            logger.info(f"Generating thought attempt {attempt}/{max_attempts} for theme '{selected_theme}'...")
            generated: StructuredThought = await provider.generate_thought(
                theme=selected_theme,
                tone=tone,
                language=language,
                custom_prompt=guidance
            )

            # Check duplicates
            is_dup, reason, matched = cls.check_is_duplicate(generated.thought, user.id, db)
            if not is_dup:
                # Unique! Persist to DB
                content_hash = compute_content_hash(generated.thought)
                thought = Thought(
                    user_id=user.id,
                    text=generated.thought,
                    theme=selected_theme,
                    language=generated.language,
                    title=generated.title,
                    description=generated.description,
                    hashtags=generated.hashtags,
                    narration_script=generated.narration_script,
                    content_hash=content_hash,
                    prompt_version=generated.prompt_version,
                    status=status
                )
                db.add(thought)

                # Log activity
                log = ActivityLog(
                    user_id=user.id,
                    event_type="thought_generated",
                    message=f"Generated original thought for theme '{selected_theme}': \"{thought.text[:60]}...\"",
                    log_metadata={
                        "theme": selected_theme,
                        "model": generated.model_name,
                        "prompt_version": generated.prompt_version,
                        "token_usage": generated.token_usage.model_dump() if generated.token_usage else None
                    }
                )
                db.add(log)
                db.commit()
                db.refresh(thought)
                logger.info(f"Successfully generated and saved unique thought {thought.id} (hash: {content_hash[:8]}).")
                return thought

            last_duplicate_reason = f"{reason} (matched: '{matched[:40]}...')"
            logger.warning(f"Thought generated on attempt {attempt} rejected as {last_duplicate_reason}. Retrying...")

        raise DuplicateThoughtError(f"Failed to generate a completely unique thought after {max_attempts} attempts. Last reason: {last_duplicate_reason}")
