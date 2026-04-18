"""
Shared pytest fixtures for chatbot graph-node unit tests.

The primary purpose is to let us render prompts through the real node code
without calling OpenAI or Postgres — so CI stays deterministic and free.

Two load-bearing details:

1. `get_llm` must be patched at the IMPORT SITE (e.g.
   `graph.nodes.sql_generator.get_llm`), not at the definition in
   `llm.provider.get_llm`. Node modules do `from llm.provider import get_llm`,
   which binds a local name — patching the provider module after import time
   does nothing.

2. Each node module caches the LLM instance in a module-level `_LLM` variable.
   We reset that to `None` before every patch so the next call hits the new
   stub, not a previously-cached real client.
"""
from __future__ import annotations

from typing import Any
from unittest.mock import MagicMock

import pytest


class _StubLLM:
    """LLM stub that records every invocation and returns a canned response."""

    def __init__(self, content: str = "SELECT 1;") -> None:
        self.content = content
        self.calls: list[list] = []

    def invoke(self, messages, **_kwargs):
        self.calls.append(messages)
        resp = MagicMock()
        resp.content = self.content
        resp.response_metadata = {"system_fingerprint": "stub-fp"}
        return resp


@pytest.fixture
def mock_llm(monkeypatch):
    """Replace `get_llm` in sql_generator with a stub; reset its _LLM cache.

    Returns a dict with the stub and a `captured` key pointing to the last
    message list fed to `invoke()`. Tests assert on `captured["messages"]`
    substring content to verify prompt rendering.
    """
    import graph.nodes.sql_generator as sg

    stub = _StubLLM()
    captured: dict[str, Any] = {"stub": stub, "messages": None}

    def _factory(**_kwargs):
        return stub

    monkeypatch.setattr(sg, "get_llm", _factory)
    monkeypatch.setattr(sg, "_LLM", None, raising=False)

    # Wrap stub.invoke so captured["messages"] always tracks the latest call
    orig_invoke = stub.invoke

    def _tracked_invoke(messages, **kwargs):
        captured["messages"] = messages
        return orig_invoke(messages, **kwargs)

    stub.invoke = _tracked_invoke  # type: ignore[method-assign]
    return captured


@pytest.fixture
def mock_executor(monkeypatch):
    """Replace `execute_query` in sql_generator with a no-op that returns an empty result.

    Keeps the node from hitting Postgres when prompt-shape tests only care about
    what got fed to the LLM.
    """
    import graph.nodes.sql_generator as sg

    calls: list[str] = []

    def _fake_execute(sql: str):
        calls.append(sql)
        return {"columns": [], "rows": [], "row_count": 0, "truncated": False}

    monkeypatch.setattr(sg, "execute_query", _fake_execute)
    return calls


@pytest.fixture
def mock_sql_filter(monkeypatch):
    """Short-circuit RBAC CTE injection + enforce_scope so tests can use arbitrary SQL.

    `inject_cte_if_missing` would prepend a real CTE and `enforce_scope` would
    reject the stub `SELECT 1;` for not using any `_allowed_*` alias. Both pure
    string ops, so we stub both for determinism.
    """
    import graph.nodes.sql_generator as sg

    class _FakeFilter:
        def inject_cte_if_missing(self, sql, role, user_id):
            return sql

        def enforce_scope(self, sql, role):
            return sql

    monkeypatch.setattr(sg, "get_sql_filter", lambda: _FakeFilter())


@pytest.fixture
def build_test_state():
    """Factory for a minimal AgentState that's valid enough to render a prompt."""

    def _build(
        question: str,
        role: str = "CORPORATE",
        user_id: str = "test-user-1",
        message_history: list[dict] | None = None,
        retry_count: int = 0,
        schema_context: str = "(schema placeholder)",
        role_filter_cte: str = "WITH _allowed_stores AS (SELECT id FROM stores WHERE owner_id = 'test-user-1')",
        role_usage_hint: str = "Use _allowed_stores, _allowed_orders, _allowed_products, _allowed_order_items, _allowed_reviews.",
    ) -> dict:
        return {
            "original_question": question,
            "session_id": "test-session",
            "user_context": {"user_id": user_id, "email": "t@t", "role": role},
            "schema_context": schema_context,
            "role_filter_cte": role_filter_cte,
            "role_usage_hint": role_usage_hint,
            "message_history": message_history or [],
            "retry_count": retry_count,
            "sql_error": None,
            "sql_error_type": None,
            "generated_sql": None,
            "execution_result": None,
            "intent": "sql_query",
            "is_safe": True,
            "guardrail_rejection_reason": None,
            "analysis_text": "",
            "visualization_spec": None,
            "final_response": "",
            "fatal_error": None,
        }

    return _build
