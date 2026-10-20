import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

import asyncio
import time
from app.services.ai_provider import MockAIProvider

print(f"[{time.time()}] Starting test...")
provider = MockAIProvider()
themes = ["motivation", "discipline", "consistency", "resilience"]
for theme in themes:
    print(f"[{time.time()}] Generating for {theme}...")
    t = asyncio.run(provider.generate_thought(theme=theme))
    print(f"[{time.time()}] Generated {theme}: {t.thought[:30]}...")

print(f"[{time.time()}] ALL DONE!")
