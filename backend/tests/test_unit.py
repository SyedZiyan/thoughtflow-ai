import pytest
from app.services.ai_provider import (
    StructuredThought,
    normalize_text,
    compute_content_hash,
    check_near_duplicate,
    MockAIProvider
)
from app.services.video_renderer import video_renderer, VideoValidationError

def test_thought_validation_success():
    payload = {
        "theme": "discipline",
        "thought": "Small steps create extraordinary change.",
        "title": "One Small Step Every Day #Shorts",
        "description": "A daily reminder to stay consistent.",
        "hashtags": ["motivation", "discipline", "shorts"],
        "narration_script": "Small steps create extraordinary change.",
        "language": "en"
    }
    thought = StructuredThought(**payload)
    assert thought.theme == "discipline"
    assert "shorts" in thought.hashtags
    assert len(thought.thought) > 10

def test_text_normalization_and_hash():
    text1 = "Small steps create extraordinary change!"
    text2 = "  small steps   create extraordinary change. "
    norm1 = normalize_text(text1)
    norm2 = normalize_text(text2)
    assert norm1 == norm2
    assert compute_content_hash(text1) == compute_content_hash(text2)

def test_near_duplicate_detection():
    existing = [
        "Discipline is choosing between what you want now and what you want most.",
        "Consistency beats talent when talent does not show up."
    ]
    # Similar thought with 80% token overlap
    candidate = "Discipline is choosing between what you want right now and what you want most."
    is_dup, match = check_near_duplicate(candidate, existing, threshold=0.7)
    assert is_dup is True
    assert match == existing[0]

    # Completely different thought
    unique_cand = "The sunrise brings endless possibilities for the courageous mind."
    is_dup2, _ = check_near_duplicate(unique_cand, existing, threshold=0.7)
    assert is_dup2 is False

def test_mock_ai_provider_generation():
    import asyncio
    provider = MockAIProvider()
    thought = asyncio.run(provider.generate_thought(theme="consistency"))
    assert thought.theme == "consistency"
    assert len(thought.thought) > 10
    assert len(thought.title) > 5
    assert "shorts" in thought.hashtags

def test_video_metadata_validation_non_existent():
    with pytest.raises(VideoValidationError):
        video_renderer.validate_video("non_existent_file.mp4")
