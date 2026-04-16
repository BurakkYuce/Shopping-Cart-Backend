"""
Shared helper: format recent conversation turns for LLM prompts.
Used by guardrails (intent classifier) and sql_generator (query builder)
so both nodes see the same multi-turn context.
"""
from __future__ import annotations


def format_recent_history(history: list[dict] | None, turns: int = 3) -> str:
    """Return a compact text block of the last `turns` Q&A pairs.

    One pair = one USER + one ASSISTANT message, so we slice `turns * 2`.
    Each message is truncated to 300 chars to keep the prompt bounded.
    Returns a sentinel string when there is no history so the template
    still renders cleanly.
    """
    if not history:
        return "(no prior conversation)"

    recent = history[-(turns * 2) :]
    lines: list[str] = []
    for msg in recent:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        lines.append(f"{role.upper()}: {content[:300]}")
    return "\n".join(lines)
