"""
Chainlit entry point — port 8001.
Authentication: user pastes their JWT from Spring Boot's /api/auth/login.
Supports: text queries (LangGraph) + image upload (visual search).
"""
import os
import sys
import uuid

# Ensure the chatbot package root is on sys.path regardless of CWD
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import aiofiles
import chainlit as cl
import chainlit.data as cl_data
import plotly.graph_objects as go
import plotly.io as pio
from chainlit.data.storage_clients.base import BaseStorageClient

_UPLOAD_DIR = "/tmp/chainlit-uploads"
os.makedirs(_UPLOAD_DIR, exist_ok=True)


class LocalStorageClient(BaseStorageClient):
    """Minimal local-disk storage client for Chainlit 2.x file uploads."""

    async def upload_file(self, object_key, data, mime="application/octet-stream",
                          overwrite=True, content_disposition=None):
        path = os.path.join(_UPLOAD_DIR, object_key.replace("/", "_"))
        async with aiofiles.open(path, "wb") as f:
            await f.write(data if isinstance(data, bytes) else data.encode())
        return {"object_key": object_key, "url": f"file://{path}"}

    async def delete_file(self, object_key):
        path = os.path.join(_UPLOAD_DIR, object_key.replace("/", "_"))
        try:
            os.remove(path)
        except FileNotFoundError:
            pass
        return True

    async def get_read_url(self, object_key):
        return os.path.join(_UPLOAD_DIR, object_key.replace("/", "_"))

    async def close(self):
        pass


cl_data.storage_client = LocalStorageClient()

from auth.jwt_validator import decode_token
from db.schema_context import get_schema_context
from graph.graph_builder import get_graph
from graph.state import AgentState
from rbac.sql_filter import get_sql_filter


@cl.on_chat_start
async def on_chat_start():
    res = await cl.AskUserMessage(
        content=(
            "**Welcome to the E-Commerce Analytics Assistant!**\n\n"
            "Please paste your access token (JWT) from the login API to get started.\n"
            "> You can get one via `POST /api/auth/login` in Swagger."
        ),
        timeout=120,
    ).send()

    if not res:
        await cl.Message("Session timed out. Please refresh and try again.").send()
        return

    token = res["output"].strip()
    if token.startswith("Bearer "):
        token = token[7:].strip()

    try:
        user_ctx = decode_token(token)
    except ValueError as e:
        await cl.Message(f"Authentication failed: {e}\n\nPlease refresh and try again.").send()
        return

    sql_filter = get_sql_filter()

    cl.user_session.set("user_context", {
        "user_id": user_ctx.user_id,
        "email": user_ctx.email,
        "role": user_ctx.role,
    })
    cl.user_session.set("role_filter_cte", sql_filter.build_cte(user_ctx.role, user_ctx.user_id))
    cl.user_session.set("role_usage_hint", sql_filter.build_usage_hint(user_ctx.role))
    cl.user_session.set("schema_context", get_schema_context())
    cl.user_session.set("message_history", [])
    cl.user_session.set("session_id", str(uuid.uuid4()))

    await cl.Message(
        f"Authenticated as **{user_ctx.email}** (role: `{user_ctx.role}`).\n\n"
        "Ask me anything about your data, or **upload an image** to find similar products!"
    ).send()


@cl.on_message
async def on_message(message: cl.Message):
    user_context = cl.user_session.get("user_context")
    if not user_context:
        await cl.Message("Please refresh and authenticate first.").send()
        return

    # ── Image upload → visual search ───────────────────────────────────────
    image_files = [f for f in (message.elements or []) if f.mime and f.mime.startswith("image/")]
    if image_files:
        await _handle_visual_search(image_files[0], message.content)
        return

    # ── Text query → LangGraph ─────────────────────────────────────────────
    question = message.content.strip()
    if not question:
        await cl.Message("Please type a question or upload an image.").send()
        return

    history: list[dict] = cl.user_session.get("message_history", [])

    state: AgentState = {
        "original_question": question,
        "session_id": cl.user_session.get("session_id", str(uuid.uuid4())),
        "user_context": user_context,
        "intent": "",
        "is_safe": True,
        "guardrail_rejection_reason": None,
        "schema_context": cl.user_session.get("schema_context", ""),
        "role_filter_cte": cl.user_session.get("role_filter_cte", ""),
        "role_usage_hint": cl.user_session.get("role_usage_hint", ""),
        "generated_sql": None,
        "sql_error": None,
        "retry_count": 0,
        "execution_result": None,
        "analysis_text": "",
        "visualization_spec": None,
        "final_response": "",
        "message_history": history,
        "fatal_error": None,
    }

    async with cl.Step(name="Thinking...", show_input=False) as step:
        graph = get_graph()
        result_state = graph.invoke(state)
        step.output = f"Intent: {result_state.get('intent', 'unknown')}"

    final_text = result_state.get("final_response", "I couldn't process that request.")
    await cl.Message(content=final_text).send()

    vis = result_state.get("visualization_spec")
    if vis and vis.get("chart_type") not in ("none", "table", None):
        try:
            fig = pio.from_json(vis["plotly_json"])
            await cl.Plotly(figure=fig, display="inline").send()
        except Exception:
            pass

    history.append({"role": "user", "content": question})
    history.append({"role": "assistant", "content": final_text})
    cl.user_session.set("message_history", history[-20:])


async def _handle_visual_search(image_file, user_text: str):
    """Run CLIP-based visual similarity search.

    When the user also types a text prompt (e.g. 'find the red version'),
    the image and text embeddings are blended (70 % image / 30 % text) so
    the text acts as a refinement hint rather than overriding visual similarity.
    """
    from visual_search.searcher import search_by_image_bytes, search_by_image_and_text

    text_hint = (user_text or "").strip()

    async with cl.Step(name="Searching similar products...", show_input=False):
        # Chainlit 2.x: prefer path, fall back to content bytes
        image_bytes = None
        if hasattr(image_file, "path") and image_file.path:
            with open(image_file.path, "rb") as f:
                image_bytes = f.read()
        if not image_bytes and hasattr(image_file, "content") and image_file.content:
            image_bytes = image_file.content

        if not image_bytes:
            await cl.Message("Could not read the uploaded image. Please try again.").send()
            return

        if text_hint:
            results = search_by_image_and_text(image_bytes, text_hint, top_k=8)
        else:
            results = search_by_image_bytes(image_bytes, top_k=8)

    if not results:
        await cl.Message("No similar products found. Make sure products with images are loaded.").send()
        return

    # Build summary text
    lines = [f"**Found {len(results)} similar products:**\n"]
    for i, p in enumerate(results, 1):
        price_str = f"${p['price']:.2f}" if p["price"] else "N/A"
        retail_str = f" ~~${p['retail_price']:.2f}~~" if p.get("retail_price") and p["retail_price"] != p["price"] else ""
        rating_str = f"⭐ {p['rating']}" if p.get("rating") else ""
        brand_str = f"*{p['brand']}* — " if p.get("brand") else ""
        lines.append(
            f"{i}. **{p['name'][:70]}**\n"
            f"   {brand_str}{price_str}{retail_str}  {rating_str}  "
            f"(similarity: {p['similarity']:.0%})"
        )

    # Send images inline alongside the text list
    elements = [
        cl.Image(url=p["image_url"], name=p["name"][:40], display="inline")
        for p in results[:8]
        if p.get("image_url")
    ]

    await cl.Message(content="\n".join(lines), elements=elements).send()
