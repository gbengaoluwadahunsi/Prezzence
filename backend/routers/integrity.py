"""Play Integrity verification endpoint.

The Android client attaches a Play Integrity token to attest the app/device. By default
this is ADVISORY: the verdict is recorded but legitimate users are never blocked (devices
without Play services, transient failures, etc.). Set PLAY_INTEGRITY_ENFORCED=true to
return 403 when a configured verification reports an untrusted app/device.
"""

import os

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from middleware.auth import get_current_user
from services.database import neon_db
from services.play_integrity import play_integrity

router = APIRouter(prefix="/api/integrity", tags=["Integrity"])

INTEGRITY_ENFORCED = os.getenv("PLAY_INTEGRITY_ENFORCED", "false").lower() == "true"


class IntegrityVerifyRequest(BaseModel):
    token: str
    action: str | None = None


@router.post("/verify", status_code=200)
async def verify_integrity(
    payload: IntegrityVerifyRequest,
    current_user: dict = Depends(get_current_user),
):
    token = (payload.token or "").strip()
    if len(token) < 20:
        raise HTTPException(status_code=400, detail="Invalid integrity token")

    if not play_integrity.configured():
        # Cannot verify without a service account; stay advisory and never block.
        return {
            "verified": False,
            "trusted": True,
            "reason": "verification_not_configured",
            "enforced": INTEGRITY_ENFORCED,
        }

    try:
        verdict = await play_integrity.decode_token(token)
    except ValueError as exc:
        if INTEGRITY_ENFORCED:
            raise HTTPException(status_code=403, detail="Integrity verification failed")
        return {
            "verified": False,
            "trusted": True,
            "reason": str(exc),
            "enforced": INTEGRITY_ENFORCED,
        }

    trusted = bool(verdict.get("trusted"))

    try:
        await neon_db.track_event({
            "name": "play_integrity_verdict",
            "properties": {
                "user_id": str(current_user.get("id")),
                "action": payload.action or "unspecified",
                "trusted": trusted,
                "app_verdict": verdict.get("app_verdict"),
                "device_verdicts": verdict.get("device_verdicts"),
                "licensing_verdict": verdict.get("licensing_verdict"),
            },
        })
    except Exception:
        pass

    if INTEGRITY_ENFORCED and not trusted:
        raise HTTPException(status_code=403, detail="Device or app failed integrity checks")

    return {
        "verified": True,
        "trusted": trusted,
        "app_verdict": verdict.get("app_verdict"),
        "device_verdicts": verdict.get("device_verdicts"),
        "licensing_verdict": verdict.get("licensing_verdict"),
        "enforced": INTEGRITY_ENFORCED,
    }
