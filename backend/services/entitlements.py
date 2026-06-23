"""Accounts with unlimited / premium access without a paid subscription."""

import os

_DEFAULT_ADMIN_EMAILS = frozenset({
    "gbengaoluwadahunsicodes@gmail.com",
    "gbengaoluwadahunsicode@gmail.com",
    "alabiolusola399@gmail.com",
})


def _load_admin_emails() -> frozenset[str]:
    raw = os.getenv("PREMIUM_ADMIN_EMAILS", "").strip()
    if not raw:
        return _DEFAULT_ADMIN_EMAILS
    from_env = {email.strip().lower() for email in raw.split(",") if email.strip()}
    return _DEFAULT_ADMIN_EMAILS | from_env


PREMIUM_EMAIL_ALLOWLIST = _load_admin_emails()


def has_unlimited_access(email: str | None) -> bool:
    return (email or "").strip().lower() in PREMIUM_EMAIL_ALLOWLIST


def is_admin_account(email: str | None) -> bool:
    """Alias for admin / full-feature access checks."""
    return has_unlimited_access(email)
