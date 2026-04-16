"""
FastAPI REST layer — port 8002.
Exposes POST /chat/ask and POST /chat/ask-stream for Spring Boot's ChatService proxy.
Shares the same LangGraph graph as the Chainlit UI.
"""
import json
import uuid
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from auth.jwt_validator import decode_token
from db.engine import check_connection
from db.schema_context import get_schema_context
from graph.graph_builder import get_graph
from graph.state import AgentState
from rbac.sql_filter import get_sql_filter
from visual_search.searcher import (
    search_by_image_bytes, search_by_image_and_text, search_by_text,
    _get_model, InvalidImageError,
)

app = FastAPI(title="E-Commerce Chatbot API", version="1.0.0")


@app.on_event("startup")
def warmup_clip():
    """Pre-load the CLIP model in a background thread.

    Kicking off in a thread so a slow HuggingFace download cannot block
    `/chat/ask` — visual search needs CLIP, but chat Q&A does not.
    """
    import threading

    def _warm():
        try:
            _get_model()
        except Exception:
            pass  # non-fatal — model will load on first visual-search request

    threading.Thread(target=_warm, daemon=True).start()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    message: str
    sessionId: Optional[str] = None
    conversationId: Optional[str] = None
    messageHistory: Optional[list[dict]] = None


class ChatResponse(BaseModel):
    message: str
    sessionId: str
    status: str
    intent: Optional[str] = None
    plotlyJson: Optional[str] = None
    generatedSql: Optional[str] = None


@app.get("/health")
def health():
    db_ok = check_connection()
    return {"status": "ok" if db_ok else "degraded", "db": "up" if db_ok else "down"}


@app.post("/visual-search")
async def visual_search(
    image: UploadFile = File(default=None),
    hint: str = Form(default=""),
    top_k: int = Form(default=9),
):
    """CLIP-based visual product search. Accepts image upload + optional text hint."""
    if image is not None:
        image_bytes = await image.read()
        try:
            if hint.strip():
                results = search_by_image_and_text(image_bytes, hint.strip(), top_k=top_k)
            else:
                results = search_by_image_bytes(image_bytes, top_k=top_k)
        except InvalidImageError as e:
            raise HTTPException(status_code=422, detail=str(e))
    elif hint.strip():
        results = search_by_text(hint.strip(), top_k=top_k)
    else:
        raise HTTPException(status_code=400, detail="Provide an image or a text hint.")

    if not results:
        return {"results": [], "message": "No similar products found."}
    return {"results": results}


@app.post("/chat/ask", response_model=ChatResponse)
def chat_ask(
    request: ChatRequest,
    authorization: Optional[str] = Header(default=None),
):
    # Extract token
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    token = authorization[7:].strip()

    try:
        user_ctx = decode_token(token)
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

    sql_filter = get_sql_filter()
    session_id = request.sessionId or str(uuid.uuid4())

    state: AgentState = {
        "original_question": request.message,
        "session_id": session_id,
        "user_context": {
            "user_id": user_ctx.user_id,
            "email": user_ctx.email,
            "role": user_ctx.role,
        },
        "intent": "",
        "is_safe": True,
        "guardrail_rejection_reason": None,
        "schema_context": get_schema_context(),
        "role_filter_cte": sql_filter.build_cte(user_ctx.role, user_ctx.user_id),
        "role_usage_hint": sql_filter.build_usage_hint(user_ctx.role),
        "generated_sql": None,
        "sql_error": None,
        "retry_count": 0,
        "execution_result": None,
        "analysis_text": "",
        "visualization_spec": None,
        "final_response": "",
        "message_history": request.messageHistory or [],
        "fatal_error": None,
    }

    graph = get_graph()
    result_state = graph.invoke(state)

    vis = result_state.get("visualization_spec")
    plotly_json = vis["plotly_json"] if vis and vis.get("plotly_json") else None

    return ChatResponse(
        message=result_state.get("final_response", ""),
        sessionId=session_id,
        status="completed" if not result_state.get("fatal_error") else "error",
        intent=result_state.get("intent"),
        plotlyJson=plotly_json,
        generatedSql=result_state.get("generated_sql"),
    )


@app.post("/chat/ask-stream")
def chat_ask_stream(
    request: ChatRequest,
    authorization: Optional[str] = Header(default=None),
):
    """SSE streaming variant — emits one event per graph node so the frontend
    can show real-time progress (guardrails → sql → analysis → visualization)."""

    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    token = authorization[7:].strip()
    try:
        user_ctx = decode_token(token)
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

    sql_filter = get_sql_filter()
    session_id = request.sessionId or str(uuid.uuid4())

    state: AgentState = {
        "original_question": request.message,
        "session_id": session_id,
        "user_context": {
            "user_id": user_ctx.user_id,
            "email": user_ctx.email,
            "role": user_ctx.role,
        },
        "intent": "",
        "is_safe": True,
        "guardrail_rejection_reason": None,
        "schema_context": get_schema_context(),
        "role_filter_cte": sql_filter.build_cte(user_ctx.role, user_ctx.user_id),
        "role_usage_hint": sql_filter.build_usage_hint(user_ctx.role),
        "generated_sql": None,
        "sql_error": None,
        "retry_count": 0,
        "execution_result": None,
        "analysis_text": "",
        "visualization_spec": None,
        "final_response": "",
        "message_history": request.messageHistory or [],
        "fatal_error": None,
    }

    def event_stream():
        graph = get_graph()
        collected: dict = {}

        for node_output in graph.stream(state):
            node_name = list(node_output.keys())[0]
            node_data = node_output[node_name]
            collected.update(node_data)

            event: dict = {"node": node_name}

            if node_name == "guardrails":
                event["intent"] = node_data.get("intent", "")
                if node_data.get("final_response"):
                    event["message"] = node_data["final_response"]
            elif node_name == "sql_generator":
                event["generatedSql"] = node_data.get("generated_sql")
                if node_data.get("sql_error"):
                    event["sqlError"] = node_data["sql_error"]
            elif node_name == "error_recovery":
                event["retryCount"] = node_data.get("retry_count", 0)
            elif node_name == "analysis":
                event["analysisText"] = node_data.get("analysis_text", "")
            elif node_name == "visualization":
                vis = node_data.get("visualization_spec")
                if vis and vis.get("plotly_json"):
                    event["plotlyJson"] = vis["plotly_json"]
                event["message"] = node_data.get("final_response", "")
            elif node_name == "fatal_sql_handler":
                event["message"] = node_data.get("final_response", "")
                event["error"] = True

            yield f"data: {json.dumps(event)}\n\n"

        done = {
            "node": "__done__",
            "sessionId": session_id,
            "status": "completed" if not collected.get("fatal_error") else "error",
            "intent": collected.get("intent"),
            "generatedSql": collected.get("generated_sql"),
        }
        vis = collected.get("visualization_spec")
        if vis and vis.get("plotly_json"):
            done["plotlyJson"] = vis["plotly_json"]
        done["message"] = collected.get("final_response", "")
        yield f"data: {json.dumps(done)}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
