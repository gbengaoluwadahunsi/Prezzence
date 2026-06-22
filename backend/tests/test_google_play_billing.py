from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

import pytest

from services.google_play_billing import GooglePlayBillingService


def test_verify_subscription_sync_active():
    service = GooglePlayBillingService()
    future_ms = int((datetime.now(timezone.utc).timestamp() + 3600) * 1000)
    mock_result = {
        "expiryTimeMillis": str(future_ms),
        "paymentState": 1,
        "orderId": "GPA.1234",
        "autoRenewing": True,
    }
    mock_publisher = MagicMock()
    mock_publisher.purchases().subscriptions().get().execute.return_value = mock_result

    with patch.object(service, "_publisher", return_value=mock_publisher):
        verified = service._verify_subscription_sync("prezzence_pro", "token-abc")

    assert verified["active"] is True
    assert verified["order_id"] == "GPA.1234"
    assert verified["auto_renewing"] is True


def test_verify_subscription_sync_expired():
    service = GooglePlayBillingService()
    past_ms = int((datetime.now(timezone.utc).timestamp() - 3600) * 1000)
    mock_result = {
        "expiryTimeMillis": str(past_ms),
        "paymentState": 1,
        "orderId": "GPA.9999",
        "autoRenewing": False,
    }
    mock_publisher = MagicMock()
    mock_publisher.purchases().subscriptions().get().execute.return_value = mock_result

    with patch.object(service, "_publisher", return_value=mock_publisher):
        verified = service._verify_subscription_sync("prezzence_pro", "token-abc")

    assert verified["active"] is False
