"""
Agent 1 — Guardrails
Classifies user intent and checks for safety issues.
"""
import json
import os
from pathlib import Path

from langchain_core.messages import HumanMessage, SystemMessage

from graph.nodes._history import format_recent_history
from graph.nodes._logging import hash16, log_event, preview
from graph.state import AgentState
from llm.provider import get_fast_llm

_PROMPTS_DIR = Path(__file__).parent.parent.parent / "prompts"
_PROMPT_PATH = _PROMPTS_DIR / "guardrails_system.md"
_SHARED_CONTEXT_PATH = _PROMPTS_DIR / "_shared_context.md"
_SYSTEM_PROMPT_TEMPLATE = _PROMPT_PATH.read_text()
_SHARED_CONTEXT = _SHARED_CONTEXT_PATH.read_text()
_LLM = None


def _get_llm():
    global _LLM
    if _LLM is None:
        _LLM = get_fast_llm(temperature=0.0)
    return _LLM


def guardrails_node(state: AgentState) -> AgentState:
    question = state["original_question"]
    user_context = state.get("user_context") or {}
    role = user_context.get("role", "INDIVIDUAL")
    recent_context = format_recent_history(state.get("message_history", []))
    system_prompt = (
        _SYSTEM_PROMPT_TEMPLATE
        .replace("{role}", role)
        .replace("{recent_context}", recent_context)
        .replace("{shared_context}", _SHARED_CONTEXT)
    )

    messages = [
        SystemMessage(content=system_prompt),
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
        language = parsed.get("language", "en")
        action_key = parsed.get("action_key")
    except (json.JSONDecodeError, ValueError):
        # Fail safe: treat as off_topic if parsing breaks
        intent = "off_topic"
        is_safe = False
        reason = f"Guardrails LLM returned unparseable response: {raw[:200]}"
        language = "en"
        action_key = None
        log_event(
            "guardrails.parse_error",
            level="warning",
            session_id=state.get("session_id"),
            user_id=user_context.get("user_id"),
            role=role,
            raw_preview=raw[:200],
        )

    if language not in ("tr", "en"):
        language = "en"

    # Guard: action_redirect is INDIVIDUAL-only. Downgrade if another role slipped through.
    if intent == "action_redirect" and role != "INDIVIDUAL":
        log_event(
            "guardrails.action_redirect_downgraded",
            level="warning",
            session_id=state.get("session_id"),
            role=role,
            action_key=action_key,
        )
        intent = "sql_query"
        action_key = None

    log_event(
        "guardrails.classified",
        session_id=state.get("session_id"),
        user_id=user_context.get("user_id"),
        role=role,
        question_hash=hash16(question),
        question_preview=preview(question),
        intent=intent,
        is_safe=is_safe,
        language=language,
    )

    updates: dict = {
        "intent": intent,
        "is_safe": is_safe,
        "language": language,
        "action_key": action_key if intent == "action_redirect" else None,
        "guardrail_rejection_reason": reason if not is_safe else None,
    }

    messages_map = _CANNED_RESPONSES.get(language, _CANNED_RESPONSES["en"])

    if not is_safe:
        updates["final_response"] = messages_map["unsafe"]
    elif intent == "greeting":
        updates["final_response"] = messages_map["greeting"]
    elif intent == "clarify":
        updates["final_response"] = messages_map["clarify"]
    elif intent == "off_topic":
        updates["final_response"] = messages_map["off_topic"]

    return {**state, **updates}


_CANNED_RESPONSES = {
    "en": {
        "unsafe": (
            "I'm unable to help with that request. "
            "Please ask questions about your e-commerce data."
        ),
        "greeting": (
            "Hi! I'm your e-commerce analytics assistant. "
            "Ask me anything about your orders, products, revenue, shipments, reviews, or customers. "
            "For example: 'Show me my top 10 products by revenue this month' or "
            "'What is the average order value by payment method?'"
        ),
        "clarify": (
            "Could you be more specific? For example, which metric are you interested in "
            "(revenue, order count, ratings, shipment status) and for what time period or category?"
        ),
        "off_topic": (
            "I can only answer questions about this e-commerce platform's data — "
            "orders, products, revenue, customers, shipments, and reviews. "
            "What data would you like to explore?"
        ),
    },
    "tr": {
        "unsafe": (
            "Bu talebe yardımcı olamıyorum. "
            "Lütfen e-ticaret verilerinizle ilgili sorular sorun."
        ),
        "greeting": (
            "Merhaba! Ben e-ticaret analiz asistanınızım. "
            "Siparişleriniz, ürünleriniz, gelirleriniz, kargolarınız, yorumlarınız veya müşterilerinizle ilgili her şeyi sorabilirsiniz. "
            "Örneğin: 'Bu ay gelire göre en iyi 10 ürünümü göster' veya "
            "'Ödeme yöntemine göre ortalama sipariş değeri nedir?'"
        ),
        "clarify": (
            "Biraz daha açık olabilir misiniz? Örneğin, hangi metrik ilginizi çekiyor "
            "(gelir, sipariş sayısı, puanlar, kargo durumu) ve hangi zaman aralığı veya kategori için?"
        ),
        "off_topic": (
            "Yalnızca bu e-ticaret platformunun verilerine ilişkin sorulara yanıt verebilirim — "
            "siparişler, ürünler, gelir, müşteriler, kargolar ve yorumlar. "
            "Hangi veriyi incelemek istersiniz?"
        ),
    },
}
