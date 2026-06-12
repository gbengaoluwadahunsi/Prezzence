import logging
import sys
from typing import Any

# Custom log format that looks great in the terminal
LOG_FORMAT = "[[%(levelname)s]] %(asctime)s | %(name)s | %(message)s"

def setup_logging():
    # Configure root logger
    logging.basicConfig(
        level=logging.INFO,
        format=LOG_FORMAT,
        handlers=[
            logging.StreamHandler(sys.stdout)
        ]
    )

    # Specific levels for third-party libs
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("uvicorn.error").setLevel(logging.INFO)

def get_logger(name: str):
    return logging.getLogger(name)
