from fastapi import APIRouter, Depends, HTTPException
from services.tts import tts_service
from services.database import neon_db
from services.rate_limit import public_rate_limited
from pydantic import BaseModel

router = APIRouter(prefix="/api/tts", tags=["TTS"])

class TTSRequest(BaseModel):
    text: str
    personality: str = "neutral"
    lang: str = "en"

@router.post("/synthesize")
async def synthesize_text(request: TTSRequest, _rate_limit: None = Depends(public_rate_limited("tts"))):
    try:
        result = await tts_service.synthesize(
            text=request.text,
            personality=request.personality,
            lang=request.lang
        )
    except Exception as exc:
        print(f"[TTS] Synthesis failed: {exc}")
        await neon_db.track_event({
            "user_id": "anonymous",
            "name": "api_tts_failed",
            "properties": {
                "chars": len(request.text),
                "lang": request.lang,
                "personality": request.personality,
                "error": str(exc)[:300],
            },
        })
        raise HTTPException(status_code=503, detail="Voice synthesis is temporarily unavailable.")
    await neon_db.track_event({
        "user_id": "anonymous",
        "name": "api_tts_synthesized",
        "properties": {
            "chars": len(request.text),
            "lang": request.lang,
            "personality": request.personality,
            "engine": result.get("engine"),
            "voice": result.get("voice"),
            "storage": result.get("storage"),
        },
    })
    return result
