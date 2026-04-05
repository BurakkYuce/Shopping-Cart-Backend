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
