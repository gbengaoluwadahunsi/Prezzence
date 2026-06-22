import os

from fastapi import HTTPException

from services.database import neon_db
from services.google_play_billing import google_play_billing


async def sync_verified_google_subscription(
    user_id: str,
    product_id: str,
    purchase_token: str,
    package_name: str | None = None,
    order_id: str | None = None,
) -> dict:
    if package_name and package_name.strip() and package_name.strip() != google_play_billing.package_name:
        raise HTTPException(status_code=400, detail="Package name does not match server configuration")

    if not google_play_billing.configured():
        if os.getenv("ENVIRONMENT", "development").lower() == "development":
            print("[Billing] Google Play verification skipped in development (service account not configured).")
            saved = await neon_db.upsert_play_entitlement(
                user_id=user_id,
                product_id=product_id,
                purchase_token=purchase_token,
                order_id=order_id,
                expires_at=None,
                is_premium=True,
                source="dev_sync",
            )
            if not saved:
                raise HTTPException(status_code=503, detail="Could not update entitlement")
            return {"is_premium": True, "plan": "premium", "synced": True, "verified": False}

        raise HTTPException(
            status_code=503,
            detail="Google Play billing verification is not configured on the server.",
        )

    try:
        verified = await google_play_billing.verify_subscription(product_id, purchase_token)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    if not verified.get("active"):
        await neon_db.revoke_play_entitlement(user_id)
        raise HTTPException(status_code=402, detail="Subscription is not active")

    saved = await neon_db.upsert_play_entitlement(
        user_id=user_id,
        product_id=verified.get("product_id") or product_id,
        purchase_token=purchase_token,
        order_id=verified.get("order_id") or order_id,
        expires_at=verified.get("expires_at"),
        is_premium=True,
        source="google_play",
    )
    if not saved:
        raise HTTPException(status_code=503, detail="Could not update entitlement")

    expires_at = verified.get("expires_at")
    return {
        "is_premium": True,
        "plan": "premium",
        "synced": True,
        "verified": True,
        "expires_at": expires_at.isoformat() if expires_at else None,
        "auto_renewing": verified.get("auto_renewing"),
    }
