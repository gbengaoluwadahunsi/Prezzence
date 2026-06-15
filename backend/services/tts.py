import os
import edge_tts
import uuid
import time
import wave
from services.supabase import db
from typing import Dict, List

try:
    import miniaudio
except Exception:
    miniaudio = None

# Local directory for temporary TTS files
TEMP_TTS_DIR = "static/tts"
os.makedirs(TEMP_TTS_DIR, exist_ok=True)
TTS_MAX_FILE_AGE_SECONDS = int(os.getenv("TTS_MAX_FILE_AGE_SECONDS", "86400"))
TTS_CLEANUP_INTERVAL_SECONDS = int(os.getenv("TTS_CLEANUP_INTERVAL_SECONDS", "900"))
TTS_STORAGE_BUCKET = os.getenv("TTS_STORAGE_BUCKET", "tts")
TTS_AUDIO_FORMAT = os.getenv("TTS_AUDIO_FORMAT", "wav").lower()

class TTSService:
    def __init__(self):
        self._last_cleanup = 0.0
        self.storage_backend = os.getenv("TTS_STORAGE_BACKEND", "local").lower()
        environment = os.getenv("ENVIRONMENT", os.getenv("APP_ENV", "development")).lower()
        if environment in {"production", "prod"} and self.storage_backend == "local":
            raise RuntimeError("TTS_STORAGE_BACKEND must be object storage in production, not local static/tts.")
        self.default_voice = os.getenv("TTS_DEFAULT_VOICE", "en-US-AvaNeural")

        # Default English voices by interviewer persona.
        self.voice_map = {
            "friendly": os.getenv("TTS_VOICE_FRIENDLY", "en-US-AvaNeural"),
            "tough": os.getenv("TTS_VOICE_TOUGH", "en-US-AndrewNeural"),
            "neutral": os.getenv("TTS_VOICE_NEUTRAL", "en-US-EmmaNeural"),
            "technical": os.getenv("TTS_VOICE_TECHNICAL", "en-US-BrianNeural"),
            "executive": os.getenv("TTS_VOICE_EXECUTIVE", "en-US-AndrewNeural"),
        }
        self.voice_style_map = {
            "friendly": {"rate": "-5%", "pitch": "+3Hz"},
            "tough": {"rate": "-2%", "pitch": "-2Hz"},
            "neutral": {"rate": "-4%", "pitch": "+1Hz"},
            "technical": {"rate": "-3%", "pitch": "-1Hz"},
            "executive": {"rate": "-5%", "pitch": "-3Hz"},
        }
        print(f"[TTS] Edge styles loaded: neutral_rate={self.voice_style_map['neutral']['rate']}")

        # Language-specific voices keep the product consistent outside English.
        # Values are (friendly, tough, neutral) where available.
        self.lang_voice_map = {
            "ar": ("ar-SA-ZariyahNeural", "ar-SA-HamedNeural", "ar-SA-ZariyahNeural"),
            "de": ("de-DE-KatjaNeural", "de-DE-ConradNeural", "de-DE-KatjaNeural"),
            "en": (self.voice_map["friendly"], self.voice_map["tough"], self.voice_map["neutral"]),
            "en-gb": ("en-GB-SoniaNeural", "en-GB-RyanNeural", "en-GB-LibbyNeural"),
            "es": ("es-ES-ElviraNeural", "es-ES-AlvaroNeural", "es-ES-ElviraNeural"),
            "fr": ("fr-FR-DeniseNeural", "fr-FR-HenriNeural", "fr-FR-DeniseNeural"),
            "hi": ("hi-IN-SwaraNeural", "hi-IN-MadhurNeural", "hi-IN-SwaraNeural"),
            "it": ("it-IT-ElsaNeural", "it-IT-DiegoNeural", "it-IT-IsabellaNeural"),
            "ja": ("ja-JP-NanamiNeural", "ja-JP-KeitaNeural", "ja-JP-NanamiNeural"),
            "ko": ("ko-KR-SunHiNeural", "ko-KR-InJoonNeural", "ko-KR-SunHiNeural"),
            "pt": ("pt-BR-FranciscaNeural", "pt-BR-AntonioNeural", "pt-BR-FranciscaNeural"),
            "zh": ("zh-CN-XiaoxiaoNeural", "zh-CN-YunxiNeural", "zh-CN-XiaoxiaoNeural"),
        }

    def ensure_storage_ready(self) -> bool:
        if self.storage_backend != "supabase":
            return True
        return db.ensure_storage_bucket(TTS_STORAGE_BUCKET, public=True)

    async def synthesize(self, text: str, personality: str = "neutral", lang: str = "en"):
        personality = self._normalize_personality(personality)
        lang = self._normalize_lang(lang)
        voice = self._select_voice(personality, lang)
        self._cleanup_stale_files()

        audio_id = str(uuid.uuid4())
        mp3_file_name = f"{audio_id}.mp3"
        mp3_file_path = os.path.join(TEMP_TTS_DIR, mp3_file_name)

        style = self.voice_style_map.get(personality, self.voice_style_map["neutral"])
        visemes: List[Dict] = []
        used_voice = voice
        voice_attempts = []
        for candidate in (
            voice,
            self.default_voice,
            self.voice_map.get("neutral"),
            "en-US-AriaNeural",
            "en-US-JennyNeural",
        ):
            if candidate and candidate not in voice_attempts:
                voice_attempts.append(candidate)

        last_error = None
        for candidate in voice_attempts:
            try:
                visemes.clear()
                used_voice = candidate
                candidate_personality = personality if candidate == voice else "neutral"
                candidate_style = style if candidate == voice else self.voice_style_map["neutral"]
                await self._stream_to_file(text, candidate, candidate_style, mp3_file_path, visemes, candidate_personality)
                last_error = None
                break
            except Exception as exc:
                last_error = exc
                print(f"[TTS] Voice {candidate} failed; trying fallback. {exc}")

        if last_error is not None:
            raise last_error

        file_name = mp3_file_name
        file_path = mp3_file_path
        content_type = "audio/mpeg"
        audio_format = "mp3"

        if TTS_AUDIO_FORMAT == "wav":
            wav_file_name = f"{audio_id}.wav"
            wav_file_path = os.path.join(TEMP_TTS_DIR, wav_file_name)
            try:
                self._convert_mp3_to_wav(mp3_file_path, wav_file_path)
                file_name = wav_file_name
                file_path = wav_file_path
                content_type = "audio/x-wav"  # Supabase requires audio/x-wav instead of audio/wav
                audio_format = "wav"
            except Exception as exc:
                print(f"[TTS] WAV conversion failed; falling back to MP3. {exc}")

        audio_url = f"/static/tts/{file_name}"
        storage = "local"
        if self.storage_backend == "supabase":
            self.ensure_storage_ready()
            uploaded_url = self._upload_to_supabase(file_path, file_name, content_type)
            if uploaded_url:
                audio_url = uploaded_url
                storage = "supabase"

        return {
            "audio_url": audio_url,
            "visemes": visemes,
            "engine": "edge-tts",
            "voice": used_voice,
            "format": audio_format,
            "storage": storage,
        }

    def _convert_mp3_to_wav(self, mp3_path: str, wav_path: str):
        if miniaudio is None:
            raise RuntimeError("miniaudio is not installed; cannot convert TTS MP3 to WAV.")

        decoded = miniaudio.decode_file(
            mp3_path,
            output_format=miniaudio.SampleFormat.SIGNED16,
            nchannels=1,
            sample_rate=16000,
        )
        samples = decoded.samples
        if hasattr(samples, "tobytes"):
            pcm_bytes = samples.tobytes()
        elif isinstance(samples, (bytes, bytearray)):
            pcm_bytes = bytes(samples)
        else:
            pcm_bytes = bytes(samples)

        with wave.open(wav_path, "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(16000)
            wav.writeframes(pcm_bytes)

    async def _stream_to_file(self, text: str, voice: str, style: Dict, file_path: str, visemes: List[Dict], personality: str):
        communicate = edge_tts.Communicate(
            text,
            voice,
            rate=self._normalize_edge_rate(style.get("rate")),
            pitch=self._normalize_edge_pitch(style.get("pitch")),
            boundary="WordBoundary",
        )
        with open(file_path, "wb") as audio:
            async for message in communicate.stream():
                if message["type"] == "audio":
                    audio.write(message["data"])
                elif message["type"] == "WordBoundary":
                    word = message.get("text", "")
                    start_ms = int(message.get("offset", 0) / 10000)
                    duration_ms = max(90, int(message.get("duration", 0) / 10000))
                    visemes.append({
                        "word": word,
                        "startMs": start_ms,
                        "durationMs": duration_ms,
                        "shape": self._word_to_viseme(word),
                        "intensity": self._word_intensity(word, personality),
                    })

    def _select_voice(self, personality: str, lang: str) -> str:
        voices = self.lang_voice_map.get(lang) or self.lang_voice_map.get(lang.split("-")[0])
        if voices:
            index = 0 if personality == "friendly" else 1 if personality == "tough" else 2
            return voices[index]
        return self.voice_map.get(personality, self.default_voice)

    def _normalize_lang(self, lang: str) -> str:
        return (lang or "en").strip().lower().replace("_", "-")

    def _normalize_personality(self, personality: str) -> str:
        value = (personality or "neutral").strip().lower()
        if value in self.voice_style_map:
            return value
        if value in {"amina", "sophia", "lily"}:
            return "friendly"
        if value in {"maya", "sofia"}:
            return "neutral"
        if value in {"jonas", "oliver"}:
            return "tough"
        if value in {"strict", "challenging", "skeptical"}:
            return "tough"
        if value in {"warm", "supportive", "encouraging"}:
            return "friendly"
        return "neutral"

    def _normalize_edge_rate(self, rate: str | None) -> str:
        value = str(rate or "+0%").strip()
        if value in {"0", "0%", "+0", "-0", "-0%"}:
            return "+0%"
        if value.endswith("%") and not value.startswith(("+", "-")):
            return f"+{value}"
        return value

    def _normalize_edge_pitch(self, pitch: str | None) -> str:
        value = str(pitch or "+0Hz").strip()
        if value in {"0", "0Hz", "+0", "-0", "-0Hz"}:
            return "+0Hz"
        if value.endswith("Hz") and not value.startswith(("+", "-")):
            return f"+{value}"
        return value

    def _cleanup_stale_files(self):
        now = time.time()
        if now - self._last_cleanup < TTS_CLEANUP_INTERVAL_SECONDS:
            return

        self._last_cleanup = now
        cutoff = now - TTS_MAX_FILE_AGE_SECONDS
        try:
            for entry in os.scandir(TEMP_TTS_DIR):
                if entry.is_file() and entry.name.endswith((".mp3", ".wav")) and entry.stat().st_mtime < cutoff:
                    os.remove(entry.path)
        except Exception as exc:
            print(f"[TTS] Cleanup warning: {exc}")

    def _upload_to_supabase(self, file_path: str, file_name: str, content_type: str) -> str | None:
        if not db.client:
            print("[TTS] Supabase storage requested but client is unavailable; using local static file.")
            return None

        storage_path = f"tts/{file_name}"
        try:
            with open(file_path, "rb") as audio:
                db.client.storage.from_(TTS_STORAGE_BUCKET).upload(
                    storage_path,
                    audio,
                    {"content-type": content_type, "upsert": "true"},
                )
            return db.client.storage.from_(TTS_STORAGE_BUCKET).get_public_url(storage_path)
        except Exception as exc:
            print(f"[TTS] Supabase upload failed; using local static file. {exc}")
            return None

    def _word_to_viseme(self, word: str) -> str:
        clean = "".join(ch.lower() for ch in word if ch.isalpha())
        if not clean:
            return "jawOpen"
        if any(ch in clean for ch in ("o", "u", "w")):
            return "viseme_O"
        if any(ch in clean for ch in ("f", "v")):
            return "viseme_FF"
        if any(ch in clean for ch in ("a", "e", "i", "y")):
            return "viseme_aa"
        return "jawOpen"

    def _word_intensity(self, word: str, personality: str) -> float:
        base = 0.42 if len(word) <= 3 else 0.58 if len(word) <= 7 else 0.72
        if personality == "tough":
            base += 0.08
        elif personality == "friendly":
            base -= 0.04
        return round(min(0.88, max(0.32, base)), 2)

tts_service = TTSService()
