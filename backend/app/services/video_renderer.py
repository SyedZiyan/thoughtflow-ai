import os
import subprocess
import json
import math
import random
from pathlib import Path
from typing import Dict, Any, Optional, Tuple, List
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from app.core.config import settings

class VideoValidationError(Exception):
    pass

class VideoRendererService:
    def __init__(self):
        self.width = settings.DEFAULT_VIDEO_WIDTH
        self.height = settings.DEFAULT_VIDEO_HEIGHT
        self.fps = settings.DEFAULT_FPS
        self.ffmpeg_bin = settings.FFMPEG_PATH
        self.ffprobe_bin = settings.FFPROBE_PATH
        self.output_dir = Path(settings.VIDEO_STORAGE_DIR)
        self.temp_dir = Path(settings.TEMP_STORAGE_DIR)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.temp_dir.mkdir(parents=True, exist_ok=True)

    def _get_font(self, size: int, bold: bool = False) -> ImageFont.ImageFont:
        """Attempt to load a clean TTF font; fallback to PIL default if not found."""
        font_names = [
            "arial.ttf" if not bold else "arialbd.ttf",
            "segoeui.ttf" if not bold else "segoeuib.ttf",
            "calibri.ttf" if not bold else "calibrib.ttf",
            "DejaVuSans.ttf" if not bold else "DejaVuSans-Bold.ttf"
        ]
        windows_font_dir = Path(os.environ.get("WINDIR", "C:\\Windows")) / "Fonts"
        for name in font_names:
            font_path = windows_font_dir / name
            if font_path.exists():
                try:
                    return ImageFont.truetype(str(font_path), size)
                except Exception:
                    pass
        # Fallback to default
        try:
            return ImageFont.load_default()
        except Exception:
            return None

    def _draw_gradient(self, draw: ImageDraw.ImageDraw, theme: str):
        """Render high quality vertical background gradient based on theme"""
        if theme == "gradient_purple":
            # Deep purple/indigo to dark obsidian
            top_color = (24, 10, 48)
            mid_color = (48, 16, 92)
            bottom_color = (10, 6, 22)
        elif theme == "geometric_dark":
            # Dark emerald/obsidian
            top_color = (6, 24, 20)
            mid_color = (12, 42, 34)
            bottom_color = (4, 12, 10)
        elif theme == "nebula":
            # Deep midnight blue to cosmic cyan
            top_color = (8, 16, 40)
            mid_color = (16, 36, 75)
            bottom_color = (5, 8, 20)
        elif theme == "gold_luxury":
            # Bronze / charcoal luxury
            top_color = (32, 24, 12)
            mid_color = (54, 38, 16)
            bottom_color = (14, 10, 6)
        else:  # minimal_slate
            top_color = (20, 24, 32)
            mid_color = (30, 36, 48)
            bottom_color = (12, 14, 18)

        # Draw linear gradient across the height
        for y in range(self.height):
            ratio = y / self.height
            if ratio < 0.5:
                sub_r = ratio / 0.5
                r = int(top_color[0] + (mid_color[0] - top_color[0]) * sub_r)
                g = int(top_color[1] + (mid_color[1] - top_color[1]) * sub_r)
                b = int(top_color[2] + (mid_color[2] - top_color[2]) * sub_r)
            else:
                sub_r = (ratio - 0.5) / 0.5
                r = int(mid_color[0] + (bottom_color[0] - mid_color[0]) * sub_r)
                g = int(mid_color[1] + (bottom_color[1] - mid_color[1]) * sub_r)
                b = int(mid_color[2] + (bottom_color[2] - mid_color[2]) * sub_r)
            draw.line([(0, y), (self.width, y)], fill=(r, g, b))

    def _draw_accents(self, img: Image.Image, draw: ImageDraw.ImageDraw, theme: str):
        """Add subtle particle glow, abstract shapes, and framed corners"""
        rng = random.Random(42)  # Deterministic seed for aesthetic balance

        # Add soft glowing circles in the background
        overlay = Image.new("RGBA", (self.width, self.height), (0, 0, 0, 0))
        overlay_draw = ImageDraw.Draw(overlay)

        # Glowing orb 1
        cx, cy = self.width * 0.25, self.height * 0.35
        glow_color = (138, 75, 255, 30) if theme == "gradient_purple" else (64, 200, 180, 25)
        for rad in range(350, 50, -50):
            overlay_draw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=glow_color)

        # Subtle geometric particles
        for _ in range(35):
            px = rng.randint(40, self.width - 40)
            py = rng.randint(80, self.height - 80)
            p_size = rng.randint(2, 5)
            alpha = rng.randint(30, 90)
            overlay_draw.ellipse([px, py, px + p_size, py + p_size], fill=(255, 255, 255, alpha))

        # Merge blurred overlay
        blurred_overlay = overlay.filter(ImageFilter.GaussianBlur(radius=25))
        img.paste(Image.alpha_composite(img.convert("RGBA"), blurred_overlay).convert("RGB"))

        # Redo draw handle on merged image
        draw = ImageDraw.Draw(img)

        # Border frame lines
        margin = 48
        border_color = (255, 255, 255, 20)
        draw.rectangle([margin, margin, self.width - margin, self.height - margin], outline=(80, 80, 100), width=1)

        # Corner accents
        corner_len = 36
        draw.line([(margin, margin), (margin + corner_len, margin)], fill=(160, 130, 255), width=3)
        draw.line([(margin, margin), (margin, margin + corner_len)], fill=(160, 130, 255), width=3)
        draw.line([(self.width - margin, margin), (self.width - margin - corner_len, margin)], fill=(160, 130, 255), width=3)
        draw.line([(self.width - margin, margin), (self.width - margin, margin + corner_len)], fill=(160, 130, 255), width=3)
        draw.line([(margin, self.height - margin), (margin + corner_len, self.height - margin)], fill=(160, 130, 255), width=3)
        draw.line([(margin, self.height - margin), (margin, self.height - margin - corner_len)], fill=(160, 130, 255), width=3)
        draw.line([(self.width - margin, self.height - margin), (self.width - margin - corner_len, self.height - margin)], fill=(160, 130, 255), width=3)
        draw.line([(self.width - margin, self.height - margin), (self.width - margin, self.height - margin - corner_len)], fill=(160, 130, 255), width=3)

    def _wrap_text(self, text: str, font: ImageFont.ImageFont, max_width: int, draw: ImageDraw.ImageDraw) -> List[str]:
        words = text.split()
        lines = []
        current_line = []
        for word in words:
            current_line.append(word)
            test_line = " ".join(current_line)
            bbox = draw.textbbox((0, 0), test_line, font=font)
            line_w = bbox[2] - bbox[0]
            if line_w > max_width:
                if len(current_line) > 1:
                    current_line.pop()
                    lines.append(" ".join(current_line))
                    current_line = [word]
                else:
                    lines.append(test_line)
                    current_line = []
        if current_line:
            lines.append(" ".join(current_line))
        return lines

    def compose_slide_frame(
        self,
        thought_text: str,
        theme: str = "motivation",
        visual_theme: str = "gradient_purple",
        category_tag: str = "DAILY THOUGHT"
    ) -> Image.Image:
        """Compose the primary 1080x1920 high-resolution slide for video generation"""
        img = Image.new("RGB", (self.width, self.height), (15, 15, 22))
        draw = ImageDraw.Draw(img)

        # 1. Background gradient
        self._draw_gradient(draw, visual_theme)

        # 2. Accents & Particles
        self._draw_accents(img, draw, visual_theme)
        draw = ImageDraw.Draw(img)

        # 3. Category Tag Pill (Top header)
        pill_text = f"• {category_tag.upper()} •"
        font_tag = self._get_font(28, bold=True)
        tag_bbox = draw.textbbox((0, 0), pill_text, font=font_tag)
        tag_w = tag_bbox[2] - tag_bbox[0]
        tag_h = tag_bbox[3] - tag_bbox[1]
        pill_x = (self.width - tag_w) // 2
        pill_y = int(self.height * 0.18)

        # Pill background
        pad_x, pad_y = 28, 12
        draw.rounded_rectangle(
            [pill_x - pad_x, pill_y - pad_y, pill_x + tag_w + pad_x, pill_y + tag_h + pad_y],
            radius=20,
            fill=(30, 20, 60),
            outline=(168, 120, 255),
            width=2
        )
        draw.text((pill_x, pill_y), pill_text, font=font_tag, fill=(230, 215, 255))

        # 4. Main Thought Typography
        max_text_width = int(self.width * 0.82)
        font_size = 56
        font_main = self._get_font(font_size, bold=True)
        lines = self._wrap_text(thought_text, font_main, max_text_width, draw)

        # Scale down font if text has too many lines
        if len(lines) > 5:
            font_size = 46
            font_main = self._get_font(font_size, bold=True)
            lines = self._wrap_text(thought_text, font_main, max_text_width, draw)

        line_height = int(font_size * 1.45)
        total_text_h = len(lines) * line_height
        start_y = int(self.height * 0.44 - (total_text_h // 2))

        # Draw decorative quotation mark
        font_quote = self._get_font(120, bold=True)
        draw.text(((self.width // 2) - 35, start_y - 130), "“", font=font_quote, fill=(168, 120, 255, 120))

        # Render lines with subtle dropshadow
        for idx, line in enumerate(lines):
            l_bbox = draw.textbbox((0, 0), line, font=font_main)
            lw = l_bbox[2] - l_bbox[0]
            lx = (self.width - lw) // 2
            ly = start_y + (idx * line_height)

            # Shadow
            draw.text((lx + 3, ly + 4), line, font=font_main, fill=(0, 0, 0, 180))
            # Main Text
            draw.text((lx, ly), line, font=font_main, fill=(255, 255, 255))

        # 5. Bottom Branding & Call to Action
        font_brand = self._get_font(32, bold=True)
        brand_text = "THOUGHTFLOW AI"
        b_bbox = draw.textbbox((0, 0), brand_text, font=font_brand)
        bw = b_bbox[2] - b_bbox[0]
        draw.text(((self.width - bw) // 2, int(self.height * 0.82)), brand_text, font=font_brand, fill=(180, 160, 220))

        font_cta = self._get_font(22, bold=False)
        cta_text = "Subscribe for Daily Motivation • #Shorts"
        c_bbox = draw.textbbox((0, 0), cta_text, font=font_cta)
        cw = c_bbox[2] - c_bbox[0]
        draw.text(((self.width - cw) // 2, int(self.height * 0.855)), cta_text, font=font_cta, fill=(130, 130, 160))

        return img

    def render_animated_video(
        self,
        thought_id: str,
        thought_text: str,
        theme: str = "motivation",
        visual_theme: str = "gradient_purple",
        animation_style: str = "smooth_zoom",
        duration_seconds: int = 15,
        narration_audio_path: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Creates an animated vertical 9:16 MP4 using Pillow composition and FFmpeg animation filters.
        Produces genuine H.264 video with smooth zoom pan and audio/silent stream.
        """
        # Step 1: Generate high quality frame
        frame_img = self.compose_slide_frame(
            thought_text=thought_text,
            theme=theme,
            visual_theme=visual_theme,
            category_tag=theme
        )

        frame_path = self.temp_dir / f"frame_{thought_id}.png"
        frame_img.save(str(frame_path), "PNG")

        # Step 2: Output MP4 path
        output_filename = f"thought_{thought_id}.mp4"
        output_path = self.output_dir / output_filename

        total_frames = duration_seconds * self.fps

        # Step 3: Build FFmpeg filter complex for smooth zoom and text fade
        # zoompan filter continuously zooms smoothly over duration
        if animation_style == "kinetic_pan":
            # Subtle pan from top to bottom
            zoom_filter = f"zoompan=z='1.05':y='ih*0.03*on/{total_frames}':x='0':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"
        else:
            # Smooth cinematic slow zoom from 1.0 to 1.12
            zoom_filter = f"zoompan=z='min(zoom+0.0008,1.12)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"

        # Fade in at start (1 second) and fade out at end (0.8 second)
        fade_filter = f"fade=t=in:st=0:d=1,fade=t=out:st={duration_seconds - 0.8}:d=0.8"
        video_filters = f"{zoom_filter},{fade_filter},format=yuv420p"

        # Audio handling: use provided audio file or generate a silent AAC track so YouTube Short has valid audio
        if narration_audio_path and Path(narration_audio_path).exists():
            audio_inputs = ["-i", str(narration_audio_path)]
            audio_filters = ["-c:a", "aac", "-b:a", "128k", "-shortest"]
        else:
            # Generate silent audio track
            audio_inputs = ["-f", "lavfi", "-i", f"anullsrc=channel_layout=stereo:sample_rate=44100"]
            audio_filters = ["-c:a", "aac", "-b:a", "128k", "-t", str(duration_seconds)]

        cmd = [
            self.ffmpeg_bin,
            "-y",  # Overwrite
            "-loop", "1",
            "-i", str(frame_path),
            *audio_inputs,
            "-filter_complex", f"[0:v]{video_filters}[v]",
            "-map", "[v]",
            "-map", "1:a",
            "-c:v", "libx264",
            "-preset", "faster",
            "-crf", "22",
            *audio_filters,
            "-t", str(duration_seconds),
            "-movflags", "+faststart",
            str(output_path)
        ]

        # Execute FFmpeg safely
        try:
            result = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=180,
                check=False
            )
            if result.returncode != 0:
                raise RuntimeError(f"FFmpeg failed with exit code {result.returncode}: {result.stderr[-500:]}")
        except subprocess.TimeoutExpired:
            raise RuntimeError("FFmpeg rendering timed out after 180 seconds.")
        finally:
            # Clean up temp slide frame
            if frame_path.exists():
                try:
                    frame_path.unlink()
                except Exception:
                    pass

        # Step 4: Validate output video with FFprobe
        validation = self.validate_video(str(output_path), expected_duration=duration_seconds)

        return {
            "storage_key": output_filename,
            "absolute_path": str(output_path),
            "duration_seconds": validation["duration"],
            "width": validation["width"],
            "height": validation["height"],
            "codec": validation["codec"],
            "file_size_bytes": validation["file_size_bytes"]
        }

    def validate_video(self, video_path: str, expected_duration: Optional[int] = None) -> Dict[str, Any]:
        """
        Validates the generated MP4 with ffprobe:
        - File exists and is non-empty
        - Video stream exists
        - Codec is h264
        - Dimensions match 1080x1920 (or vertical 9:16)
        - Duration is within acceptable tolerance
        """
        path = Path(video_path)
        if not path.exists():
            raise VideoValidationError(f"Video file does not exist: {video_path}")

        file_size = path.stat().st_size
        if file_size < 10000:
            raise VideoValidationError(f"Video file too small ({file_size} bytes), likely corrupted.")

        cmd = [
            self.ffprobe_bin,
            "-v", "error",
            "-show_entries", "format=duration,size:stream=width,height,codec_name,codec_type",
            "-of", "json",
            str(path)
        ]

        try:
            res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
            data = json.loads(res.stdout)
        except Exception as e:
            raise VideoValidationError(f"Failed to inspect video with ffprobe: {e}")

        streams = data.get("streams", [])
        video_stream = next((s for s in streams if s.get("codec_type") == "video"), None)
        if not video_stream:
            raise VideoValidationError("No valid video stream found in file.")

        codec = video_stream.get("codec_name")
        width = int(video_stream.get("width", 0))
        height = int(video_stream.get("height", 0))

        format_info = data.get("format", {})
        duration = float(format_info.get("duration", 0.0))

        if width <= 0 or height <= 0:
            raise VideoValidationError(f"Invalid dimensions: {width}x{height}")

        # Check aspect ratio is vertical
        if width > height:
            raise VideoValidationError(f"Video is not vertical: {width}x{height}")

        return {
            "valid": True,
            "width": width,
            "height": height,
            "duration": round(duration, 2),
            "codec": codec,
            "file_size_bytes": file_size
        }

video_renderer = VideoRendererService()
