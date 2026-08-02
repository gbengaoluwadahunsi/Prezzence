"""Google Play Integrity token verification (server side).

Decodes the integrity token produced by the Android client via the Play Integrity API
and summarizes the verdicts (genuine app, device integrity, app licensing). Reuses the
same Google Play service account used for subscription verification.
"""

from __future__ import annotations

import asyncio
import json
import os
from typing import Any

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

PLAY_INTEGRITY_SCOPE = "https://www.googleapis.com/auth/playintegrity"


class PlayIntegrityService:
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
                scopes=[PLAY_INTEGRITY_SCOPE],
            )
        if self.service_account_file and os.path.isfile(self.service_account_file):
            return service_account.Credentials.from_service_account_file(
                self.service_account_file,
                scopes=[PLAY_INTEGRITY_SCOPE],
            )
        raise RuntimeError("Google Play service account is not configured")

    def _api(self):
        if self._service is None:
            self._service = build(
                "playintegrity",
                "v1",
                credentials=self._credentials(),
                cache_discovery=False,
            )
        return self._service

    def _decode_sync(self, token: str) -> dict:
        api = self._api()
        try:
            result = (
                api.v1()
                .decodeIntegrityToken(
                    packageName=self.package_name,
                    body={"integrityToken": token},
                )
                .execute()
            )
        except HttpError as exc:
            status = getattr(exc.resp, "status", None)
            raise ValueError(f"Integrity token decode failed (HTTP {status})") from exc

        payload = result.get("tokenPayloadExternal", {}) or {}
        return self._summarize(payload)

    @staticmethod
    def _summarize(payload: dict) -> dict:
        app_integrity = payload.get("appIntegrity", {}) or {}
        device_integrity = payload.get("deviceIntegrity", {}) or {}
        account_details = payload.get("accountDetails", {}) or {}
        request_details = payload.get("requestDetails", {}) or {}

        app_verdict = app_integrity.get("appRecognitionVerdict", "UNKNOWN")
        device_verdicts = device_integrity.get("deviceRecognitionVerdict", []) or []
        licensing_verdict = account_details.get("appLicensingVerdict", "UNKNOWN")

        genuine_app = app_verdict == "PLAY_RECOGNIZED"
        meets_device_integrity = "MEETS_DEVICE_INTEGRITY" in device_verdicts
        trusted = genuine_app and meets_device_integrity

        return {
            "trusted": trusted,
            "app_verdict": app_verdict,
            "device_verdicts": device_verdicts,
            "licensing_verdict": licensing_verdict,
            "request_package": request_details.get("requestPackageName"),
            "request_hash": request_details.get("requestHash"),
        }

    async def decode_token(self, token: str) -> dict:
        return await asyncio.to_thread(self._decode_sync, token)


play_integrity = PlayIntegrityService()
