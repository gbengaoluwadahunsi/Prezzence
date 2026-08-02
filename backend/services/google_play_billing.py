"""Google Play subscription verification via Android Publisher API."""

from __future__ import annotations

import asyncio
import json
import os
from datetime import datetime, timedelta, timezone
from typing import Any, Optional

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
ACTIVE_PAYMENT_STATES = {1, 2}  # payment received, free trial
# One-time product purchaseState: 0 = purchased, 1 = canceled, 2 = pending.
PRODUCT_PURCHASED_STATE = 0


class GooglePlayBillingService:
    def __init__(self) -> None:
        self.package_name = os.getenv("GOOGLE_PLAY_PACKAGE_NAME", "com.pollecode.prezzence").strip()
        self.service_account_json = os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "").strip()
        self.service_account_file = os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_FILE", "").strip()
        self._service: Any = None

    def configured(self) -> bool:
        return bool(self.service_account_json or self.service_account_file)

    def _credentials(self):
        if self.service_account_json:
            info = json.loads(self.service_account_json)
            return service_account.Credentials.from_service_account_info(
                info,
                scopes=[ANDROID_PUBLISHER_SCOPE],
            )
        if self.service_account_file and os.path.isfile(self.service_account_file):
            return service_account.Credentials.from_service_account_file(
                self.service_account_file,
                scopes=[ANDROID_PUBLISHER_SCOPE],
            )
        raise RuntimeError("Google Play service account is not configured")

    def _publisher(self):
        if self._service is None:
            self._service = build(
                "androidpublisher",
                "v3",
                credentials=self._credentials(),
                cache_discovery=False,
            )
        return self._service

    def _verify_subscription_sync(self, product_id: str, purchase_token: str) -> dict:
        publisher = self._publisher()
        try:
            result = (
                publisher.purchases()
                .subscriptions()
                .get(
                    packageName=self.package_name,
                    subscriptionId=product_id,
                    token=purchase_token,
                )
                .execute()
            )
        except HttpError as exc:
            status = getattr(exc.resp, "status", None)
            raise ValueError(f"Google Play verification failed (HTTP {status})") from exc

        expiry_ms = int(result.get("expiryTimeMillis") or 0)
        payment_state = int(result.get("paymentState") or 0)
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        is_active = payment_state in ACTIVE_PAYMENT_STATES and expiry_ms > now_ms

        expires_at = None
        if expiry_ms > 0:
            expires_at = datetime.fromtimestamp(expiry_ms / 1000, tz=timezone.utc)

        return {
            "active": is_active,
            "product_id": product_id,
            "purchase_token": purchase_token,
            "order_id": str(result.get("orderId") or ""),
            "payment_state": payment_state,
            "auto_renewing": bool(result.get("autoRenewing")),
            "expires_at": expires_at,
            "cancel_reason": result.get("cancelReason"),
            "raw": result,
        }

    async def verify_subscription(self, product_id: str, purchase_token: str) -> dict:
        return await asyncio.to_thread(self._verify_subscription_sync, product_id, purchase_token)

    def _verify_product_sync(self, product_id: str, purchase_token: str, duration_days: int) -> dict:
        """Verify a one-time (managed) product purchase and derive a fixed-window expiry."""
        publisher = self._publisher()
        try:
            result = (
                publisher.purchases()
                .products()
                .get(
                    packageName=self.package_name,
                    productId=product_id,
                    token=purchase_token,
                )
                .execute()
            )
        except HttpError as exc:
            status = getattr(exc.resp, "status", None)
            raise ValueError(f"Google Play verification failed (HTTP {status})") from exc

        purchase_state = int(result.get("purchaseState") or 0)
        purchase_time_ms = int(result.get("purchaseTimeMillis") or 0)
        is_purchased = purchase_state == PRODUCT_PURCHASED_STATE

        purchased_at = None
        expires_at = None
        if purchase_time_ms > 0:
            purchased_at = datetime.fromtimestamp(purchase_time_ms / 1000, tz=timezone.utc)
            expires_at = purchased_at + timedelta(days=duration_days)

        now = datetime.now(timezone.utc)
        is_active = is_purchased and expires_at is not None and expires_at > now

        return {
            "active": is_active,
            "product_id": product_id,
            "purchase_token": purchase_token,
            "order_id": str(result.get("orderId") or ""),
            "purchase_state": purchase_state,
            "purchased_at": purchased_at,
            "expires_at": expires_at,
            "raw": result,
        }

    async def verify_product(self, product_id: str, purchase_token: str, duration_days: int) -> dict:
        return await asyncio.to_thread(self._verify_product_sync, product_id, purchase_token, duration_days)


google_play_billing = GooglePlayBillingService()
