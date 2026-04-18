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
from graph.nodes._history import format_recent_history
from graph.nodes._logging import hash16, log_event, preview
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


# Anaphora / demonstrative tokens that indicate the current question refers back
# to a prior assistant turn. Deliberately excludes bare "bu", "şu", "this", "it"
# because those standalone forms appear constantly in non-anaphoric phrases
# ("bu ay", "bu hafta", "this month"). False negatives are cheap — the user gets
# a clarification question. False positives poison determinism by leaking prior
# history into a standalone query.
_ANAPHORA_TOKENS = {
    # Turkish inflected demonstratives — bare "bu"/"şu"/"o" deliberately excluded
    # because they appear in timeframe phrases ("bu ay", "şu anda") constantly.
    # Only suffixed / plural forms carry unambiguous anaphoric meaning.
    "bunlar", "bunları", "bunların", "bunun", "bundan", "bunu",
    "şunlar", "şunları", "şunun", "şunu",
    "onu", "onları", "onların",
    "seçtiklerim",
    # Turkish relative-clause nominalizers (always anaphoric)
    "olanlar", "olanı", "olanları", "olanların",
    # English explicit references
    "these", "those", "them",
}
# Turkish stems whose inflected forms are always anaphoric. Matched as
# `stem` + optional case suffix so "yukarıdakiler", "yukarıdakilerin",
# "ilkini", "sonuncunun" all resolve correctly without listing every
# morphological variant. Anchored on word boundary to avoid substring hits.
_ANAPHORA_STEM_RE = re.compile(
    r"\b(?:yukarıdaki(?:ler)?|ilki|sonuncu(?:su)?)(?:i|ı|u|ü|e|a|in|ın|un|ün|nin|nın|nun|nün|den|dan|ne|na|yu|yü)?\b",
    flags=re.IGNORECASE | re.UNICODE,
)
_ANAPHORA_PHRASES = (
    "the first", "the last", "the same", "the above",
    "just the ones", "only the ones", "only active ones", "only the active ones",
    "aynı veriler", "aynı sonuç", "ilk sıradaki", "son sıradaki",
)

_WORD_RE = re.compile(r"\w+", flags=re.UNICODE)


def _needs_history(question: str) -> bool:
    """True when the question references an earlier assistant result.

    When False, the history block is replaced with the no-history sentinel so
    the LLM cannot hallucinate a prior turn — this is the primary determinism
    fix for repeat-query inconsistency across chats.
    """
    q = question.lower()
    if any(p in q for p in _ANAPHORA_PHRASES):
        return True
    if _ANAPHORA_STEM_RE.search(q):
        return True
    tokens = set(_WORD_RE.findall(q))
    return bool(tokens & _ANAPHORA_TOKENS)


def _categorize_error(e: Exception) -> str:
    """Classify SQL errors into actionable buckets for error_recovery prompt routing."""
    msg = str(e).lower()

    # enforce_scope raises ValueError BEFORE SQL hits Postgres — highest priority.
    # Tying the "_allowed_" heuristic to ValueError prevents psycopg2 syntax errors
    # that happen to mention `_allowed_orders` in user-written SQL from being
    # misclassified as RBAC violations.
    if isinstance(e, ValueError) and "_allowed_" in msg:
        return "rbac_violation"
    # Postgres-level RBAC (role bypass via view, missing grant, etc.)
    if "cannot read base table" in msg or "permission denied" in msg:
        return "rbac_violation"
    if "does not exist" in msg and "column" in msg:
        return "column_missing"
    if "does not exist" in msg and ("relation" in msg or "table" in msg):
        return "table_missing"
    if "syntax error" in msg:
        return "syntax_error"
    if "timeout" in msg or "canceling statement" in msg:
        return "timeout"
    return "other"


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
    session_id = state.get("session_id")
    user_id = state["user_context"].get("user_id")
    role = state["user_context"]["role"]
    question = state["original_question"]

    if sql_error_cleared:
        # error_recovery already set generated_sql — just re-execute
        sql = state["generated_sql"]
    else:
        # Generate SQL via LLM
        cte = state.get("role_filter_cte", "")
        # Gate prior-turn history on deterministic anaphora detection. Standalone
        # questions get the no-history sentinel so the LLM cannot invent context.
        needs_hist = _needs_history(question)
        history = state.get("message_history", []) if needs_hist else []
        prompt = _PROMPT_TEMPLATE.format(
            schema_context=state["schema_context"],
            role=role,
            user_id=user_id or "",
            role_filter_cte_block=_build_cte_block_for_prompt(cte),
            role_usage_hint=state.get("role_usage_hint", ""),
            message_history=format_recent_history(history),
            original_question=question,
        )

        messages = [
            SystemMessage(content=prompt),
            HumanMessage(content=question),
        ]

        log_event(
            "sql_generator.attempt",
            session_id=session_id,
            user_id=user_id,
            role=role,
            question_hash=hash16(question),
            question_preview=preview(question),
            needs_history=needs_hist,
            retry_count=retry_count,
        )

        response = _get_llm().invoke(messages)
        sql = _strip_sql_fences(response.content)

        # Hard RBAC fallback: if the LLM dropped the CTE block for a scoped role,
        # prepend it before execution so cross-role table access is structurally blocked.
        sql = get_sql_filter().inject_cte_if_missing(sql, role, user_id or "")

    # Execute
    try:
        # Structural RBAC: reject queries that reach past the CTE aliases.
        # The LLM sometimes defines _allowed_* CTEs correctly but then the main
        # SELECT uses the base tables — silently leaking cross-role data.
        sql = get_sql_filter().enforce_scope(sql, role)
        result = execute_query(sql)
        log_event(
            "sql_generator.result",
            session_id=session_id,
            user_id=user_id,
            role=role,
            row_count=result.get("row_count", 0),
            injected_sql_hash=hash16(sql),
            injected_sql_preview=preview(sql, limit=1200),
            retry_count=retry_count,
        )
        return {
            **state,
            "generated_sql": sql,
            "sql_error": None,
            "sql_error_type": None,
            "execution_result": result,
        }
    except (SQLAlchemyError, ValueError, Exception) as e:
        error_type = _categorize_error(e)
        log_event(
            "sql_generator.error",
            level="warning",
            session_id=session_id,
            user_id=user_id,
            role=role,
            error_type=error_type,
            error_msg=str(e)[:500],
            injected_sql_hash=hash16(sql),
            injected_sql_preview=preview(sql, limit=1200),
            retry_count=retry_count,
        )
        return {
            **state,
            "generated_sql": sql,
            "sql_error": str(e),
            "sql_error_type": error_type,
            "execution_result": None,
        }
