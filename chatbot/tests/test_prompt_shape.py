"""
Prompt-shape regression tests.

Strategy: we don't assert what SQL the LLM *produces* — that's nondeterministic
and paid. We assert what the node *feeds* to the LLM. If someone deletes the
glossary, removes a canonical example, or breaks the anaphora history gate,
these tests fail immediately in CI without any OpenAI key.

Pair with `test_sql_shape_snapshot.py` (nightly, real LLM) which covers the
other direction: given the prompt, does the LLM still emit the expected SQL?
"""
from __future__ import annotations

import pytest


def _rendered_prompt(messages) -> str:
    """Flatten the message list fed to the LLM into a single searchable string."""
    return "\n".join(str(m.content) for m in messages)


# USER: / ASSISTANT: prefixes are ONLY produced by format_recent_history — they
# are a reliable signal that real turn content was injected. The bare sentinel
# string "(no prior conversation)" also appears in prompt documentation, so we
# don't use it as a must/must_not probe.

# (question, role, user_id, message_history, must_contain, must_not_contain)
GOLDEN_PROMPTS = [
    # 1. Canonical CORPORATE question — glossary + examples injected, history empty.
    (
        "Compare my best-selling stores",
        "CORPORATE",
        "corp-user-1",
        [],
        [
            "Business Term Canonical Definitions",
            "Canonical Examples",
            "Example 1",
            "SUM(oi.price * oi.quantity)",
        ],
        # No prior turn content should reach the prompt for a standalone question.
        ["USER: prior assistant table", "ASSISTANT: product_id"],
    ),
    # 2. Anaphoric follow-up — history block MUST render
    (
        "bunların toplam geliri",
        "CORPORATE",
        "corp-user-1",
        [
            {"role": "user", "content": "top 3 products"},
            {
                "role": "assistant",
                "content": "product_id | name | revenue\n11111111 | Widget | 4200",
            },
        ],
        [
            "product_id | name | revenue",
            "Widget",
            "USER: top 3 products",
            "ASSISTANT: product_id",
        ],
        [],
    ),
    # 3. Turkish "bu ay" — bare "bu" must NOT trigger history injection
    (
        "Bu ay ne kadar harcadım",
        "INDIVIDUAL",
        "ind-user-1",
        [
            {"role": "user", "content": "previous Q about cart"},
            {"role": "assistant", "content": "cart items listed"},
        ],
        [
            "Example 4",
            "DATE_TRUNC('month', NOW())",
        ],
        ["USER: previous Q about cart", "ASSISTANT: cart items listed"],
    ),
    # 4. English "this month" — "this" bare, also standalone
    (
        "How much did my stores earn this month?",
        "CORPORATE",
        "corp-user-2",
        [
            {"role": "user", "content": "previous Q about cart"},
            {"role": "assistant", "content": "cart items listed"},
        ],
        [],
        ["USER: previous Q about cart", "ASSISTANT: cart items listed"],
    ),
    # 5. Plural anaphora with Turkish stem suffix — "yukarıdakilerin" anaphoric
    (
        "yukarıdakilerin ortalaması",
        "CORPORATE",
        "corp-user-1",
        [
            {"role": "user", "content": "top 5"},
            {"role": "assistant", "content": "list shown earlier"},
        ],
        ["USER: top 5", "ASSISTANT: list shown earlier"],
        [],
    ),
    # 6. ADMIN role — user_id still injected; standalone question, no history leak
    (
        "Platform revenue last 30 days",
        "ADMIN",
        "admin-1",
        [],
        [
            "Role: **ADMIN**",
            "Example 6",
            "NEVER",  # the explicit "don't add user_id filter for ADMIN" rule
        ],
        ["USER: ", "ASSISTANT: "],
    ),
]


@pytest.mark.parametrize(
    "question,role,user_id,history,must,must_not", GOLDEN_PROMPTS
)
def test_prompt_render(
    question,
    role,
    user_id,
    history,
    must,
    must_not,
    mock_llm,
    mock_executor,
    mock_sql_filter,
    build_test_state,
):
    from graph.nodes.sql_generator import sql_generator_node

    state = build_test_state(
        question,
        role=role,
        user_id=user_id,
        message_history=history,
    )
    sql_generator_node(state)

    messages = mock_llm["messages"]
    assert messages is not None, "LLM stub was never invoked"
    rendered = _rendered_prompt(messages)

    for needle in must:
        assert needle in rendered, (
            f"[{question}] expected substring missing: {needle!r}\n"
            f"--- rendered prompt ---\n{rendered[:2000]}"
        )
    for needle in must_not:
        assert needle not in rendered, (
            f"[{question}] forbidden substring leaked: {needle!r}\n"
            f"--- rendered prompt ---\n{rendered[:2000]}"
        )


def test_user_question_reaches_prompt(
    mock_llm, mock_executor, mock_sql_filter, build_test_state
):
    """Sanity: the raw user question must appear in the HumanMessage."""
    from graph.nodes.sql_generator import sql_generator_node

    q = "Compare my best-selling stores"
    state = build_test_state(q, role="CORPORATE")
    sql_generator_node(state)

    messages = mock_llm["messages"]
    # HumanMessage is the last element in the list
    human = messages[-1]
    assert q == human.content
