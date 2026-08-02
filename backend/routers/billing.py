import base64
import json

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel

from core.production_config import WEEK_PASS_PRODUCT_ID
from middleware.auth import get_current_user
from services.billing_entitlements import (
    sync_verified_google_product,
    sync_verified_google_subscription,
)
from services.database import neon_db
from services.google_play_billing import google_play_billing

router = APIRouter(prefix="/api/billing", tags=["Billing"])

RTDN_GRANT_TYPES = {1, 2, 4, 7}
RTDN_REVOKE_TYPES = {3, 5, 12, 13}


@router.get("/google/status", status_code=200)
async def google_billing_status():
    """Shows whether server-side subscription verification is configured."""
    from core.production_config import PRODUCTION_PACKAGE, SUBSCRIPTION_PRODUCT_ID

    return {
        "configured": google_play_billing.configured(),
        "package_name": google_play_billing.package_name,
        "expected_package_name": PRODUCTION_PACKAGE,
        "subscription_product_id": SUBSCRIPTION_PRODUCT_ID,
        "package_matches_production": google_play_billing.package_name == PRODUCTION_PACKAGE,
    }


class EntitlementSyncRequest(BaseModel):
    purchase_token: str
    product_id: str = "prezzence_pro"
    package_name: str | None = None
    order_id: str | None = None
    # "subscription" (default) or "product" for the one-time week pass. The
    # server also infers "product" when product_id matches the week-pass SKU,
    # so older clients that only send product_id still route correctly.
    product_type: str | None = None


@router.post("/google/sync", status_code=200)
async def sync_google_subscription(
    payload: EntitlementSyncRequest,
    current_user: dict = Depends(get_current_user),
):
    token = payload.purchase_token.strip()
    product_id = payload.product_id.strip() or "prezzence_pro"
    if len(token) < 8:
        raise HTTPException(status_code=400, detail="Invalid purchase token")

    is_one_time = (payload.product_type or "").strip().lower() == "product" or product_id == WEEK_PASS_PRODUCT_ID
    if is_one_time:
        return await sync_verified_google_product(
            user_id=str(current_user["id"]),
            product_id=product_id,
            purchase_token=token,
            package_name=payload.package_name,
            order_id=payload.order_id,
        )
    return await sync_verified_google_subscription(
        user_id=str(current_user["id"]),
        product_id=product_id,
        purchase_token=token,
        package_name=payload.package_name,
        order_id=payload.order_id,
    )


@router.post("/google/rtdn", status_code=200)
async def google_rtdn_webhook(request: Request):
    body = await request.json()
    message = body.get("message") or {}
    data_b64 = message.get("data")
    if not data_b64:
        return {"status": "ignored"}

    try:
        payload = json.loads(base64.b64decode(data_b64).decode("utf-8"))
    except Exception:
        return {"status": "invalid_payload"}

    sub_note = payload.get("subscriptionNotification") or {}
    purchase_token = str(sub_note.get("purchaseToken") or "").strip()
    product_id = str(sub_note.get("subscriptionId") or "prezzence_pro").strip()
    notification_type = int(sub_note.get("notificationType") or 0)

    if not purchase_token:
        return {"status": "ignored"}

    user_id = await neon_db.get_user_id_by_purchase_token(purchase_token)
    if not user_id and notification_type in RTDN_GRANT_TYPES:
        return {"status": "no_user_for_token"}

    if notification_type in RTDN_REVOKE_TYPES:
        if user_id:
            await neon_db.revoke_play_entitlement(user_id)
        return {"status": "revoked", "notification_type": notification_type}

    if notification_type in RTDN_GRANT_TYPES and user_id:
        if not google_play_billing.configured():
            return {"status": "verification_not_configured"}
        try:
            verified = await google_play_billing.verify_subscription(product_id, purchase_token)
        except ValueError:
            return {"status": "verification_failed"}
        if verified.get("active"):
            await neon_db.upsert_play_entitlement(
                user_id=user_id,
                product_id=product_id,
                purchase_token=purchase_token,
                order_id=verified.get("order_id"),
                expires_at=verified.get("expires_at"),
                is_premium=True,
                source="google_play_rtdn",
            )
            return {"status": "granted", "notification_type": notification_type}
        await neon_db.revoke_play_entitlement(user_id)
        return {"status": "inactive", "notification_type": notification_type}

    return {"status": "ignored", "notification_type": notification_type}
