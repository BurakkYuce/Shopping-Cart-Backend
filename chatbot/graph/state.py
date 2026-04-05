from __future__ import annotations

from operator import add
from typing import Annotated, Optional, TypedDict


class UserContext(TypedDict):
    user_id: str
    email: str
    role: str  # "ADMIN" | "CORPORATE" | "INDIVIDUAL"


class ExecutionResult(TypedDict):
    columns: list[str]
    rows: list[list]
    row_count: int
    truncated: bool


class VisualizationSpec(TypedDict):
    chart_type: str  # "bar" | "line" | "pie" | "scatter" | "table" | "none"
    x_column: str
    y_column: str
    title: str
    plotly_json: str  # serialised Plotly figure JSON


class AgentState(TypedDict):
    # ── Input ──────────────────────────────────────────────────────────────
    original_question: str
    session_id: str
    user_context: UserContext

    # ── Routing ────────────────────────────────────────────────────────────
    intent: str  # "sql_query" | "greeting" | "off_topic" | "clarify"
    is_safe: bool
    guardrail_rejection_reason: Optional[str]

    # ── SQL pipeline ───────────────────────────────────────────────────────
    schema_context: str       # built once per session
    role_filter_cte: str      # built once at session start
    role_usage_hint: str      # injected into SQL generator prompt
    generated_sql: Optional[str]
    sql_error: Optional[str]
    retry_count: int

    # ── Results ────────────────────────────────────────────────────────────
    execution_result: Optional[ExecutionResult]

    # ── Output ─────────────────────────────────────────────────────────────
    analysis_text: str
    visualization_spec: Optional[VisualizationSpec]
    final_response: str

    # ── Conversation memory (appended each turn) ───────────────────────────
    message_history: Annotated[list[dict], add]

    # ── Error tracking ─────────────────────────────────────────────────────
    fatal_error: Optional[str]
