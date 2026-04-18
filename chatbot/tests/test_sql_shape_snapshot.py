"""
Real-LLM snapshot regression — nightly only.

Asserts the LLM, given the full rendered prompt, still produces SQL with the
expected structural fingerprints (right metric, right status filter, right
CTE aliases, no references to dropped tables).

Gated on:
  - OPENAI_API_KEY present (skipped without it)
  - CI_SKIP_LLM_TESTS != "1" (escape hatch for outages / PRs)

Execute a query is ALSO mocked here — we only care about what the LLM emits,
not whether it runs. That keeps the test independent of Postgres state.
"""
from __future__ import annotations

import os
import pytest

pytestmark = pytest.mark.skipif(
    not os.getenv("OPENAI_API_KEY") or os.getenv("CI_SKIP_LLM_TESTS") == "1",
    reason="Real-LLM snapshot; OPENAI_API_KEY required and CI_SKIP_LLM_TESTS must be unset.",
)


# (question, role, user_id, must_contain_in_sql, must_not_contain_in_sql)
# Substrings are compared case-sensitively against the generated SQL only.
# Keep assertions structural — exact ORDER BY wording varies between runs.
GOLDEN_SQL = [
    (
        "Compare my best-selling stores",
        "CORPORATE",
        "test-corp",
        [
            "_allowed_stores",
            "_allowed_orders",
            "_allowed_order_items",
            "oi.price",
            "'CONFIRMED'",
            "'DELIVERED'",
            "ORDER BY",
            "LIMIT",
        ],
        ["customer_profiles", "unit_price"],
    ),
    (
        "My top 5 products this month",
        "CORPORATE",
        "test-corp",
        [
            "_allowed_products",
            "_allowed_order_items",
            "_allowed_orders",
            "DATE_TRUNC",
            "LIMIT 5",
        ],
        ["customer_profiles"],
    ),
    (
        "Son siparişlerim",
        "INDIVIDUAL",
        "test-ind",
        ["_allowed_orders", "ORDER BY", "DESC", "LIMIT"],
        ["customer_profiles"],
    ),
    (
        "Bu ay ne kadar harcadım",
        "INDIVIDUAL",
        "test-ind",
        ["_allowed_orders", "DATE_TRUNC", "grand_total", "SUM"],
        ["customer_profiles"],
    ),
    (
        "Platform-wide revenue last 30 days",
        "ADMIN",
        "test-admin",
        [
            "order_items",  # ADMIN can reference base tables
            "orders",
            "SUM",
            "'CONFIRMED'",
        ],
        ["customer_profiles"],
    ),
]


@pytest.fixture
def real_executor_stub(monkeypatch):
    """Prevent the node from hitting Postgres — we only inspect the generated SQL."""
    import graph.nodes.sql_generator as sg

    def _noop_execute(sql: str):
        return {"columns": [], "rows": [], "row_count": 0, "truncated": False}

    monkeypatch.setattr(sg, "execute_query", _noop_execute)


@pytest.mark.parametrize("question,role,user_id,must,must_not", GOLDEN_SQL)
def test_sql_snapshot(
    question, role, user_id, must, must_not, real_executor_stub, build_test_state
):
    from graph.nodes.sql_generator import sql_generator_node

    state = build_test_state(question, role=role, user_id=user_id)
    out = sql_generator_node(state)
    sql = out["generated_sql"] or ""

    for needle in must:
        assert needle in sql, (
            f"[{question}] expected SQL substring missing: {needle!r}\n"
            f"--- generated sql ---\n{sql}"
        )
    for needle in must_not:
        assert needle not in sql, (
            f"[{question}] forbidden SQL substring leaked: {needle!r}\n"
            f"--- generated sql ---\n{sql}"
        )
