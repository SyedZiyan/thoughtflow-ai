import abc
import os
import math
import wave
import struct
import logging
from pathlib import Path
from typing import Optional, List, Dict, Any
import httpx
from app.core.config import settings

logger = logging.getLogger("thoughtflow.audio")

class BaseTTSProvider(abc.ABC):
    """Abstract interface for replaceable Text-To-Speech providers"""

    @abc.abstractmethod
    async def synthesize(self, text: str, output_path: str, voice: Optional[str] = None) -> str:
        """Synthesizes text to speech and saves to output_path. Returns output_path."""
        pass


class EdgeTTSProvider(BaseTTSProvider):
    """
    High quality free neural TTS powered by edge-tts (Microsoft Speech).
    Zero API keys required, production quality intonation.
    """
    DEFAULT_VOICE = "en-US-ChristopherNeural"
    SUPPORTED_VOICES = [
        {"id": "en-US-ChristopherNeural", "name": "Christopher (Motivating, Male)", "gender": "male", "lang": "en"},
        {"id": "en-US-JennyNeural", "name": "Jenny (Warm & Inspiring, Female)", "gender": "female", "lang": "en"},
        {"id": "en-US-GuyNeural", "name": "Guy (Conversational, Male)", "gender": "male", "lang": "en"},
        {"id": "en-GB-SoniaNeural", "name": "Sonia (Reflective British, Female)", "gender": "female", "lang": "en-GB"},
        {"id": "en-AU-WilliamNeural", "name": "William (Calm Australian, Male)", "gender": "male", "lang": "en-AU"},
    ]

    async def synthesize(self, text: str, output_path: str, voice: Optional[str] = None) -> str:
        import edge_tts

        selected_voice = voice or self.DEFAULT_VOICE
        out_file = Path(output_path)
        out_file.parent.mkdir(parents=True, exist_ok=True)

        logger.info(f"Synthesizing speech via EdgeTTS (voice: {selected_voice}): \"{text[:50]}...\"")
        communicate = edge_tts.Communicate(text, selected_voice)
        await communicate.save(str(out_file))

        if not out_file.exists() or out_file.stat().st_size == 0:
            raise RuntimeError(f"EdgeTTS failed to generate valid audio file at {output_path}")

        logger.info(f"EdgeTTS successfully generated audio: {output_path} ({out_file.stat().st_size} bytes)")
        return str(out_file)


class ElevenLabsTTSProvider(BaseTTSProvider):
    """ElevenLabs voice synthesis for premium paid studio audio"""

    def __init__(self, api_key: str, default_voice_id: str = "21m00Tcm4TlvDq8ikWAM"):
        self.api_key = api_key
        self.default_voice_id = default_voice_id

    async def synthesize(self, text: str, output_path: str, voice: Optional[str] = None) -> str:
        voice_id = voice or self.default_voice_id
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
        headers = {
            "Accept": "audio/mpeg",
            "Content-Type": "application/json",
            "xi-api-key": self.api_key
        }
        payload = {
            "text": text,
            "model_id": "eleven_monolingual_v1",
            "voice_settings": {
                "stability": 0.5,
                "similarity_boost": 0.8
            }
        }

        async with httpx.AsyncClient(timeout=60.0) as client:
            resp = await client.post(url, headers=headers, json=payload)
            if resp.status_code != 200:
                raise RuntimeError(f"ElevenLabs TTS failed: {resp.status_code} - {resp.text}")

            out_file = Path(output_path)
            out_file.parent.mkdir(parents=True, exist_ok=True)
            with open(out_file, "wb") as f:
                f.write(resp.content)

        return str(out_file)


class AmbientAudioSynthesizer:
    """
    Pure Python standard-library ambient harmonic chord synthesizer.
    Requires zero external C/pip audio dependencies.
    Produces soothing, cinematic ambient drone chords with soft tremolo and stereo widening.
    """

    @staticmethod
    def generate_ambient_track(
        output_wav_path: str,
        duration_seconds: int = 15,
        sample_rate: int = 44100
    ) -> str:
        """
        Synthesizes a 16-bit stereo WAV ambient meditation pad:
        - Frequencies: Root A2 (110Hz), E3 (164.8Hz), A3 (220Hz), C#4 (277.18Hz), E4 (329.63Hz)
        - Slow low-frequency oscillator (LFO) for breathing swell
        - Linear fade-in (1.5s) and fade-out (2.0s)
        """
        out_path = Path(output_wav_path)
        out_path.parent.mkdir(parents=True, exist_ok=True)

        num_samples = int(duration_seconds * sample_rate)
        fade_in_samples = int(1.5 * sample_rate)
        fade_out_samples = int(2.0 * sample_rate)

        # Harmonic series frequencies (warm cinematic A major 9th chord)
        notes = [
            (110.0, 0.28),   # A2 root (sub warm bass)
            (164.81, 0.22),  # E3 fifth
            (220.0, 0.25),   # A3 octave
            (277.18, 0.20),  # C#4 major third
            (329.63, 0.18),  # E4 octave fifth
            (493.88, 0.10)   # B4 ninth
        ]

        with wave.open(str(out_path), "w") as wav_file:
            # 2 channels (stereo), 2 bytes per sample (16-bit), sample_rate
            wav_file.setnchannels(2)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)

            # Generate in chunks for memory efficiency
            chunk_size = 4096
            frames = bytearray()

            for i in range(num_samples):
                t = i / sample_rate

                # Envelope: Fade in and Fade out
                env = 1.0
                if i < fade_in_samples:
                    env = i / fade_in_samples
                elif i > (num_samples - fade_out_samples):
                    env = (num_samples - i) / fade_out_samples

                # LFO for organic breathing pulsation (0.2 Hz)
                lfo = 0.85 + 0.15 * math.sin(2 * math.pi * 0.2 * t)

                # Sum sinusoids
                sample_l = 0.0
                sample_r = 0.0

                for idx, (freq, amp) in enumerate(notes):
                    # Slight detuning for analog lush warmth
                    detune_l = freq * (1.0 - 0.001 * (idx % 3))
                    detune_r = freq * (1.0 + 0.001 * (idx % 3))

                    sample_l += amp * math.sin(2 * math.pi * detune_l * t)
                    sample_r += amp * math.sin(2 * math.pi * detune_r * t)

                val_l = int(sample_l * env * lfo * 14000)
                val_r = int(sample_r * env * lfo * 14000)

                # Clamp to 16-bit signed integer [-32768, 32767]
                val_l = max(-32767, min(32767, val_l))
                val_r = max(-32767, min(32767, val_r))

                frames.extend(struct.pack("<hh", val_l, val_r))

                if len(frames) >= chunk_size:
                    wav_file.writeframes(frames)
                    frames = bytearray()

            if frames:
                wav_file.writeframes(frames)

        logger.info(f"Generated synthetic ambient track: {output_wav_path} ({duration_seconds}s)")
        return str(out_path)


def get_tts_provider() -> BaseTTSProvider:
    """Factory for selecting TTS provider based on environment configuration"""
    if settings.TTS_PROVIDER == "elevenlabs" and settings.ELEVENLABS_API_KEY:
        return ElevenLabsTTSProvider(
            api_key=settings.ELEVENLABS_API_KEY,
            default_voice_id=settings.TTS_VOICE_ID or "21m00Tcm4TlvDq8ikWAM"
        )
    return EdgeTTSProvider()
