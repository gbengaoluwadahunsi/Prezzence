import os

from core.production_config import (
    DEPRECATED_ASSETLINKS_FINGERPRINT,
    PRODUCTION_PACKAGE,
    assetlinks_fingerprints,
    assetlinks_package_names,
    production_status,
    production_warnings,
)


def test_default_production_package(monkeypatch):
    monkeypatch.delenv("ANDROID_APP_PACKAGE_NAMES", raising=False)
    assert assetlinks_package_names() == [PRODUCTION_PACKAGE]


def test_assetlinks_fingerprints_require_env(monkeypatch):
    monkeypatch.delenv("ANDROID_APP_SHA256_FINGERPRINTS", raising=False)
    assert assetlinks_fingerprints() == []


def test_production_warnings_for_missing_fingerprint(monkeypatch):
    monkeypatch.setenv("ENVIRONMENT", "production")
    monkeypatch.delenv("ANDROID_APP_SHA256_FINGERPRINTS", raising=False)
    monkeypatch.delenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", raising=False)
    warnings = production_warnings()
    assert any("ANDROID_APP_SHA256_FINGERPRINTS" in item for item in warnings)
    assert any("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" in item for item in warnings)


def test_production_warnings_for_deprecated_fingerprint(monkeypatch):
    monkeypatch.setenv("ANDROID_APP_SHA256_FINGERPRINTS", DEPRECATED_ASSETLINKS_FINGERPRINT)
    warnings = production_warnings()
    assert any("deprecated" in item.lower() for item in warnings)


def test_production_status_ready_flag(monkeypatch):
    monkeypatch.setenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", '{"type":"service_account"}')
    monkeypatch.setenv("ANDROID_APP_SHA256_FINGERPRINTS", "AA:BB:CC:DD")
    monkeypatch.setenv("ANDROID_APP_PACKAGE_NAMES", PRODUCTION_PACKAGE)
    status = production_status()
    assert status["ready_for_paid_users"] is True
    assert status["billing_verification_configured"] is True
