import os
import sys
from pathlib import Path

# Add backend directory to sys.path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from app.services.video_renderer import video_renderer

def test_manual_render():
    print("Testing VideoRendererService with real Pillow composition and FFmpeg rendering...")
    result = video_renderer.render_animated_video(
        thought_id="test_sample_01",
        thought_text="Small steps taken consistently create extraordinary transformations.",
        theme="consistency",
        visual_theme="gradient_purple",
        animation_style="smooth_zoom",
        duration_seconds=3
    )
    print("Video Render Success!")
    print("Details:", result)
    assert os.path.exists(result["absolute_path"])
    assert result["width"] == 1080
    assert result["height"] == 1920
    assert result["file_size_bytes"] > 10000
    print("ALL ASSERTIONS PASSED! Real vertical animated MP4 generated successfully.")

if __name__ == "__main__":
    test_manual_render()
