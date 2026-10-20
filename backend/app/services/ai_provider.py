import abc
import asyncio
import hashlib
import json
import logging
import random
import re
from typing import List, Optional, Tuple, Dict, Any
from pydantic import BaseModel, Field, field_validator, model_validator
import httpx
from app.core.config import settings
from app.prompts.thought_prompts import PROMPT_VERSION, SYSTEM_INSTRUCTION, build_thought_generation_prompt

logger = logging.getLogger("thoughtflow.ai")

class AIProviderError(Exception):
    """Base exception for AI provider failures"""
    pass

class AIRateLimitError(AIProviderError):
    """Raised when an AI provider hits rate limits or quota boundaries"""
    pass

class AIMalformedResponseError(AIProviderError):
    """Raised when an AI provider returns invalid or unparseable structured data"""
    pass


class TokenUsage(BaseModel):
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0
    estimated_cost_usd: float = 0.0


class StructuredThought(BaseModel):
    theme: str = Field(..., min_length=2, max_length=50)
    thought: str = Field(..., min_length=10, max_length=280)
    title: str = Field(..., min_length=5, max_length=100)
    description: str = Field(..., min_length=15, max_length=5000)
    hashtags: List[str] = Field(default_factory=list)
    narration_script: Optional[str] = None
    language: str = Field(default="en", min_length=2, max_length=10)
    model_name: str = "mock"
    prompt_version: str = PROMPT_VERSION
    token_usage: Optional[TokenUsage] = None

    @field_validator("thought")
    @classmethod
    def validate_thought_content(cls, v: str) -> str:
        clean = v.strip()
        if not clean or len(clean) < 10:
            raise ValueError("Thought text cannot be empty or shorter than 10 characters.")
        if len(clean) > 280:
            raise ValueError("Thought text cannot exceed 280 characters for Shorts display.")
        return clean

    @field_validator("title")
    @classmethod
    def validate_title(cls, v: str) -> str:
        clean = v.strip()
        if not clean:
            raise ValueError("Title cannot be empty.")
        if "#shorts" not in clean.lower() and "#Shorts" not in clean:
            clean = f"{clean} #Shorts"
        return clean[:100]

    @field_validator("description")
    @classmethod
    def validate_description(cls, v: str) -> str:
        clean = v.strip()
        if not clean or len(clean) < 15:
            raise ValueError("Description must be at least 15 characters.")
        return clean

    @field_validator("hashtags")
    @classmethod
    def clean_hashtags(cls, tags: List[str]) -> List[str]:
        cleaned = []
        for t in tags:
            tag = re.sub(r'[^a-zA-Z0-9_]', '', t).strip().lower()
            if tag and tag not in cleaned:
                cleaned.append(tag)
        if "shorts" not in cleaned:
            cleaned.append("shorts")
        return cleaned[:8]

    @field_validator("theme")
    @classmethod
    def validate_theme(cls, v: str) -> str:
        clean = v.strip().lower()
        if not clean:
            raise ValueError("Theme cannot be empty.")
        return clean


