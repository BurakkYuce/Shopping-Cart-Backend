"""
Agent 4 — Analysis
Narrates query results in plain English with role-appropriate framing.
"""
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from db.engine import get_engine
from graph.nodes._logging import log_event
from graph.state import AgentState, ExecutionResult
from llm.provider import get_fast_llm

_PROMPT_TEMPLATE = (Path(__file__).parent.parent.parent / "prompts" / "analysis_system.md").read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_fast_llm(temperature=0.3)
    return _LLM


def _run_scope_diagnostic(state: AgentState) -> dict:
    """Return scope facts for the user so the LLM can distinguish between
    "user has no data at all" vs "user has data but the filter window is empty".

    Hardcoded on purpose — we don't want the LLM inventing diagnostic SQL.
    ADMIN short-circuits with no DB hit; platform-wide is all-or-nothing context.
    """
    role = state["user_context"]["role"]
    if role == "ADMIN":
        return {"role": "ADMIN", "platform_wide": True}

    uid = state["user_context"].get("user_id") or ""
    try:
        with get_engine().connect() as conn:
            if role == "CORPORATE":
                store_count = conn.execute(
                    text("SELECT COUNT(*) FROM stores WHERE owner_id = :uid"),
                    {"uid": uid},
                ).scalar() or 0
                order_count = conn.execute(
                    text(
                        "SELECT COUNT(*) FROM orders o "
                        "JOIN stores s ON o.store_id = s.id "
                        "WHERE s.owner_id = :uid"
                    ),
                    {"uid": uid},
                ).scalar() or 0
                return {
                    "role": "CORPORATE",
                    "store_count": int(store_count),
                    "order_count": int(order_count),
                }
            # INDIVIDUAL
            order_count = conn.execute(
                text("SELECT COUNT(*) FROM orders WHERE user_id = :uid"),
                {"uid": uid},
            ).scalar() or 0
            review_count = conn.execute(
                text("SELECT COUNT(*) FROM reviews WHERE user_id = :uid"),
                {"uid": uid},
            ).scalar() or 0
            return {
                "role": "INDIVIDUAL",
                "order_count": int(order_count),
                "review_count": int(review_count),
            }
    except SQLAlchemyError as e:
        return {"role": role, "diagnostic_error": str(e)[:200]}


def _format_scope_diagnostic(diag: dict) -> str:
    """Render the diagnostic dict as instructional context for the analysis LLM."""
    role = diag.get("role")
    if diag.get("diagnostic_error"):
        return (
            "(Main query returned 0 rows. Scope diagnostic failed: "
            f"{diag['diagnostic_error']}. Default to 'no records found'.)"
        )
    if role == "ADMIN":
        return (
            "(Main query returned 0 rows. Role: ADMIN, platform-wide. "
            "Interpret as: no records match the requested filter across the entire platform.)"
        )
    if role == "CORPORATE":
        store_count = diag.get("store_count", 0)
        order_count = diag.get("order_count", 0)
        if store_count == 0:
            return (
                "(Main query returned 0 rows. Scope diagnostic: user owns 0 stores. "
                "Interpret as: the user has not onboarded any stores yet; there is no data to report.)"
            )
        if order_count == 0:
            return (
                f"(Main query returned 0 rows. Scope diagnostic: user owns {store_count} store(s) "
                "but has 0 total orders across all time. Interpret as: stores exist but have not "
                "received any orders yet.)"
            )
        return (
            f"(Main query returned 0 rows. Scope diagnostic: user owns {store_count} store(s) "
            f"with {order_count} total orders across all time. Interpret as: the filter window "
            "or criteria in the current question returned no rows — suggest broadening the timeframe "
            "or removing a filter.)"
        )
    # INDIVIDUAL
    order_count = diag.get("order_count", 0)
    review_count = diag.get("review_count", 0)
    if order_count == 0 and review_count == 0:
        return (
            "(Main query returned 0 rows. Scope diagnostic: user has 0 orders and 0 reviews. "
            "Interpret as: this is a new account with no activity yet.)"
        )
    return (
        f"(Main query returned 0 rows. Scope diagnostic: user has {order_count} total order(s) "
        f"and {review_count} total review(s) across all time. Interpret as: the filter window "
        "or criteria in the current question returned no rows — suggest broadening the timeframe "
        "or removing a filter.)"
    )


def _format_result_table(result: ExecutionResult) -> str:
    if result["row_count"] == 0:
        return "(Query returned no rows)"

    columns = result["columns"]
    rows = result["rows"][:10]  # show at most 10 rows to LLM

    header = " | ".join(str(c) for c in columns)
    separator = " | ".join("---" for _ in columns)
    body_lines = [" | ".join(str(v) for v in row) for row in rows]

    table = "\n".join([header, separator] + body_lines)
    return f"Columns: {', '.join(columns)}\n\n{table}\n\nTotal rows: {result['row_count']}"


def analysis_node(state: AgentState) -> AgentState:
    result = state["execution_result"]
    role = state["user_context"]["role"]

    result_table = _format_result_table(result)
    truncation_notice = (
        "Note: The result was truncated to 500 rows. The analysis below covers only the returned rows."
        if result.get("truncated")
        else ""
    )

    # Short-circuit when error_recovery tore through all retries without producing a query
    # (state["generated_sql"] might still be set from the failed attempt, but row_count == 0 here
    # is the signal that the main query actually ran and came back empty — NOT a recovery no-op).
    scope_diag: dict | None = None
    if state.get("generated_sql") and result.get("row_count", 0) == 0 and not result.get("truncated"):
        scope_diag = _run_scope_diagnostic(state)
        result_table = _format_scope_diagnostic(scope_diag)
        log_event(
            "analysis.scope_diagnostic",
            session_id=state.get("session_id"),
            user_id=state["user_context"].get("user_id"),
            role=role,
            diagnostic=scope_diag,
        )

    prompt = _PROMPT_TEMPLATE.format(
        role=role,
        original_question=state["original_question"],
        result_table=result_table,
        truncation_notice=truncation_notice,
    )

    messages = [
        SystemMessage(content=prompt),
        HumanMessage(content="Summarise these results."),
    ]

    response = _get_llm().invoke(messages)
    analysis_text = response.content.strip()

    log_event(
        "analysis.completed",
        session_id=state.get("session_id"),
        user_id=state["user_context"].get("user_id"),
        role=role,
        row_count=result.get("row_count", 0),
        truncated=bool(result.get("truncated")),
        scope_diagnostic_ran=scope_diag is not None,
        analysis_chars=len(analysis_text),
    )

    return {**state, "analysis_text": analysis_text}
