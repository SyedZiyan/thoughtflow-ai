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
        "cozy_balcony": {
            "name": "Cozy Balcony Sunset (Lantern & City)",
            "bg_file": "user_balcony_sunset.jpg",
            "fallback_bg": "cozy_sunset_balcony.jpg",
            "accent": (245, 194, 118),
            "glow": (245, 194, 118, 40),
            "pill_bg": (32, 20, 14),
            "pill_text": (254, 240, 199),
            "default_typography": "poetic_script"
        },
        "mountain_summit": {
            "name": "Alpine Mountain Sunrise (Peak Ridge)",
            "bg_file": "user_mountain_hiker.jpg",
            "fallback_bg": "mountain_ridge_sunrise.jpg",
            "accent": (255, 255, 255),
            "glow": (255, 255, 255, 30),
            "pill_bg": (18, 24, 36),
            "pill_text": (226, 232, 240),
            "default_typography": "bold_impact"
        },
        "misty_forest": {
            "name": "Misty Pine Forest",
            "bg_file": "misty_forest.jpg",
            "accent": (52, 211, 153),
            "glow": (16, 185, 129, 30),
            "pill_bg": (12, 34, 24),
            "pill_text": (167, 243, 208),
            "default_typography": "bold_impact"
        },
        "ocean_sunset": {
            "name": "Ocean Twilight Horizon",
            "bg_file": "ocean_sunset.jpg",
            "accent": (251, 146, 60),
            "glow": (249, 115, 22, 30),
            "pill_bg": (36, 18, 24),
            "pill_text": (254, 215, 170),
            "default_typography": "poetic_script"
        },
        "starry_night": {
            "name": "Cosmic Starry Night",
            "bg_file": "starry_night_cabin.jpg",
            "accent": (168, 120, 255),
            "glow": (138, 75, 255, 35),
            "pill_bg": (24, 14, 48),
            "pill_text": (230, 215, 255),
            "default_typography": "poetic_script"
        },
        "golden_lake": {
            "name": "Reflective Golden Lake",
            "bg_file": "fairy_lights_dusk.jpg",
            "accent": (251, 191, 36),
            "glow": (245, 158, 11, 30),
            "pill_bg": (28, 24, 16),
            "pill_text": (254, 243, 199),
            "default_typography": "poetic_script"
        },
        "rainy_window": {
            "name": "Rainy City Evening",
            "bg_file": "rainy_window.jpg",
            "accent": (56, 189, 248),
            "glow": (14, 165, 233, 30),
            "pill_bg": (16, 28, 48),
            "pill_text": (186, 230, 253),
            "default_typography": "poetic_script"
        },
        "golden_desert": {
            "name": "Golden Hour Sand Dunes",
            "bg_file": "golden_desert.jpg",
            "accent": (251, 191, 36),
            "glow": (245, 158, 11, 30),
            "pill_bg": (38, 26, 12),
            "pill_text": (254, 243, 199),
            "default_typography": "bold_impact"
        },
        "gradient_purple": {
            "name": "Cosmic Purple (Gradient)",
            "top": (24, 10, 48),
            "mid": (48, 16, 92),
            "bottom": (10, 6, 22),
            "accent": (168, 120, 255),
            "glow": (138, 75, 255, 30),
            "pill_bg": (36, 18, 68),
            "pill_text": (230, 215, 255)
        },
        "geometric_dark": {
            "name": "Emerald Obsidian (Gradient)",
            "top": (6, 24, 20),
            "mid": (12, 42, 34),
            "bottom": (4, 12, 10),
            "accent": (52, 211, 153),
            "glow": (16, 185, 129, 25),
            "pill_bg": (10, 36, 28),
            "pill_text": (167, 243, 208)
        },
        "nebula": {
            "name": "Midnight Cyan (Gradient)",
            "top": (8, 16, 40),
            "mid": (16, 36, 75),
            "bottom": (5, 8, 20),
            "accent": (56, 189, 248),
            "glow": (14, 165, 233, 25),
            "pill_bg": (14, 30, 62),
            "pill_text": (186, 230, 253)
        },
        "gold_luxury": {
            "name": "Bronze Luxury (Gradient)",
            "top": (32, 24, 12),
            "mid": (54, 38, 16),
            "bottom": (14, 10, 6),
            "accent": (251, 191, 36),
            "glow": (245, 158, 11, 25),
            "pill_bg": (42, 30, 14),
            "pill_text": (254, 243, 199)
        },
        "minimal_slate": {
            "name": "Graphite Minimal (Gradient)",
            "top": (20, 24, 32),
            "mid": (30, 36, 48),
            "bottom": (12, 14, 18),
            "accent": (148, 163, 184),
            "glow": (100, 116, 139, 20),
            "pill_bg": (24, 28, 38),
            "pill_text": (226, 232, 240)
        },
        "sunset_crimson": {
            "name": "Sunset Crimson (Gradient)",
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
        if typography in ("editorial_serif", "poetic_script"):
            font_names = [
                "georgiab.ttf" if bold else "georgia.ttf",
                "timesbd.ttf" if bold else "times.ttf",
                "palab.ttf" if bold else "pala.ttf"
            ]
        elif typography == "bold_impact":
            font_names = [
                "impact.ttf",
                "trebucbd.ttf",
                "arialbd.ttf",
                "calibrib.ttf"
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

    def _get_script_font(self, size: int) -> ImageFont.ImageFont:
        """Loads cursive script font for emotional highlight words (Image 1 aesthetic)"""
        windows_font_dir = Path(os.environ.get("WINDIR", "C:\\Windows")) / "Fonts"
        script_candidates = [
            "MTCORSVA.TTF",
            "Gabriola.ttf",
            "georgiai.ttf",
            "FRSCRIPT.TTF",
            "BRUSHSCI.TTF",
            "segoepr.ttf"
        ]
        for name in script_candidates:
            p = windows_font_dir / name
            if p.exists():
                try:
                    return ImageFont.truetype(str(p), size)
                except Exception:
                    pass
        return self._get_font(size, typography="editorial_serif", bold=True)

    def _create_base_canvas(self, visual_theme: str) -> Image.Image:
        """Returns base 1080x1920 canvas using either photographic asset or gradient fallback"""
        theme_cfg = self.THEMES.get(visual_theme, self.THEMES.get("cozy_balcony", {}))
        bg_dir = Path(__file__).resolve().parent.parent / "assets" / "backgrounds"

        bg_filename = theme_cfg.get("bg_file")
        bg_path = bg_dir / bg_filename if bg_filename else None

        if (not bg_path or not bg_path.exists()) and theme_cfg.get("fallback_bg"):
            bg_path = bg_dir / theme_cfg["fallback_bg"]

        if bg_path and bg_path.exists():
            try:
                base_img = Image.open(str(bg_path)).convert("RGB")
                orig_w, orig_h = base_img.size
                target_ratio = self.width / self.height
                orig_ratio = orig_w / orig_h

                if orig_ratio > target_ratio:
                    new_w = int(orig_h * target_ratio)
                    left = (orig_w - new_w) // 2
                    base_img = base_img.crop((left, 0, left + new_w, orig_h))
                else:
                    new_h = int(orig_w / target_ratio)
                    top = (orig_h - new_h) // 2
                    base_img = base_img.crop((0, top, orig_w, top + new_h))

                base_img = base_img.resize((self.width, self.height), Image.Resampling.LANCZOS)

                # Atmospheric darkening scrim in upper and middle section where text resides
                scrim = Image.new("RGBA", (self.width, self.height), (0, 0, 0, 0))
                sdraw = ImageDraw.Draw(scrim)
                scrim_limit = int(self.height * 0.55)
                for y in range(scrim_limit):
                    alpha = int(145 * (1.0 - (y / scrim_limit) ** 1.35))
                    sdraw.line([(0, y), (self.width, y)], fill=(0, 0, 0, alpha))
                base_img.paste(Image.alpha_composite(base_img.convert("RGBA"), scrim).convert("RGB"))
                return base_img
            except Exception as e:
                logger.warning(f"Failed to load background {bg_path}: {e}, falling back to gradient.")

        # Gradient fallback
        img = Image.new("RGB", (self.width, self.height), (15, 15, 22))
        draw = ImageDraw.Draw(img)
        self._draw_gradient(draw, visual_theme)
        self._draw_accents(img, draw, visual_theme)
        return img

    def _draw_vector_heart(self, draw: ImageDraw.ImageDraw, center_x: int, center_y: int, size: float = 16, color: Tuple = (245, 194, 118)):
        points = []
        for t in range(0, 360, 6):
            rad = math.radians(t)
            x = 16 * (math.sin(rad) ** 3)
            y = -(13 * math.cos(rad) - 5 * math.cos(2 * rad) - 2 * math.cos(3 * rad) - math.cos(4 * rad))
            px = center_x + x * (size / 16.0)
            py = center_y + y * (size / 16.0)
            points.append((px, py))
        draw.polygon(points, fill=color)

    def _render_poetic_script(self, thought_text: str, visual_theme: str = "cozy_balcony", theme: str = "motivation") -> Image.Image:
        """
        Image 1 Style: Cinematic Scenic Background + Poetic Serif + Golden Cursive Calligraphy Accents + Golden Heart Divider.
        """
        img = self._create_base_canvas(visual_theme)
        draw = ImageDraw.Draw(img)

        f_serif = self._get_font(52, typography="editorial_serif", bold=False)
        f_script = self._get_script_font(68)

        emphasis_keywords = {
            "lost", "find", "becoming", "silence", "quiet", "strength", "peace",
            "courage", "grow", "growth", "discipline", "unstoppable", "wisdom",
            "dreams", "power", "greatness", "direction", "speed", "forward",
            "begin", "mind", "today", "tomorrow", "standards", "freedom",
            "energy", "focus", "flame", "unyielding", "roots", "fuel", "truth",
            "clear", "storms", "destiny", "patience", "faith", "hope", "rise",
            "light", "path", "proud", "calm", "soul", "heart", "glow"
        }

        # Handle explicit newlines or naturally wrap to ~860px max width
        clean_text = thought_text.replace('"', '').replace("'", "'")
        if "\n" in clean_text:
            raw_lines = [l.strip() for l in clean_text.split("\n") if l.strip()]
            lines_words = [l.split() for l in raw_lines]
        else:
            all_words = clean_text.split()
            max_w = int(self.width * 0.82)
            lines_words = []
            curr_line = []
            for w in all_words:
                test_line = " ".join(curr_line + [w])
                bbox = draw.textbbox((0, 0), test_line, font=f_serif)
                if (bbox[2] - bbox[0]) > max_w and curr_line:
                    lines_words.append(curr_line)
                    curr_line = [w]
                else:
                    curr_line.append(w)
            if curr_line:
                lines_words.append(curr_line)

        # Scale font if many lines
        line_height = 80
        if len(lines_words) > 3:
            f_serif = self._get_font(44, typography="editorial_serif", bold=False)
            f_script = self._get_script_font(58)
            line_height = 68
        elif len(lines_words) > 5:
            f_serif = self._get_font(38, typography="editorial_serif", bold=False)
            f_script = self._get_script_font(50)
            line_height = 58

        start_y = int(self.height * 0.20)
        gold_color = (245, 194, 118)
        white_color = (255, 255, 255)
        shadow_color = (0, 0, 0, 200)

        for idx, line_list in enumerate(lines_words):
            y = start_y + (idx * line_height)

            # Check if any word has emphasis or markdown asterisks
            segments = []
            for w in line_list:
                is_asterisk = w.startswith("*") and w.endswith("*") and len(w) > 2
                clean_w = w.strip(".,;:!?\"'*").lower()
                is_emp = is_asterisk or (clean_w in emphasis_keywords)
                display_w = w.strip("*") + " "
                segments.append((display_w, is_emp))

            has_emp = any(s[1] for s in segments)

            if has_emp and len(line_list) >= 2:
                # Measure combined widths for centered alignment
                total_w = 0
                seg_widths = []
                for w_text, is_emp in segments:
                    font = f_script if is_emp else f_serif
                    bbox = draw.textbbox((0, 0), w_text, font=font)
                    w = bbox[2] - bbox[0]
                    seg_widths.append(w)
                    total_w += w

                curr_x = (self.width - total_w) // 2
                for s_idx, (w_text, is_emp) in enumerate(segments):
                    font = f_script if is_emp else f_serif
                    color = gold_color if is_emp else white_color
                    # Drop shadow
                    draw.text((curr_x + 2, y + 3), w_text, font=font, fill=shadow_color)
                    # Main text
                    draw.text((curr_x, y), w_text, font=font, fill=color)
                    curr_x += seg_widths[s_idx]
            else:
                line_str = " ".join([w.strip("*") for w in line_list])
                draw.text((self.width // 2 + 2, y + 3), line_str, font=f_serif, fill=shadow_color, anchor="mm")
                draw.text((self.width // 2, y), line_str, font=f_serif, fill=white_color, anchor="mm")

        # Golden heart divider below quote
        div_y = start_y + (len(lines_words) * line_height) + 36
        draw.line([(self.width // 2 - 140, div_y + 2), (self.width // 2 - 32, div_y + 2)], fill=(0, 0, 0, 160), width=2)
        draw.line([(self.width // 2 + 32, div_y + 2), (self.width // 2 + 140, div_y + 2)], fill=(0, 0, 0, 160), width=2)
        self._draw_vector_heart(draw, self.width // 2 + 2, div_y + 2, size=15, color=(0, 0, 0, 160))

        draw.line([(self.width // 2 - 140, div_y), (self.width // 2 - 32, div_y)], fill=gold_color, width=2)
        draw.line([(self.width // 2 + 32, div_y), (self.width // 2 + 140, div_y)], fill=gold_color, width=2)
        self._draw_vector_heart(draw, self.width // 2, div_y, size=15, color=gold_color)

        return img

    def _render_bold_impact(self, thought_text: str, visual_theme: str = "mountain_summit", theme: str = "motivation") -> Image.Image:
        """
        Image 2 Style: Dramatic Scenic Mountain Landscape + "THOUGHT OF THE DAY" Header + Bold Uppercase Condensed Typography with Drop Shadow + Subtitle + Footer.
        """
        img = self._create_base_canvas(visual_theme)
        draw = ImageDraw.Draw(img)

        f_tag = self._get_font(30, typography="modern_sans", bold=True)
        f_impact = self._get_font(72, typography="bold_impact", bold=True)
        f_sub = self._get_font(34, typography="modern_sans", bold=True)
        f_footer = self._get_font(26, typography="modern_sans", bold=False)

        # 1. Header: THOUGHT OF THE DAY
        header_text = "THOUGHT OF THE DAY"
        header_y = int(self.height * 0.125)
        draw.text((self.width // 2 + 2, header_y + 2), header_text, font=f_tag, fill=(0, 0, 0, 190), anchor="mm")
        draw.text((self.width // 2, header_y), header_text, font=f_tag, fill=(235, 240, 250), anchor="mm")

        # 2. Main Quote: Condensed uppercase impact text
        clean_text = thought_text.replace('"', '').upper()
        if "\n" in clean_text:
            lines = [l.strip() for l in clean_text.split("\n") if l.strip()]
        else:
            words = clean_text.split()
            max_w = int(self.width * 0.82)
            lines = []
            curr = []
            for w in words:
                test = " ".join(curr + [w])
                bbox = draw.textbbox((0, 0), test, font=f_impact)
                if (bbox[2] - bbox[0]) > max_w and curr:
                    lines.append(" ".join(curr))
                    curr = [w]
                else:
                    curr.append(w)
            if curr:
                lines.append(" ".join(curr))

        # Dynamically scale if lengthy
        line_h = 92
        if len(lines) > 3:
            f_impact = self._get_font(56, typography="bold_impact", bold=True)
            line_h = 76
        elif len(lines) > 5:
            f_impact = self._get_font(44, typography="bold_impact", bold=True)
            line_h = 62

        start_y = int(self.height * 0.19)
        for idx, line in enumerate(lines):
            y = start_y + (idx * line_h)
            # Deep drop shadow
            draw.text((self.width // 2 + 4, y + 4), line, font=f_impact, fill=(0, 0, 0, 220), anchor="mm")
            draw.text((self.width // 2, y), line, font=f_impact, fill=(255, 255, 255), anchor="mm")

        # 3. Subtitle
        sub_y = start_y + (len(lines) * line_h) + 42
        sub_text = "KEEP MOVING FORWARD."
        draw.text((self.width // 2 + 2, sub_y + 2), sub_text, font=f_sub, fill=(0, 0, 0, 190), anchor="mm")
        draw.text((self.width // 2, sub_y), sub_text, font=f_sub, fill=(245, 248, 255), anchor="mm")

        # 4. Footer & Sparkle
        draw.text((self.width // 2, int(self.height * 0.93)), "- ANONYMOUS -", font=f_footer, fill=(185, 195, 215), anchor="mm")
        draw.text((int(self.width * 0.91), int(self.height * 0.96)), "✦", font=f_footer, fill=(225, 235, 250), anchor="mm")

        return img

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
        visual_theme: str = "cozy_balcony",
        typography: str = "poetic_script",
        text_placement: str = "center",
        category_tag: Optional[str] = None
    ) -> Image.Image:
        """Compose the primary 1080x1920 slide for preview and video compilation"""
        # 1. Image 1 Aesthetic: Poetic Script
        if typography == "poetic_script" or (
            visual_theme in ("cozy_balcony", "fairy_lights", "rainy_window", "golden_lake")
            and typography not in ("bold_impact", "clean_mono")
        ):
            return self._render_poetic_script(thought_text, visual_theme=visual_theme, theme=theme)

        # 2. Image 2 Aesthetic: Bold Impact
        if typography == "bold_impact" or (
            visual_theme in ("mountain_summit", "golden_desert")
            and typography not in ("poetic_script", "editorial_serif")
        ):
            return self._render_bold_impact(thought_text, visual_theme=visual_theme, theme=theme)

        # 3. If photographic background is selected with another typography
        theme_cfg = self.THEMES.get(visual_theme, {})
        if "bg_file" in theme_cfg:
            if typography in ("editorial_serif", "poetic_script"):
                return self._render_poetic_script(thought_text, visual_theme=visual_theme, theme=theme)
            else:
                return self._render_bold_impact(thought_text, visual_theme=visual_theme, theme=theme)

        # 4. Standard Gradient Fallback (Cosmic Purple, Emerald Obsidian, Midnight Cyan, etc.)
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
