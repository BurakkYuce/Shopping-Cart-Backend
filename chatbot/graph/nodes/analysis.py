"""
Agent 4 — Analysis
Narrates query results in plain English with role-appropriate framing.
"""
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import AgentState, ExecutionResult
from llm.provider import get_fast_llm

_PROMPT_TEMPLATE = (Path(__file__).parent.parent.parent / "prompts" / "analysis_system.md").read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_fast_llm(temperature=0.3)
    return _LLM


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

    return {**state, "analysis_text": analysis_text}
