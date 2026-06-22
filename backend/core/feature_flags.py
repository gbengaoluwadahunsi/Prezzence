import os


def env_bool(name: str, default: str = "false") -> bool:
    return os.getenv(name, default).strip().lower() in {"1", "true", "yes", "on"}


BETA_UNLOCK_ALL_FEATURES = env_bool("BETA_UNLOCK_ALL_FEATURES", "false")
FREE_SESSION_LIMIT = int(os.getenv("FREE_SESSION_LIMIT", "3"))
