"""
Shared structured-logging helpers for chatbot graph nodes.

Logs are emitted as single-line JSON so they can be shipped to ELK and queried
by event / session / user. Question text and generated SQL default to hash-only
to keep PII off disk; set `LOG_RAW_QUESTION=true` in dev or a time-bounded
debug window to also emit a short preview.
"""
from __future__ import annotations

import hashlib
import json
import logging
import os
from typing import Any


_LOG_RAW = os.environ.get("LOG_RAW_QUESTION", "false").strip().lower() == "true"
logger = logging.getLogger("chatbot.graph")


def hash16(value: str | None) -> str | None:
    """Return the first 16 chars of a sha256 hex digest — stable anon token."""
    if value is None:
        return None
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]


def preview(value: str | None, limit: int = 80) -> str | None:
    """Truncated raw preview, gated on LOG_RAW_QUESTION."""
    if value is None or not _LOG_RAW:
        return None
    return value[:limit]


def log_event(event: str, *, level: str = "info", **fields: Any) -> None:
    """Emit a single JSON line with `event` plus arbitrary kv fields.

    `None` values are preserved so downstream schemas stay stable across calls.
    """
    payload: dict[str, Any] = {"event": event}
    payload.update(fields)
    line = json.dumps(payload, ensure_ascii=False, default=str)
    getattr(logger, level, logger.info)(line)
