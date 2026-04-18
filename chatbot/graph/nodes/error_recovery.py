"""
Agent 3 — Error Recovery
Diagnoses a SQL execution error and produces a corrected query.
Increments retry_count; graph_builder routes back to sql_generator.
"""
import re
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage

from graph.nodes._logging import hash16, log_event, preview
from graph.state import AgentState
from llm.provider import get_llm

_PROMPT_TEMPLATE = (Path(__file__).parent.parent.parent / "prompts" / "error_recovery_system.md").read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_llm(temperature=0.0)
    return _LLM


def _strip_sql_fences(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-z]*\n?", "", text)
        text = re.sub(r"\n?```$", "", text)
    return text.strip()


def _build_cte_block_for_prompt(role_filter_cte: str) -> str:
    if not role_filter_cte:
        return "(No CTE required — full table access.)"
    return (
        "The corrected query MUST include this CTE block at the top:\n\n"
        f"```sql\n{role_filter_cte}\n```"
    )


def error_recovery_node(state: AgentState) -> AgentState:
    retry_count = state.get("retry_count", 0) + 1
    cte = state.get("role_filter_cte", "")

    prompt = _PROMPT_TEMPLATE.format(
        schema_context=state["schema_context"],
        role=state["user_context"]["role"],
        role_filter_cte_block=_build_cte_block_for_prompt(cte),
        role_usage_hint=state.get("role_usage_hint", ""),
        original_question=state["original_question"],
        generated_sql=state.get("generated_sql", "(none)"),
        sql_error=state.get("sql_error", "(unknown error)"),
        error_type=state.get("sql_error_type") or "other",
    )

    messages = [
        SystemMessage(content=prompt),
        HumanMessage(content="Fix the SQL query."),
    ]

    error_type = state.get("sql_error_type") or "other"
    log_event(
        "error_recovery.attempt",
        session_id=state.get("session_id"),
        user_id=state["user_context"].get("user_id"),
        role=state["user_context"]["role"],
        error_type=error_type,
        error_msg=(state.get("sql_error") or "")[:500],
        failing_sql_hash=hash16(state.get("generated_sql")),
        retry_count=retry_count,
    )

    response = _get_llm().invoke(messages)
    corrected_sql = _strip_sql_fences(response.content)

    log_event(
        "error_recovery.corrected",
        session_id=state.get("session_id"),
        user_id=state["user_context"].get("user_id"),
        corrected_sql_hash=hash16(corrected_sql),
        corrected_sql_preview=preview(corrected_sql, limit=1200),
        retry_count=retry_count,
    )

    return {
        **state,
        "generated_sql": corrected_sql,
        "sql_error": None,  # cleared so sql_generator re-executes without re-generating
        "retry_count": retry_count,
    }
