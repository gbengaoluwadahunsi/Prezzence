from datetime import datetime, timezone
from typing import Any, Dict, Optional
from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field
from middleware.auth import get_current_user
from services.database import neon_db
from services.entitlements import has_unlimited_access
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


@router.get("/urgency-conversion", status_code=200)
async def urgency_conversion(window_days: int = 90, current_user: dict = Depends(get_current_user)):
    """Admin-only: is Prezzence a painkiller? Conversion to premium by interview-urgency bucket.

    If 'today'/'this_week' users convert far above 'exploring', the acute-moment offer is working.
    """
    if not has_unlimited_access(current_user.get("email")):
        raise HTTPException(status_code=403, detail="Admin access required")
    window = max(1, min(int(window_days or 90), 365))
    buckets = await neon_db.get_urgency_conversion(window_days=window)
    urgent = [b for b in buckets if b["bucket"] in ("today", "this_week")]
    urgent_users = sum(b["users"] for b in urgent)
    urgent_premium = sum(b["premium_users"] for b in urgent)
    return {
        "window_days": window,
        "buckets": buckets,
        "urgent_summary": {
            "users": urgent_users,
            "premium_users": urgent_premium,
            "conversion_rate": round(urgent_premium / urgent_users, 4) if urgent_users else 0.0,
        },
    }
