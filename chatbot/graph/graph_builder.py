"""
LangGraph StateGraph wiring.

Topology:
  START → guardrails
    ├── off_topic / unsafe / greeting / clarify → END
    └── sql_query → sql_generator
                       ├── success → analysis → visualization → END
                       └── sql_error → error_recovery
                                           ├── retry_count < limit → sql_generator
                                           └── retry_count >= limit → END (fatal)
"""
import os
from typing import Literal

from langgraph.graph import END, START, StateGraph

from graph.nodes.analysis import analysis_node
from graph.nodes.error_recovery import error_recovery_node
from graph.nodes.guardrails import guardrails_node
from graph.nodes.sql_generator import sql_generator_node
from graph.nodes.visualization import visualization_node
from graph.state import AgentState

MAX_RETRIES = int(os.environ.get("SQL_RETRY_LIMIT", 1))


# ── Conditional edge functions ──────────────────────────────────────────────

def after_guardrails(state: AgentState) -> Literal["sql_generator", "__end__"]:
    if not state.get("is_safe", True):
        return END
    intent = state.get("intent", "off_topic")
    if intent == "sql_query":
        return "sql_generator"
    return END


def after_sql_attempt(state: AgentState) -> Literal["analysis", "error_recovery", "__end__"]:
    if state.get("sql_error") is None:
        return "analysis"
    if state.get("retry_count", 0) >= MAX_RETRIES:
        return END
    return "error_recovery"


def after_error_recovery(state: AgentState) -> Literal["sql_generator"]:
    return "sql_generator"


def _handle_fatal_sql(state: AgentState) -> AgentState:
    """Injected before END when retries are exhausted."""
    return {
        **state,
        "fatal_error": "Unable to generate valid SQL after multiple attempts.",
        "final_response": (
            "I wasn't able to generate a working query for your question after several attempts. "
            "Could you rephrase or be more specific? For example, mention the exact table or metric you're after."
        ),
    }


# ── Graph assembly ──────────────────────────────────────────────────────────

def build_graph() -> StateGraph:
    graph = StateGraph(AgentState)

    graph.add_node("guardrails", guardrails_node)
    graph.add_node("sql_generator", sql_generator_node)
    graph.add_node("error_recovery", error_recovery_node)
    graph.add_node("analysis", analysis_node)
    graph.add_node("visualization", visualization_node)
    graph.add_node("fatal_sql_handler", _handle_fatal_sql)

    graph.add_edge(START, "guardrails")

    graph.add_conditional_edges(
        "guardrails",
        after_guardrails,
        {"sql_generator": "sql_generator", END: END},
    )

    graph.add_conditional_edges(
        "sql_generator",
        after_sql_attempt,
        {
            "analysis": "analysis",
            "error_recovery": "error_recovery",
            END: "fatal_sql_handler",
        },
    )

    graph.add_conditional_edges(
        "error_recovery",
        after_error_recovery,
        {"sql_generator": "sql_generator"},
    )

    graph.add_edge("analysis", "visualization")
    graph.add_edge("visualization", END)
    graph.add_edge("fatal_sql_handler", END)

    return graph.compile()


# Module-level compiled graph (lazy-initialised)
_graph = None


def get_graph():
    global _graph
    if _graph is None:
        _graph = build_graph()
    return _graph
