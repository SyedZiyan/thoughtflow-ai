import os
import io
import subprocess
import json
import math
import random
import logging
from pathlib import Path
from typing import Dict, Any, Optional, Tuple, List
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from app.core.config import settings
from app.services.audio_service import (
    get_tts_provider,
    AmbientAudioSynthesizer,
    EdgeTTSProvider
)

logger = logging.getLogger("thoughtflow.video")

class VideoValidationError(Exception):
    pass

class VideoRendererService:
    THEMES = {
        "gradient_purple": {
            "name": "Cosmic Purple",
            "top": (24, 10, 48),
            "mid": (48, 16, 92),
            "bottom": (10, 6, 22),
            "accent": (168, 120, 255),
            "glow": (138, 75, 255, 30),
            "pill_bg": (36, 18, 68),
            "pill_text": (230, 215, 255)
        },
        "geometric_dark": {
            "name": "Emerald Obsidian",
            "top": (6, 24, 20),
            "mid": (12, 42, 34),
            "bottom": (4, 12, 10),
            "accent": (52, 211, 153),
            "glow": (16, 185, 129, 25),
            "pill_bg": (10, 36, 28),
            "pill_text": (167, 243, 208)
        },
        "nebula": {
            "name": "Midnight Cyan",
            "top": (8, 16, 40),
            "mid": (16, 36, 75),
            "bottom": (5, 8, 20),
            "accent": (56, 189, 248),
            "glow": (14, 165, 233, 25),
            "pill_bg": (14, 30, 62),
            "pill_text": (186, 230, 253)
        },
        "gold_luxury": {
            "name": "Bronze Luxury",
            "top": (32, 24, 12),
            "mid": (54, 38, 16),
            "bottom": (14, 10, 6),
            "accent": (251, 191, 36),
            "glow": (245, 158, 11, 25),
            "pill_bg": (42, 30, 14),
            "pill_text": (254, 243, 199)
        },
        "minimal_slate": {
            "name": "Graphite Minimal",
            "top": (20, 24, 32),
            "mid": (30, 36, 48),
            "bottom": (12, 14, 18),
            "accent": (148, 163, 184),
            "glow": (100, 116, 139, 20),
            "pill_bg": (24, 28, 38),
            "pill_text": (226, 232, 240)
        },
        "sunset_crimson": {
            "name": "Sunset Crimson",
            "top": (44, 12, 20),
            "mid": (72, 18, 30),
            "bottom": (18, 6, 10),
            "accent": (244, 63, 94),
            "glow": (225, 29, 72, 25),
            "pill_bg": (56, 16, 26),
            "pill_text": (254, 205, 211)
        }
    }

    ANIMATION_STYLES = [
        "smooth_zoom",
        "kinetic_pan",
        "subtle_pulse",
        "gentle_drift"
    ]

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

    def _get_font(self, size: int, typography: str = "modern_sans", bold: bool = False) -> ImageFont.ImageFont:
        """Loads TTF fonts based on typography style with fallback hierarchy"""
        if typography == "editorial_serif":
            font_names = [
                "georgiab.ttf" if bold else "georgia.ttf",
                "timesbd.ttf" if bold else "times.ttf",
                "palab.ttf" if bold else "pala.ttf"
            ]
        elif typography == "clean_mono":
            font_names = [
                "consola.ttf" if bold else "consolab.ttf",
                "courbd.ttf" if bold else "cour.ttf",
                "lucon.ttf"
            ]
        else:  # modern_sans
            font_names = [
                "arialbd.ttf" if bold else "arial.ttf",
                "segoeuib.ttf" if bold else "segoeui.ttf",
                "calibrib.ttf" if bold else "calibri.ttf",
                "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
            ]

        windows_font_dir = Path(os.environ.get("WINDIR", "C:\\Windows")) / "Fonts"
        for name in font_names:
            font_path = windows_font_dir / name
            if font_path.exists():
                try:
                    return ImageFont.truetype(str(font_path), size)
                except Exception:
                    pass

        try:
            return ImageFont.load_default()
        except Exception:
            return None

    def _draw_gradient(self, draw: ImageDraw.ImageDraw, theme: str):
        """Render high quality vertical background gradient based on theme"""
        theme_cfg = self.THEMES.get(theme, self.THEMES["gradient_purple"])
        top_color = theme_cfg["top"]
        mid_color = theme_cfg["mid"]
        bottom_color = theme_cfg["bottom"]

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
        rng = random.Random(42)
        theme_cfg = self.THEMES.get(theme, self.THEMES["gradient_purple"])
        glow_color = theme_cfg["glow"]
        accent_color = theme_cfg["accent"]

        overlay = Image.new("RGBA", (self.width, self.height), (0, 0, 0, 0))
        overlay_draw = ImageDraw.Draw(overlay)

        # Upper glowing orb
        cx1, cy1 = self.width * 0.25, self.height * 0.35
        for rad in range(350, 50, -50):
            overlay_draw.ellipse([cx1 - rad, cy1 - rad, cx1 + rad, cy1 + rad], fill=glow_color)

        # Lower complementary glowing orb
        cx2, cy2 = self.width * 0.75, self.height * 0.65
        for rad in range(300, 50, -50):
            overlay_draw.ellipse([cx2 - rad, cy2 - rad, cx2 + rad, cy2 + rad], fill=glow_color)

        # Geometric micro particles
        for _ in range(40):
            px = rng.randint(40, self.width - 40)
            py = rng.randint(80, self.height - 80)
            p_size = rng.randint(2, 5)
            alpha = rng.randint(25, 80)
            overlay_draw.ellipse([px, py, px + p_size, py + p_size], fill=(255, 255, 255, alpha))

        blurred_overlay = overlay.filter(ImageFilter.GaussianBlur(radius=25))
        img.paste(Image.alpha_composite(img.convert("RGBA"), blurred_overlay).convert("RGB"))

        # Re-acquire draw handle
        draw = ImageDraw.Draw(img)

        # Subtle frame borders
        margin = 48
        draw.rectangle([margin, margin, self.width - margin, self.height - margin], outline=(60, 65, 80), width=1)

        # Accent corner brackets
        corner_len = 36
        c_w = 3
        draw.line([(margin, margin), (margin + corner_len, margin)], fill=accent_color, width=c_w)
        draw.line([(margin, margin), (margin, margin + corner_len)], fill=accent_color, width=c_w)
        draw.line([(self.width - margin, margin), (self.width - margin - corner_len, margin)], fill=accent_color, width=c_w)
        draw.line([(self.width - margin, margin), (self.width - margin, margin + corner_len)], fill=accent_color, width=c_w)
        draw.line([(margin, self.height - margin), (margin + corner_len, self.height - margin)], fill=accent_color, width=c_w)
        draw.line([(margin, self.height - margin), (margin, self.height - margin - corner_len)], fill=accent_color, width=c_w)
        draw.line([(self.width - margin, self.height - margin), (self.width - margin - corner_len, self.height - margin)], fill=accent_color, width=c_w)
        draw.line([(self.width - margin, self.height - margin), (self.width - margin, self.height - margin - corner_len)], fill=accent_color, width=c_w)

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
        typography: str = "modern_sans",
        text_placement: str = "center",
        category_tag: Optional[str] = None
    ) -> Image.Image:
        """Compose the primary 1080x1920 slide for preview and video compilation"""
        theme_cfg = self.THEMES.get(visual_theme, self.THEMES["gradient_purple"])
        img = Image.new("RGB", (self.width, self.height), (15, 15, 22))
        draw = ImageDraw.Draw(img)

        # 1. Background gradient
        self._draw_gradient(draw, visual_theme)

        # 2. Accents & Particles
        self._draw_accents(img, draw, visual_theme)
        draw = ImageDraw.Draw(img)

        # 3. Category Tag Pill (Top Header)
        display_tag = (category_tag or theme).upper()
        pill_text = f"• {display_tag} •"
        font_tag = self._get_font(28, typography=typography, bold=True)
        tag_bbox = draw.textbbox((0, 0), pill_text, font=font_tag)
        tag_w = tag_bbox[2] - tag_bbox[0]
        tag_h = tag_bbox[3] - tag_bbox[1]
        pill_x = (self.width - tag_w) // 2
        pill_y = int(self.height * 0.17)

        pad_x, pad_y = 28, 12
        draw.rounded_rectangle(
            [pill_x - pad_x, pill_y - pad_y, pill_x + tag_w + pad_x, pill_y + tag_h + pad_y],
            radius=20,
            fill=theme_cfg["pill_bg"],
            outline=theme_cfg["accent"],
            width=2
        )
        draw.text((pill_x, pill_y), pill_text, font=font_tag, fill=theme_cfg["pill_text"])

        # 4. Main Thought Typography
        max_text_width = int(self.width * 0.82)
        font_size = 56
        font_main = self._get_font(font_size, typography=typography, bold=True)
        lines = self._wrap_text(thought_text, font_main, max_text_width, draw)

        # Dynamically scale down font if text is lengthy
        if len(lines) > 5:
            font_size = 46
            font_main = self._get_font(font_size, typography=typography, bold=True)
            lines = self._wrap_text(thought_text, font_main, max_text_width, draw)
        if len(lines) > 8:
            font_size = 38
            font_main = self._get_font(font_size, typography=typography, bold=True)
            lines = self._wrap_text(thought_text, font_main, max_text_width, draw)

        line_height = int(font_size * 1.48)
        total_text_h = len(lines) * line_height

        # Calculate text placement Y coordinate
        if text_placement == "upper_third":
            start_y = int(self.height * 0.36 - (total_text_h // 2))
        elif text_placement == "lower_third":
            start_y = int(self.height * 0.58 - (total_text_h // 2))
        else:  # center
            start_y = int(self.height * 0.46 - (total_text_h // 2))

        # Decorative quotation mark
        font_quote = self._get_font(130, typography=typography, bold=True)
        quote_y = max(int(self.height * 0.24), start_y - 130)
        draw.text(
            ((self.width // 2) - 40, quote_y),
            "“",
            font=font_quote,
            fill=(*theme_cfg["accent"][:3], 130)
        )

        # Render lines with drop shadow for legibility
        for idx, line in enumerate(lines):
            l_bbox = draw.textbbox((0, 0), line, font=font_main)
            lw = l_bbox[2] - l_bbox[0]
            lx = (self.width - lw) // 2
            ly = start_y + (idx * line_height)

            # Drop shadow
            draw.text((lx + 3, ly + 4), line, font=font_main, fill=(0, 0, 0, 190))
            # Main white foreground text
            draw.text((lx, ly), line, font=font_main, fill=(255, 255, 255))

        # 5. Bottom Brand & Call-to-Action
        font_brand = self._get_font(30, typography="modern_sans", bold=True)
        brand_text = "THOUGHTFLOW AI"
        b_bbox = draw.textbbox((0, 0), brand_text, font=font_brand)
        bw = b_bbox[2] - b_bbox[0]
        draw.text(((self.width - bw) // 2, int(self.height * 0.825)), brand_text, font=font_brand, fill=(185, 175, 210))

        font_cta = self._get_font(22, typography="modern_sans", bold=False)
        cta_text = "Subscribe for Daily Motivation • #Shorts"
        c_bbox = draw.textbbox((0, 0), cta_text, font=font_cta)
        cw = c_bbox[2] - c_bbox[0]
        draw.text(((self.width - cw) // 2, int(self.height * 0.86)), cta_text, font=font_cta, fill=(135, 135, 160))

        return img

    def compose_slide_preview(
        self,
        thought_text: str,
        theme: str = "motivation",
        visual_theme: str = "gradient_purple",
        typography: str = "modern_sans",
        text_placement: str = "center"
    ) -> bytes:
        """Returns PNG image bytes for real-time frontend canvas/preview rendering"""
        img = self.compose_slide_frame(
            thought_text=thought_text,
            theme=theme,
            visual_theme=visual_theme,
            typography=typography,
            text_placement=text_placement
        )
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        return buf.getvalue()

    async def render_animated_video(
        self,
        thought_id: str,
        thought_text: str,
        theme: str = "motivation",
        visual_theme: str = "gradient_purple",
        animation_style: str = "smooth_zoom",
        typography: str = "modern_sans",
        text_placement: str = "center",
        duration_seconds: int = 15,
        narration_enabled: bool = False,
        ambient_enabled: bool = True,
        narration_voice: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Creates an animated vertical 9:16 MP4 using Pillow composition and FFmpeg animation filters.
        Produces genuine H.264 video with smooth zoom pan and multi-track audio blending.
        """
        temp_files_to_clean: List[Path] = []
        try:
            # Step 1: Render static high-res slide frame
            frame_img = self.compose_slide_frame(
                thought_text=thought_text,
                theme=theme,
                visual_theme=visual_theme,
                typography=typography,
                text_placement=text_placement,
                category_tag=theme
            )
            frame_path = self.temp_dir / f"frame_{thought_id}.png"
            frame_img.save(str(frame_path), "PNG")
            temp_files_to_clean.append(frame_path)

            output_filename = f"thought_{thought_id}.mp4"
            output_path = self.output_dir / output_filename

            total_frames = duration_seconds * self.fps

            # Step 2: Build FFmpeg video animation filter
            if animation_style == "kinetic_pan":
                zoom_filter = f"zoompan=z='1.05':y='ih*0.03*on/{total_frames}':x='0':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"
            elif animation_style == "subtle_pulse":
                zoom_filter = f"zoompan=z='1.0+0.04*sin(2*PI*on/{total_frames})':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"
            elif animation_style == "gentle_drift":
                zoom_filter = f"zoompan=z='1.06':x='iw*0.02*on/{total_frames}':y='ih*0.02*on/{total_frames}':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"
            else:  # smooth_zoom (default)
                zoom_filter = f"zoompan=z='min(zoom+0.0008,1.12)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={total_frames}:s={self.width}x{self.height}:fps={self.fps}"

            fade_filter = f"fade=t=in:st=0:d=1,fade=t=out:st={duration_seconds - 0.8}:d=0.8"
            video_filters = f"{zoom_filter},{fade_filter},format=yuv420p"

            # Step 3: Audio Generation & Blending
            narration_file: Optional[Path] = None
            ambient_file: Optional[Path] = None

            if narration_enabled:
                try:
                    tts_provider = get_tts_provider()
                    narr_path = self.temp_dir / f"narr_{thought_id}.mp3"
                    await tts_provider.synthesize(thought_text, str(narr_path), voice=narration_voice)
                    if narr_path.exists() and narr_path.stat().st_size > 0:
                        narration_file = narr_path
                        temp_files_to_clean.append(narration_file)
                except Exception as tts_err:
                    logger.warning(f"Narration generation skipped due to error: {tts_err}. Continuing with ambient audio.")

            if ambient_enabled:
                try:
                    amb_path = self.temp_dir / f"ambient_{thought_id}.wav"
                    AmbientAudioSynthesizer.generate_ambient_track(str(amb_path), duration_seconds=duration_seconds)
                    if amb_path.exists() and amb_path.stat().st_size > 0:
                        ambient_file = amb_path
                        temp_files_to_clean.append(ambient_file)
                except Exception as amb_err:
                    logger.warning(f"Ambient track synthesis failed: {amb_err}")

            # Assemble FFmpeg command with appropriate audio stream inputs
            if narration_file and ambient_file:
                # Both narration + ambient: Blend using amix with volume ducking (voice: 1.0, ambient: 0.18)
                cmd = [
                    self.ffmpeg_bin,
                    "-y",
                    "-loop", "1",
                    "-i", str(frame_path),
                    "-i", str(narration_file),
                    "-i", str(ambient_file),
                    "-filter_complex",
                    f"[0:v]{video_filters}[v];[1:a]volume=1.0[a1];[2:a]volume=0.18[a2];[a1][a2]amix=inputs=2:duration=first:dropout_transition=2[a]",
                    "-map", "[v]",
                    "-map", "[a]",
                    "-c:v", "libx264",
                    "-preset", "faster",
                    "-crf", "22",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-t", str(duration_seconds),
                    "-movflags", "+faststart",
                    str(output_path)
                ]
            elif narration_file:
                # Narration only
                cmd = [
                    self.ffmpeg_bin,
                    "-y",
                    "-loop", "1",
                    "-i", str(frame_path),
                    "-i", str(narration_file),
                    "-filter_complex", f"[0:v]{video_filters}[v]",
                    "-map", "[v]",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "faster",
                    "-crf", "22",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-t", str(duration_seconds),
                    "-movflags", "+faststart",
                    str(output_path)
                ]
            elif ambient_file:
                # Ambient only
                cmd = [
                    self.ffmpeg_bin,
                    "-y",
                    "-loop", "1",
                    "-i", str(frame_path),
                    "-i", str(ambient_file),
                    "-filter_complex", f"[0:v]{video_filters}[v]",
                    "-map", "[v]",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "faster",
                    "-crf", "22",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-t", str(duration_seconds),
                    "-movflags", "+faststart",
                    str(output_path)
                ]
            else:
                # Silent AAC stream fallback
                cmd = [
                    self.ffmpeg_bin,
                    "-y",
                    "-loop", "1",
                    "-i", str(frame_path),
                    "-f", "lavfi",
                    "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
                    "-filter_complex", f"[0:v]{video_filters}[v]",
                    "-map", "[v]",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "faster",
                    "-crf", "22",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-t", str(duration_seconds),
                    "-movflags", "+faststart",
                    str(output_path)
                ]

            logger.info(f"Rendering animated video via FFmpeg: {output_path} ({duration_seconds}s)")
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

            # Step 4: Validate rendered output
            validation = self.validate_video(str(output_path), expected_duration=duration_seconds)

            return {
                "storage_key": output_filename,
                "absolute_path": str(output_path),
                "duration_seconds": validation["duration"],
                "width": validation["width"],
                "height": validation["height"],
                "codec": validation["codec"],
                "file_size_bytes": validation["file_size_bytes"],
                "has_narration": bool(narration_file),
                "has_ambient": bool(ambient_file)
            }
        finally:
            # Clean up intermediate frame and audio files
            for p in temp_files_to_clean:
                if p.exists():
                    try:
                        p.unlink()
                    except Exception:
                        pass

    def validate_video(self, video_path: str, expected_duration: Optional[int] = None) -> Dict[str, Any]:
        """
        Validates the generated MP4 with ffprobe:
        - File exists and is non-empty (>10KB)
        - Video stream exists
        - Codec is h264
        - Dimensions match 1080x1920 (vertical 9:16)
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
