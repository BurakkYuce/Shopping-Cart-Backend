"""
Agent 1 — Guardrails
Classifies user intent and checks for safety issues.
"""
import json
import os
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import AgentState
from llm.provider import get_fast_llm

_PROMPT_PATH = Path(__file__).parent.parent.parent / "prompts" / "guardrails_system.md"
_SYSTEM_PROMPT = _PROMPT_PATH.read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_fast_llm(temperature=0.0)
    return _LLM


def guardrails_node(state: AgentState) -> AgentState:
    question = state["original_question"]

    messages = [
        SystemMessage(content=_SYSTEM_PROMPT),
        HumanMessage(content=question),
    ]

    response = _get_llm().invoke(messages)
    raw = response.content.strip()

    # Strip markdown fences if LLM added them
    if raw.startswith("```"):
        raw = raw.split("```")[1]
        if raw.startswith("json"):
            raw = raw[4:]
        raw = raw.strip()

    try:
        parsed = json.loads(raw)
        intent = parsed.get("intent", "off_topic")
        is_safe = bool(parsed.get("is_safe", True))
        reason = parsed.get("reason", "")
    except (json.JSONDecodeError, ValueError):
        # Fail safe: treat as off_topic if parsing breaks
        intent = "off_topic"
        is_safe = False
        reason = f"Guardrails LLM returned unparseable response: {raw[:200]}"

    updates: dict = {
        "intent": intent,
        "is_safe": is_safe,
        "guardrail_rejection_reason": reason if not is_safe else None,
    }

    if not is_safe:
        updates["final_response"] = (
            "I'm unable to help with that request. "
            "Please ask questions about your e-commerce data."
        )
    elif intent == "greeting":
        updates["final_response"] = (
            "Hi! I'm your e-commerce analytics assistant. "
            "Ask me anything about your orders, products, revenue, shipments, reviews, or customers. "
            "For example: 'Show me my top 10 products by revenue this month' or "
            "'What is the average order value by payment method?'"
        )
    elif intent == "clarify":
        updates["final_response"] = (
            "Could you be more specific? For example, which metric are you interested in "
            "(revenue, order count, ratings, shipment status) and for what time period or category?"
        )
    elif intent == "off_topic":
        updates["final_response"] = (
            "I can only answer questions about this e-commerce platform's data — "
            "orders, products, revenue, customers, shipments, and reviews. "
            "What data would you like to explore?"
        )

    return {**state, **updates}
