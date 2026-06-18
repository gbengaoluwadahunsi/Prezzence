"""Accounts with unlimited / premium access without a paid subscription."""

PREMIUM_EMAIL_ALLOWLIST = frozenset({
    "gbengaoluwadahunsicodes@gmail.com",
    "gbengaoluwadahunsicode@gmail.com",
    "alabiolusola399@gmail.com",
})


def has_unlimited_access(email: str | None) -> bool:
    return (email or "").strip().lower() in PREMIUM_EMAIL_ALLOWLIST
