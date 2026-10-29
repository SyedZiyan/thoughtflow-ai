import io
import os
import wave
import pytest
from pathlib import Path
from PIL import Image
from fastapi.testclient import TestClient
from app.main import app
from app.services.video_renderer import video_renderer, VideoValidationError
from app.services.audio_service import (
    AmbientAudioSynthesizer,
    EdgeTTSProvider,
    get_tts_provider
)

client = TestClient(app)

# ==========================================
# 1. SLIDE FRAME COMPOSITION ACROSS THEMES
# ==========================================

def test_compose_slide_frame_all_themes():
    themes = [
        "gradient_purple",
        "geometric_dark",
        "nebula",
        "gold_luxury",
        "minimal_slate",
        "sunset_crimson"
    ]
    thought_text = "Master the mundane repetitions. That is where quiet greatness is forged."

    for theme in themes:
        img = video_renderer.compose_slide_frame(
            thought_text=thought_text,
            theme="consistency",
            visual_theme=theme,
            typography="modern_sans",
            text_placement="center",
            category_tag="CONSISTENCY"
        )
        assert isinstance(img, Image.Image)
        assert img.size == (1080, 1920)
        assert img.mode == "RGB"

def test_compose_slide_preview_bytes():
    png_bytes = video_renderer.compose_slide_preview(
        thought_text="Action creates the confidence that waiting destroys.",
        theme="confidence",
        visual_theme="nebula",
        typography="editorial_serif",
        text_placement="center"
    )
    assert isinstance(png_bytes, bytes)
    assert len(png_bytes) > 5000
    # PNG signature check: \x89PNG\r\n\x1a\n
    assert png_bytes[:8] == b"\x89PNG\r\n\x1a\n"

    # Verify bytes can be reopened by PIL
    reopened = Image.open(io.BytesIO(png_bytes))
    assert reopened.size == (1080, 1920)

def test_typography_and_placement_options():
    typos = ["modern_sans", "editorial_serif", "clean_mono"]
    placements = ["upper_third", "center", "lower_third"]

    for typo in typos:
        for plc in placements:
            img = video_renderer.compose_slide_frame(
                thought_text="A short test thought for placement testing.",
                theme="mindfulness",
                visual_theme="minimal_slate",
                typography=typo,
                text_placement=plc
            )
            assert img.size == (1080, 1920)


# ==========================================
# 2. AUDIO SYNTHESIS & TTS
# ==========================================

def test_ambient_audio_synthesizer(tmp_path):
    output_wav = str(tmp_path / "test_ambient.wav")
    path = AmbientAudioSynthesizer.generate_ambient_track(
        output_wav_path=output_wav,
        duration_seconds=3,
        sample_rate=44100
    )
    assert os.path.exists(path)
    assert os.path.getsize(path) > 10000

    # Inspect WAV file headers
    with wave.open(path, "r") as wf:
        assert wf.getnchannels() == 2
        assert wf.getsampwidth() == 2
        assert wf.getframerate() == 44100
        duration = wf.getnframes() / 44100
        assert 2.9 <= duration <= 3.1

def test_tts_provider_configuration():
    provider = get_tts_provider()
    assert isinstance(provider, EdgeTTSProvider)
    assert len(EdgeTTSProvider.SUPPORTED_VOICES) >= 4
    voice_ids = [v["id"] for v in EdgeTTSProvider.SUPPORTED_VOICES]
    assert "en-US-ChristopherNeural" in voice_ids
    assert "en-US-JennyNeural" in voice_ids


# ==========================================
# 3. VIDEO VALIDATION LOGIC
# ==========================================

def test_video_validation_missing_file():
    with pytest.raises(VideoValidationError, match="Video file does not exist"):
        video_renderer.validate_video("storage/videos/non_existent_thought_file.mp4")

def test_video_validation_file_too_small(tmp_path):
    small_file = tmp_path / "tiny.mp4"
    small_file.write_bytes(b"corrupt header only")
    with pytest.raises(VideoValidationError, match="Video file too small"):
        video_renderer.validate_video(str(small_file))


# ==========================================
# 4. FULL VIDEO RENDERING (FFMPEG H.264/AAC)
# ==========================================

@pytest.mark.asyncio
async def test_full_animated_video_render():
    result = await video_renderer.render_animated_video(
        thought_id="phase3_test_01",
        thought_text="Energy follows focus. Direct your mind toward solutions, and obstacles shrink.",
        theme="motivation",
        visual_theme="sunset_crimson",
        animation_style="kinetic_pan",
        typography="modern_sans",
        text_placement="center",
        duration_seconds=3,
        ambient_enabled=True,
        narration_enabled=False
    )
    assert os.path.exists(result["absolute_path"])
    assert result["width"] == 1080
    assert result["height"] == 1920
    assert result["codec"] == "h264"
    assert result["file_size_bytes"] > 50000
    assert result["has_ambient"] is True


# ==========================================
# 5. VIDEO API ENDPOINTS
# ==========================================

def test_api_video_styles_endpoint():
    resp = client.get("/api/v1/videos/styles")
    assert resp.status_code == 200
    data = resp.json()
    assert "visual_themes" in data
    assert len(data["visual_themes"]) >= 5
    assert "animation_styles" in data
    assert "typography_options" in data
    assert "text_placements" in data
    assert "tts_voices" in data

def test_api_preview_frame_endpoint(auth_client):
    payload = {
        "thought_text": "Discipline is choosing between what you want now and what you want most.",
        "theme": "discipline",
        "visual_theme": "gold_luxury",
        "typography": "clean_mono",
        "text_placement": "center"
    }
    resp = auth_client.post("/api/v1/videos/preview-frame", json=payload)
    assert resp.status_code == 200
    assert resp.headers["content-type"] == "image/png"
    assert len(resp.content) > 5000
    assert resp.content[:8] == b"\x89PNG\r\n\x1a\n"