def normalize_text(text: str) -> str:
    """
    Normalize text for exact hash comparison:
    - Lowercase
    - Strip all punctuation and special characters
    - Collapse multiple whitespace characters into single space
    """
    text = text.lower()
    text = re.sub(r'[^\w\s]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def compute_content_hash(text: str) -> str:
    """Generate SHA-256 hash of normalized text for exact duplicate detection."""
    norm = normalize_text(text)
    return hashlib.sha256(norm.encode('utf-8')).hexdigest()


def check_near_duplicate(
    candidate: str,
    existing_texts: List[str],
    threshold: float = 0.70
) -> Tuple[bool, Optional[str]]:
    """
    Check if candidate text has high lexical overlap with any existing thought using Jaccard token similarity.
    Returns (is_duplicate, matching_text).
    """
    cand_tokens = set(normalize_text(candidate).split())
    if not cand_tokens:
        return False, None

    for existing in existing_texts:
        exist_tokens = set(normalize_text(existing).split())
        if not exist_tokens:
            continue
        intersection = cand_tokens.intersection(exist_tokens)
        union = cand_tokens.union(exist_tokens)
        similarity = len(intersection) / len(union) if union else 0.0
        if similarity >= threshold:
            return True, existing

    return False, None


class BaseAIProvider(abc.ABC):
    """Abstract Base Class for replaceable AI Providers"""

    @abc.abstractmethod
    async def generate_thought(
        self,
        theme: str,
        tone: str = "inspiring",
        language: str = "en",
        custom_prompt: str = ""
    ) -> StructuredThought:
        """Generates a validated StructuredThought instance"""
        pass


class MockAIProvider(BaseAIProvider):
    """
    Robust local provider requiring zero API keys and producing high-quality thoughts across 12 themes.
    Ensures 100% offline local testing, automated tests, and near-zero cost development.
    """
    CATALOG: Dict[str, List[Tuple[str, str, str]]] = {
        "motivation": [
            ("Your future is built by what you do today, not what you promise to do tomorrow.", "Build Today, Conquer Tomorrow #Shorts", "Every morning is a clean slate to build your highest potential and leave excuses behind."),
            ("Stop waiting for the right moment. The moment becomes right the second you begin.", "Stop Waiting, Start Now #Shorts", "Action creates the confidence that waiting destroys. Step forward with conviction."),
            ("Energy follows focus. Direct your mind toward solutions, and your obstacles will shrink.", "Direct Your Energy #Shorts", "What you feed your focus expands your world. Starve your distractions."),
            ("Greatness is not a sudden lightning bolt; it is a fire stoked by daily effort.", "The Flame of Greatness #Shorts", "Feed your purpose every single morning without fail."),
        ],
        "discipline": [
            ("Discipline is choosing between what you want right now and what you want most.", "The True Price of Discipline #Shorts", "Freedom is never given; it is won through daily self-mastery and relentless consistency."),
            ("Motivation gets you started; unconditional discipline brings you to the finish line.", "Discipline Over Motivation #Shorts", "The champions build their empire on days they didn't feel like training."),
            ("You do not rise to the level of your goals, you sink to the level of your standards.", "Raise Your Daily Standards #Shorts", "Protect your daily non-negotiables with iron commitment and unwavering resolve."),
            ("Discipline weighs ounces, but the weight of regret weighs tons.", "Ounces of Discipline #Shorts", "Embrace the daily strain today to avoid the bitter sorrow of tomorrow."),
        ],
        "consistency": [
            ("Small, uncelebrated daily habits compound into extraordinary life breakthroughs.", "The Power of Compounding Habits #Shorts", "Success is rarely an explosion; it is an unrelenting drip of daily discipline."),
            ("Consistency will consistently outperform talent whenever talent relies on mood.", "Consistency Beats Talent #Shorts", "Show up even when the crowd has not yet gathered and enthusiasm is low."),
            ("Master the mundane repetitions. That is where quiet greatness is forged.", "Quiet Repetitions #Shorts", "Great work is made of small actions performed without complaining or seeking applause."),
        ],
        "resilience": [
            ("The storm did not come to destroy you. It came to clear the path you were meant to walk.", "Stronger Than the Storm #Shorts", "Resilience is knowing that every setback is a training ground for your destiny."),
            ("Bend with the wind, but anchor your roots deeper into your purpose.", "Unyielding Roots #Shorts", "Adversity tests your grip on your highest vision. Stand firm through the gale."),
            ("Pain is information, not a dead end. Use it to reshape your strategy.", "Turn Pain Into Fuel #Shorts", "You are stronger than the circumstances you are walking through right now."),
        ],
        "success": [
            ("Success is not a destination to arrive at, but a state of mind that drives every hour.", "The Real Definition of Success #Shorts", "True achievement leaves lasting impact and inner peace. Build for longevity."),
            ("When you commit to excellence in the dark, the light will find you automatically.", "Excellence in the Shadows #Shorts", "Do the work where no cameras are watching and applause does not exist."),
        ],
        "mindfulness": [
            ("Silence is not empty; it is full of the answers your chaotic mind is looking for.", "The Power of Stillness #Shorts", "Pause before you react. Wisdom lives in the gap between trigger and response."),
            ("You cannot control the waves, but you can always decide how you stand on your board.", "Mastering the Inner Ocean #Shorts", "Peace of mind is the greatest luxury you can cultivate in a noisy modern world."),
        ],
        "productivity": [
            ("Being busy is a distraction. Being effective is a calculated art form.", "Stop Being Busy, Be Effective #Shorts", "Ruthlessly eliminate low-value tasks to make room for your masterwork."),
            ("One deep, focused hour is worth eight hours of fractured multitasking.", "The Sacred Focus Hour #Shorts", "Protect your attention like the precious asset it is. Unplug and execute."),
        ],
        "confidence": [
            ("Confidence does not come from always being right, but from not fearing to be wrong.", "Unshakable Self-Trust #Shorts", "True confidence is self-trust built upon hundreds of kept promises to yourself."),
            ("Walk into the room believing you have something valuable to contribute, not to prove.", "Quiet Confidence #Shorts", "Stop asking for permission to occupy space. Own your journey."),
        ],
        "self-improvement": [
            ("Compare yourself to who you were yesterday, not to who someone else is today.", "Your Only True Competition #Shorts", "Self-improvement is a private race where personal progress is the only metric that matters."),
            ("Upgrade your habits and your identity will follow automatically.", "Identity-Based Evolution #Shorts", "You become what you repeatedly practice every day."),
        ],
        "life lessons": [
            ("Life will test you before it crowns you. Pass the test in silence.", "The Silent Apprenticeship #Shorts", "Every hardship is a prerequisite exam for the level you are heading toward."),
        ],
        "relationships": [
            ("Surround yourself with people who discuss ideas and growth, not other people.", "Curate Your Circle #Shorts", "Your environment dictates your trajectory. Choose companions who elevate your standards."),
        ],
        "personal growth": [
            ("Growth is uncomfortable because you have never been to the place it is taking you.", "The Discomfort of Growth #Shorts", "Lean into the stretch. Expansion requires leaving familiar ground behind."),
        ]
    }

    async def generate_thought(
        self,
        theme: str,
        tone: str = "inspiring",
        language: str = "en",
        custom_prompt: str = ""
    ) -> StructuredThought:
        theme_clean = theme.lower().strip()
        theme_key = theme_clean if theme_clean in self.CATALOG else "motivation"
        items = self.CATALOG[theme_key]
        thought_text, title, desc = random.choice(items)

        # Dynamic variation if custom prompt or seed is given
        if custom_prompt:
            thought_text = f"{thought_text} Focus: {custom_prompt.strip()}."

        # Simulate minimal token usage
        token_usage = TokenUsage(
            prompt_tokens=45,
            completion_tokens=65,
            total_tokens=110,
            estimated_cost_usd=0.0
        )

        return StructuredThought(
            theme=theme_clean,
            thought=thought_text,
            title=title,
            description=f"{desc}\n\nSubscribe to ThoughtFlow for your daily mindset shift.\n#shorts #{theme_clean} #mindset",
            hashtags=["shorts", theme_clean, "motivation", "wisdom", "mindset"],
            narration_script=thought_text,
            language=language,
            model_name="mock-provider-v1",
            prompt_version=PROMPT_VERSION,
            token_usage=token_usage
        )


class GeminiAIProvider(BaseAIProvider):
    """Google Gemini AI Provider with structured JSON output and bounded retries"""
    def __init__(self, api_key: str, model_name: str = "gemini-1.5-flash"):
        self.api_key = api_key
        self.model_name = model_name
        self.endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"

    async def generate_thought(
        self,
        theme: str,
        tone: str = "inspiring",
        language: str = "en",
        custom_prompt: str = ""
    ) -> StructuredThought:
        prompt_text = build_thought_generation_prompt(theme, tone, language, custom_prompt)
        payload = {
            "contents": [{
                "parts": [{"text": f"{SYSTEM_INSTRUCTION}\n\n{prompt_text}"}]
            }],
            "generationConfig": {
                "response_mime_type": "application/json",
                "temperature": 0.8,
                "maxOutputTokens": 800
            }
        }

        # Bounded retry loop (max 3 attempts with exponential backoff)
        max_retries = 3
        last_error = None

        for attempt in range(1, max_retries + 1):
            try:
                async with httpx.AsyncClient(timeout=30.0) as client:
                    resp = await client.post(self.endpoint, json=payload)

                    if resp.status_code == 429:
                        raise AIRateLimitError("Google Gemini rate limit exceeded (HTTP 429).")
                    if resp.status_code != 200:
                        raise AIProviderError(f"Gemini API returned error {resp.status_code}: {resp.text}")

                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if not candidates:
                        raise AIMalformedResponseError("Gemini returned empty candidates list.")

                    raw_json = candidates[0]["content"]["parts"][0]["text"]
                    parsed = json.loads(raw_json)

                    usage_meta = data.get("usageMetadata", {})
                    token_usage = TokenUsage(
                        prompt_tokens=usage_meta.get("promptTokenCount", 0),
                        completion_tokens=usage_meta.get("candidatesTokenCount", 0),
                        total_tokens=usage_meta.get("totalTokenCount", 0),
                        estimated_cost_usd=0.0  # Free tier
                    )

                    return StructuredThought(
                        theme=parsed.get("theme", theme),
                        thought=parsed["thought"],
                        title=parsed.get("title", f"{theme.capitalize()} Thought #Shorts"),
                        description=parsed.get("description", "Daily motivational short from ThoughtFlow AI."),
                        hashtags=parsed.get("hashtags", ["shorts", theme]),
                        narration_script=parsed.get("narration_script", parsed["thought"]),
                        language=parsed.get("language", language),
                        model_name=self.model_name,
                        prompt_version=PROMPT_VERSION,
                        token_usage=token_usage
                    )
            except (AIRateLimitError, httpx.RequestError) as e:
                last_error = e
                logger.warning(f"Gemini provider attempt {attempt}/{max_retries} failed: {e}. Retrying in {attempt * 2}s...")
                await asyncio.sleep(attempt * 2)
            except json.JSONDecodeError as e:
                raise AIMalformedResponseError(f"Failed to parse Gemini output as JSON: {e}")
            except Exception as e:
                raise AIProviderError(f"Unexpected error in Gemini provider: {e}")

        raise AIProviderError(f"Gemini API failed after {max_retries} attempts: {last_error}")


class OpenAIAIProvider(BaseAIProvider):
    """OpenAI Chat Completions Provider with JSON mode and bounded retries"""
    def __init__(self, api_key: str, model_name: str = "gpt-4o-mini"):
        self.api_key = api_key
        self.model_name = model_name
        self.endpoint = "https://api.openai.com/v1/chat/completions"

    async def generate_thought(
        self,
        theme: str,
        tone: str = "inspiring",
        language: str = "en",
        custom_prompt: str = ""
    ) -> StructuredThought:
        prompt_text = build_thought_generation_prompt(theme, tone, language, custom_prompt)
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": self.model_name,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": SYSTEM_INSTRUCTION},
                {"role": "user", "content": prompt_text}
            ],
            "temperature": 0.8
        }

        max_retries = 3
        last_error = None

        for attempt in range(1, max_retries + 1):
            try:
                async with httpx.AsyncClient(timeout=30.0) as client:
                    resp = await client.post(self.endpoint, headers=headers, json=payload)
                    if resp.status_code == 429:
                        raise AIRateLimitError("OpenAI rate limit exceeded (HTTP 429).")
                    if resp.status_code != 200:
                        raise AIProviderError(f"OpenAI API returned error {resp.status_code}: {resp.text}")

                    data = resp.json()
                    raw_json = data["choices"][0]["message"]["content"]
                    parsed = json.loads(raw_json)

                    usage = data.get("usage", {})
                    token_usage = TokenUsage(
                        prompt_tokens=usage.get("prompt_tokens", 0),
                        completion_tokens=usage.get("completion_tokens", 0),
                        total_tokens=usage.get("total_tokens", 0),
                        estimated_cost_usd=round((usage.get("prompt_tokens", 0) * 0.00015 / 1000) + (usage.get("completion_tokens", 0) * 0.0006 / 1000), 6)
                    )

                    return StructuredThought(
                        theme=parsed.get("theme", theme),
                        thought=parsed["thought"],
                        title=parsed.get("title", f"{theme.capitalize()} Thought #Shorts"),
                        description=parsed.get("description", "Daily motivational short from ThoughtFlow AI."),
                        hashtags=parsed.get("hashtags", ["shorts", theme]),
                        narration_script=parsed.get("narration_script", parsed["thought"]),
                        language=parsed.get("language", language),
                        model_name=self.model_name,
                        prompt_version=PROMPT_VERSION,
                        token_usage=token_usage
                    )
            except (AIRateLimitError, httpx.RequestError) as e:
                last_error = e
                logger.warning(f"OpenAI provider attempt {attempt}/{max_retries} failed: {e}. Retrying in {attempt * 2}s...")
                await asyncio.sleep(attempt * 2)
            except json.JSONDecodeError as e:
                raise AIMalformedResponseError(f"Failed to parse OpenAI output as JSON: {e}")
            except Exception as e:
                raise AIProviderError(f"Unexpected error in OpenAI provider: {e}")

        raise AIProviderError(f"OpenAI API failed after {max_retries} attempts: {last_error}")


def get_ai_provider(provider_type: Optional[str] = None) -> BaseAIProvider:
    provider = (provider_type or settings.AI_PROVIDER).lower()
    if provider == "gemini" and settings.GEMINI_API_KEY:
        return GeminiAIProvider(settings.GEMINI_API_KEY, settings.AI_MODEL_NAME)
    elif provider in ("openai", "chatgpt") and settings.OPENAI_API_KEY:
        return OpenAIAIProvider(settings.OPENAI_API_KEY, settings.AI_MODEL_NAME)
    return MockAIProvider()
