from routers.users import NotificationPreferencesRequest


def test_notification_preferences_request_defaults():
    payload = NotificationPreferencesRequest()
    assert payload.push_notifications_enabled is True
    assert payload.email_summaries_enabled is False
    assert payload.practice_reminders_enabled is True
