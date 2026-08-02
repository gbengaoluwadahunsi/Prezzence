import os


def env_bool(name: str, default: str = "false") -> bool:
    return os.getenv(name, default).strip().lower() in {"1", "true", "yes", "on"}


BETA_UNLOCK_ALL_FEATURES = env_bool("BETA_UNLOCK_ALL_FEATURES", "false")
FREE_SESSION_LIMIT = int(os.getenv("FREE_SESSION_LIMIT", "3"))
# Free sessions are counted within a rolling window so the free tier refreshes
# (e.g. 3 sessions every 30 days) instead of being a permanent lifetime cap.
FREE_SESSION_WINDOW_DAYS = int(os.getenv("FREE_SESSION_WINDOW_DAYS", "30"))
