"""Production readiness checks for Play billing, App Links, and subscriptions."""

from __future__ import annotations

import os

PRODUCTION_PACKAGE = "com.pollecode.prezzence"
SUBSCRIPTION_PRODUCT_ID = "prezzence_pro"
# One-time "interview week" pass: a single purchase that unlocks premium for a
# fixed window. Matches the acute pre-interview moment for users who won't sign
# up for a recurring subscription. Price is configured in Play Console.
WEEK_PASS_PRODUCT_ID = os.getenv("WEEK_PASS_PRODUCT_ID", "prezzence_week")
WEEK_PASS_DURATION_DAYS = int(os.getenv("WEEK_PASS_DURATION_DAYS", "7"))

# Legacy placeholder from early dev — not a Play App Signing or current upload key.
DEPRECATED_ASSETLINKS_FINGERPRINT = (
    "C2:DC:53:13:EB:38:9D:92:86:D5:A8:5B:C8:CD:62:AA:2E:0E:2B:67:84:BF:90:B6:D8:91:59:A7:07:69:48:B8"
)


def _csv_env(name: str, default: str = "") -> list[str]:
    return [item.strip() for item in os.getenv(name, default).split(",") if item.strip()]


def is_production() -> bool:
    env = os.getenv("ENVIRONMENT", os.getenv("APP_ENV", "development")).strip().lower()
    return env in {"production", "prod"}


def assetlinks_package_names() -> list[str]:
    return _csv_env("ANDROID_APP_PACKAGE_NAMES", PRODUCTION_PACKAGE)


def assetlinks_fingerprints() -> list[str]:
    return _csv_env("ANDROID_APP_SHA256_FINGERPRINTS")


def production_warnings() -> list[str]:
    warnings: list[str] = []
    package_name = os.getenv("GOOGLE_PLAY_PACKAGE_NAME", PRODUCTION_PACKAGE).strip()
    if package_name != PRODUCTION_PACKAGE:
        warnings.append(
            f"GOOGLE_PLAY_PACKAGE_NAME is '{package_name}'; production listing expects '{PRODUCTION_PACKAGE}'."
        )

    if not os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "").strip() and not os.getenv(
        "GOOGLE_PLAY_SERVICE_ACCOUNT_FILE", ""
    ).strip():
        warnings.append(
            "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not set — paid subscriptions cannot be verified server-side."
        )

    fingerprints = assetlinks_fingerprints()
    if not fingerprints:
        warnings.append(
            "ANDROID_APP_SHA256_FINGERPRINTS is not set — email verification App Links "
            "(https://prezzence-backend.onrender.com/auth/verified) will not verify on Play-installed builds."
        )
    elif DEPRECATED_ASSETLINKS_FINGERPRINT in fingerprints:
        warnings.append(
            "ANDROID_APP_SHA256_FINGERPRINTS still uses the deprecated dev placeholder. "
            "Replace it with the Play App Signing SHA-256 from Play Console → App integrity → App signing."
        )

    if os.getenv("BETA_UNLOCK_ALL_FEATURES", "false").strip().lower() in {"1", "true", "yes", "on"}:
        warnings.append("BETA_UNLOCK_ALL_FEATURES is enabled — all users receive premium for free.")

    return warnings


def production_status() -> dict:
    fingerprints = assetlinks_fingerprints()
    billing_configured = bool(
        os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "").strip()
        or os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_FILE", "").strip()
    )
    return {
        "environment": os.getenv("ENVIRONMENT", os.getenv("APP_ENV", "development")),
        "play_package_name": os.getenv("GOOGLE_PLAY_PACKAGE_NAME", PRODUCTION_PACKAGE).strip(),
        "subscription_product_id": SUBSCRIPTION_PRODUCT_ID,
        "billing_verification_configured": billing_configured,
        "assetlinks_packages": assetlinks_package_names(),
        "assetlinks_fingerprints_configured": bool(fingerprints),
        "assetlinks_fingerprint_count": len(fingerprints),
        "beta_unlock_all_features": os.getenv("BETA_UNLOCK_ALL_FEATURES", "false").strip().lower()
        in {"1", "true", "yes", "on"},
        "warnings": production_warnings(),
        "ready_for_paid_users": billing_configured
        and bool(fingerprints)
        and PRODUCTION_PACKAGE in assetlinks_package_names(),
    }
