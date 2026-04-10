"""
FastAPI REST layer — port 8002.
Exposes POST /chat/ask for Spring Boot's ChatService proxy.
Shares the same LangGraph graph as the Chainlit UI.
"""
import uuid
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from auth.jwt_validator import decode_token
from db.engine import check_connection
from db.schema_context import get_schema_context
from graph.graph_builder import get_graph
from graph.state import AgentState
from rbac.sql_filter import get_sql_filter
from visual_search.searcher import search_by_image_bytes, search_by_image_and_text, search_by_text, _get_model

app = FastAPI(title="E-Commerce Chatbot API", version="1.0.0")


@app.on_event("startup")
def warmup_clip():
    """Pre-load the CLIP model so the first visual-search request isn't slow."""
    try:
        _get_model()
    except Exception:
        pass  # non-fatal — model will load on first request

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
        if hint.strip():
            results = search_by_image_and_text(image_bytes, hint.strip(), top_k=top_k)
        else:
            results = search_by_image_bytes(image_bytes, top_k=top_k)
    elif hint.strip():
        results = search_by_text(hint.strip(), top_k=top_k)
    else:
        raise HTTPException(status_code=400, detail="Provide an image or a text hint.")
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
