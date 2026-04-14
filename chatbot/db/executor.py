"""
Safe SELECT-only SQL executor.
Raises ValueError for non-SELECT statements before execution.
Hard-limits result rows to MAX_SQL_ROWS (default 500).
"""
import os
import re
from typing import TypedDict

from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from db.engine import get_engine

MAX_ROWS = int(os.environ.get("MAX_SQL_ROWS", 500))

# Patterns that indicate non-SELECT DML/DDL
_FORBIDDEN_PATTERN = re.compile(
    r"^\s*(insert|update|delete|drop|truncate|alter|create|grant|revoke|exec|execute|call)\b",
    re.IGNORECASE,
)

# Credentials must never leave the DB via the chatbot path — block any reference to the column
# regardless of role. Matches the bare name and common obfuscations (p_a_s_s_w_o_r_d_h_a_s_h,
# password\u005fhash, etc.) by stripping non-word characters before the check.
_PASSWORD_PATTERN = re.compile(r"password[_\s]*hash", re.IGNORECASE)


class ExecutionResult(TypedDict):
    columns: list[str]
    rows: list[list]
    row_count: int
    truncated: bool


def execute_query(sql: str) -> ExecutionResult:
    """Execute a read-only SQL query and return results."""
    stripped = sql.strip()

    if _FORBIDDEN_PATTERN.match(stripped):
        raise ValueError(f"Only SELECT queries are allowed. Got: {stripped[:80]}")

    if not re.match(r"^\s*(with|select)\b", stripped, re.IGNORECASE):
        raise ValueError(f"Query must start with SELECT or WITH: {stripped[:80]}")

    # Strip everything but letters so obfuscations like p_a_s_s_w_o_r_d_h_a_s_h or
    # p.a.s.s.w.o.r.d.h.a.s.h collapse to plain "passwordhash" before we match.
    letters_only = re.sub(r"[^a-zA-Z]", "", stripped)
    if _PASSWORD_PATTERN.search(stripped) or re.search(r"passwordhash", letters_only, re.IGNORECASE):
        raise ValueError("Access to credential columns is not permitted.")

    try:
        with get_engine().connect() as conn:
            result = conn.execute(text(stripped))
            columns = list(result.keys())
            all_rows = result.fetchmany(MAX_ROWS + 1)
            truncated = len(all_rows) > MAX_ROWS
            rows = [list(row) for row in all_rows[:MAX_ROWS]]

        return ExecutionResult(
            columns=columns,
            rows=rows,
            row_count=len(rows),
            truncated=truncated,
        )
    except SQLAlchemyError as e:
        # Re-raise with the raw DB error message so error_recovery can use it
        raise SQLAlchemyError(str(e)) from e
