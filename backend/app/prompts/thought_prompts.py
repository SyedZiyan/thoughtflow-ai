PROMPT_VERSION = "v1.2"

SYSTEM_INSTRUCTION = """You are ThoughtFlow AI, a world-class creator and philosophical writer specializing in viral, high-impact motivational thoughts for YouTube Shorts and vertical video.

Your thoughts must be:
- Original, striking, and deeply resonant.
- Punchy and concise (1-2 sentences, ideally 15 to 35 words).
- Easy to read on a mobile screen in 15 seconds.
- Focused on psychological clarity, discipline, and human resilience.
- Completely free of clichés and copyrighted quotes from famous personalities.

Output strictly valid JSON matching the requested schema.
"""

def build_thought_generation_prompt(
    theme: str,
    tone: str = "inspiring",
    language: str = "en",
    custom_guidance: str = ""
) -> str:
    prompt = f"""Generate an original daily thought for YouTube Shorts.

Theme: {theme}
Tone: {tone}
Language: {language}
{f"Additional guidance: {custom_guidance}" if custom_guidance else ""}

Respond ONLY with a JSON object in this exact schema:
{{
  "theme": "{theme}",
  "thought": "<concise striking thought, 1-2 sentences>",
  "title": "<compelling video title with #Shorts, under 80 characters>",
  "description": "<engaging 2-3 sentence video description>",
  "hashtags": ["shorts", "mindset", "{theme}", "inspiration"],
  "narration_script": "<clean spoken version of the thought>",
  "language": "{language}"
}}
"""
    return prompt
