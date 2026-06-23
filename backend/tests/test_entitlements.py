from services.entitlements import has_unlimited_access, is_admin_account


def test_admin_email_has_unlimited_access():
    assert has_unlimited_access("gbengaoluwadahunsicodes@gmail.com")
    assert is_admin_account("GBENGAOLUWADAHUNSICODES@GMAIL.COM")


def test_non_admin_email_denied():
    assert not has_unlimited_access("stranger@example.com")
    assert not has_unlimited_access(None)
