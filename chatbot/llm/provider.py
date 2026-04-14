"""
LLM factory. Selected by LLM_PROVIDER env var: "openai" (default) or "anthropic".
Returns a LangChain chat model with a consistent interface.

get_llm()      — full-power model for SQL generation (gpt-4o / claude-3-5-sonnet)
get_fast_llm() — lightweight model for guardrails & analysis (gpt-4o-mini / claude-3-5-haiku)
"""
import os

from langchain_core.language_models import BaseChatModel


def get_llm(temperature: float = 0.0) -> BaseChatModel:
    provider = os.environ.get("LLM_PROVIDER", "openai").lower()

    if provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(
            model="claude-3-5-sonnet-20241022",
            temperature=temperature,
            anthropic_api_key=os.environ.get("ANTHROPIC_API_KEY"),
        )

    from langchain_openai import ChatOpenAI
    return ChatOpenAI(
        model="gpt-4o",
        temperature=temperature,
        openai_api_key=os.environ.get("OPENAI_API_KEY"),
    )


def get_fast_llm(temperature: float = 0.0) -> BaseChatModel:
    """Lightweight model for simple classification and summarisation tasks."""
    provider = os.environ.get("LLM_PROVIDER", "openai").lower()

    if provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(
            model="claude-3-5-haiku-20241022",
            temperature=temperature,
            anthropic_api_key=os.environ.get("ANTHROPIC_API_KEY"),
        )

    from langchain_openai import ChatOpenAI
    return ChatOpenAI(
        model="gpt-4o-mini",
        temperature=temperature,
        openai_api_key=os.environ.get("OPENAI_API_KEY"),
    )
