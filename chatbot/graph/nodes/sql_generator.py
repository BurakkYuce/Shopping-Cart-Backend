"""
Agent 2 — SQL Generator
Generates a SELECT query from natural language, then executes it.
On re-invocation after error_recovery, skips LLM and only re-executes.
"""
import re
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage
from sqlalchemy.exc import SQLAlchemyError

from db.executor import execute_query
from graph.state import AgentState
from llm.provider import get_llm
from rbac.sql_filter import get_sql_filter

_PROMPT_TEMPLATE = (Path(__file__).parent.parent.parent / "prompts" / "sql_generator_system.md").read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_llm(temperature=0.0)
    return _LLM


def _format_history(history: list[dict]) -> str:
    if not history:
        return "(no prior conversation)"
    recent = history[-6:]  # last 3 Q&A pairs
    lines = []
    for msg in recent:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        lines.append(f"{role.upper()}: {content[:300]}")
    return "\n".join(lines)


def _strip_sql_fences(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-z]*\n?", "", text)
        text = re.sub(r"\n?```$", "", text)
    return text.strip()


def _build_cte_block_for_prompt(role_filter_cte: str) -> str:
    if not role_filter_cte:
        return "(No CTE required — you have full access to all tables.)"
    return (
        "You MUST start your query with the following CTE block VERBATIM:\n\n"
        f"```sql\n{role_filter_cte}\n```"
    )


def sql_generator_node(state: AgentState) -> AgentState:
    retry_count = state.get("retry_count", 0)
    sql_error_cleared = state.get("sql_error") is None and retry_count > 0

    if sql_error_cleared:
        # error_recovery already set generated_sql — just re-execute
        sql = state["generated_sql"]
    else:
        # Generate SQL via LLM
        cte = state.get("role_filter_cte", "")
        prompt = _PROMPT_TEMPLATE.format(
            schema_context=state["schema_context"],
            role=state["user_context"]["role"],
            user_id=state["user_context"].get("user_id", ""),
            role_filter_cte_block=_build_cte_block_for_prompt(cte),
            role_usage_hint=state.get("role_usage_hint", ""),
            message_history=_format_history(state.get("message_history", [])),
            original_question=state["original_question"],
        )

        messages = [
            SystemMessage(content=prompt),
            HumanMessage(content=state["original_question"]),
        ]

        response = _get_llm().invoke(messages)
        sql = _strip_sql_fences(response.content)

        # Hard RBAC fallback: if the LLM dropped the CTE block for a scoped role,
        # prepend it before execution so cross-role table access is structurally blocked.
        sql = get_sql_filter().inject_cte_if_missing(
            sql,
            state["user_context"]["role"],
            state["user_context"].get("user_id", ""),
        )

    # Execute
    try:
        # Structural RBAC: reject queries that reach past the CTE aliases.
        # The LLM sometimes defines _allowed_* CTEs correctly but then the main
        # SELECT uses the base tables — silently leaking cross-role data.
        sql = get_sql_filter().enforce_scope(sql, state["user_context"]["role"])
        result = execute_query(sql)
        return {
            **state,
            "generated_sql": sql,
            "sql_error": None,
            "execution_result": result,
        }
    except (SQLAlchemyError, ValueError, Exception) as e:
        return {
            **state,
            "generated_sql": sql,
            "sql_error": str(e),
            "execution_result": None,
        }
