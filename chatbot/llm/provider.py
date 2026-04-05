"""
LLM factory. Selected by LLM_PROVIDER env var: "openai" (default) or "anthropic".
Returns a LangChain chat model with a consistent interface.
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

    # Default: OpenAI
    from langchain_openai import ChatOpenAI
    return ChatOpenAI(
        model="gpt-4o",
        temperature=temperature,
        openai_api_key=os.environ.get("OPENAI_API_KEY"),
    )
