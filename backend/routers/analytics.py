from datetime import datetime, timezone
from typing import Any, Dict, Optional
from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field
from middleware.auth import get_current_user
from services.database import neon_db
from services.rate_limit import rate_limited, public_rate_limited, rate_limiter, RULES

router = APIRouter(prefix="/api/analytics", tags=["Analytics"])


class AnalyticsEvent(BaseModel):
    name: str = Field(min_length=2, max_length=80)
    properties: Dict[str, Any] = Field(default_factory=dict)
    session_id: Optional[str] = None
    occurred_at: Optional[datetime] = None


class CrashEvent(BaseModel):
    name: str = Field(default="app_crash", min_length=2, max_length=80)
    message: str = Field(default="", max_length=500)
    stack: Optional[str] = Field(default=None, max_length=12000)
    fatal: bool = False
    properties: Dict[str, Any] = Field(default_factory=dict)
    session_id: Optional[str] = None
    occurred_at: Optional[datetime] = None


@router.post("/events", status_code=202)
async def track_event(
    event: AnalyticsEvent,
    current_user: dict = Depends(rate_limited("analytics")),
):
    payload = {
        "user_id": str(current_user["id"]),
        "name": event.name,
        "session_id": event.session_id,
        "properties": event.properties,
        "occurred_at": event.occurred_at or datetime.now(timezone.utc),
    }
    await neon_db.track_event(payload)
    return {"status": "accepted"}


@router.post("/crashes", status_code=202)
async def track_crash(
    event: CrashEvent,
    request: Request,
    _: None = Depends(public_rate_limited("analytics")),
):
    """
    Accept client crash reports even when auth bootstrapping is broken.
    This keeps beta diagnostics available for splash/auth/render failures.
    """
    device_id = request.headers.get("X-Device-Identity")
    user_agent = request.headers.get("User-Agent")
    payload = {
        "user_id": None,
        "name": event.name,
        "session_id": event.session_id,
        "properties": {
            "message": event.message,
            "stack": event.stack,
            "fatal": event.fatal,
            "device_id": device_id,
            "user_agent": user_agent,
            **event.properties,
        },
        "occurred_at": event.occurred_at or datetime.now(timezone.utc),
    }
    await neon_db.track_event(payload)
    return {"status": "accepted"}


@router.get("/operational-health", status_code=200)
async def operational_health(current_user: dict = Depends(get_current_user)):
    metrics = await neon_db.get_operational_metrics(hours=24)
    limiter_stats = rate_limiter.stats() if hasattr(rate_limiter, "stats") else {"backend": "unknown"}
    return {
        "status": "ok",
        "window_hours": 24,
        "metrics": metrics,
        "rate_limits": {
            "rules": {
                name: {
                    "max_requests": rule.max_requests,
                    "window_seconds": rule.window_seconds,
                }
                for name, rule in RULES.items()
            },
            "stats": limiter_stats,
        },
        "cost_controls": {
            "ai_calls_24h": metrics.get("estimated_ai_calls", 0),
            "tts_chars_24h": metrics.get("estimated_tts_chars", 0),
            "note": "Use provider dashboards for exact Groq/Gemini billing; this endpoint tracks app-side usage volume.",
        },
    }
