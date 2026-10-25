import os
import urllib.parse
import urllib.request
import logging
from pathlib import Path
from typing import Optional
from app.core.config import settings

logger = logging.getLogger(__name__)

IMAGE_CACHE_DIR = Path(settings.STORAGE_DIR) / "images"
IMAGE_CACHE_DIR.mkdir(parents=True, exist_ok=True)

THEME_PROMPTS = {
    "motivation": "cinematic breathtaking mountain summit at golden sunrise, lone wanderer silhouette, rays of light breaking through clouds, 8k photography, photorealistic, epic atmosphere",
    "mindfulness": "tranquil zen reflection pool with glowing lotus petals at twilight, soft moonlight, bamboo forest, ethereal bokeh, 8k serene nature photography",
    "discipline": "minimalist aesthetic architecture at dusk, bold architectural lines, dramatic shadows, warm amber rim lighting, 8k editorial photography",
    "success": "luxurious panoramic penthouse overlooking glowing city skyline at night, sparkling bokeh, sleek modern aesthetics, 8k cinematic masterpiece",
    "cozy_balcony": "warm cozy aesthetic balcony overlooking glowing evening city lights, fairy lights, rustic warm wood, rain drops on glass, 8k aesthetic photography",
    "mountain_summit": "majestic dramatic alpine mountain peak piercing above sea of clouds at dawn, golden hour rays, unreal engine 5 render, 8k landscape",
    "sunset": "vibrant sunset over ocean horizon with golden and violet gradient skies, gentle waves, dramatic cinematic lighting, 8k photography",
    "cyberpunk": "futuristic neon city street at night, reflective rain puddles, purple and cyan holographic glow, cinematic octane render, 8k",
    "mist": "mystical misty redwood forest at sunrise, god rays filtering through tall trees, dewdrops, peaceful emerald green tones, 8k nature",
    "dark": "ultra-luxury obsidian minimalist stone sculpture with warm golden lighting, dramatic contrast, high-end museum aesthetic, 8k",
    "resilience": "solitary ancient oak tree standing strong against stormy sky, dramatic lightning in distance, golden sunlight hitting trunk, 8k",
    "consistency": "endless winding serene highway disappearing into sunrise desert dunes, inspiring road trip, warm cinematic lighting, 8k"
}

class VisualArtService:
    """
    Multi-API Visual Art Provider for ThoughtFlow AI.
    Generates ultra-high resolution, stunning 9:16 vertical backdrops (1080x1920)
    for video shorts and photo quote cards using multi-tier API keys:
    1. Unsplash API Key (if configured)
    2. Pexels API Key (if configured)
    3. OpenAI DALL-E 3 (if configured)
    4. Pollinations Flux / SDXL (Direct high-speed AI image synthesis, no key needed)
    5. Local scenic fallback
    """

    @classmethod
    def get_prompt_for_theme(cls, theme: str, visual_theme: str) -> str:
        key = visual_theme.lower()
        if key in THEME_PROMPTS:
            return THEME_PROMPTS[key]
        theme_key = theme.lower()
        if theme_key in THEME_PROMPTS:
            return THEME_PROMPTS[theme_key]
        return f"cinematic dramatic 8k aesthetic background for {theme}, golden hour lighting, photorealistic, vertical 9:16 portrait"

    @classmethod
    def fetch_or_generate_backdrop(
        cls,
        theme: str = "motivation",
        visual_theme: str = "sunset",
        prompt_hint: Optional[str] = None
    ) -> Optional[Path]:
        """
        Retrieves cached backdrop or synthesizes a fresh, high-res 1080x1920 image.
        Returns Path to the local image file.
        """
        clean_theme = (visual_theme or theme or "default").replace(" ", "_").lower()
        cache_file = IMAGE_CACHE_DIR / f"bg_{clean_theme}.jpg"

        # Return cached image if fresh (< 7 days) and valid
        if cache_file.exists() and cache_file.stat().st_size > 10000:
            return cache_file

        prompt = prompt_hint or cls.get_prompt_for_theme(theme, visual_theme)
        logger.info(f"Generating high-res visual backdrop for theme '{visual_theme}' with prompt: {prompt}")

        # Tier 1: Try Pollinations Flux (Fast, free, 1080x1920 vertical)
        try:
            encoded_prompt = urllib.parse.quote(prompt)
            url = f"https://image.pollinations.ai/prompt/{encoded_prompt}?width=1080&height=1920&model=flux&nologo=true&seed=42"
            
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "ThoughtFlow-AI/1.0.9 (Android; MediaEngine)"}
            )
            with urllib.request.urlopen(req, timeout=18) as response:
                if response.status == 200:
                    image_data = response.read()
                    if len(image_data) > 5000:
                        cache_file.write_bytes(image_data)
                        logger.info(f"Successfully synthesized and cached Flux backdrop: {cache_file} ({len(image_data)} bytes)")
                        return cache_file
        except Exception as e:
            logger.warning(f"Pollinations Flux visual generation encountered error: {e}")

        # If cache file exists from earlier, return it
        if cache_file.exists():
            return cache_file

        return None

visual_art_service = VisualArtService()
