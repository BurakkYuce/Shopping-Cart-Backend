"""
FastAPI REST layer — port 8002.
Exposes POST /chat/ask for Spring Boot's ChatService proxy.
Shares the same LangGraph graph as the Chainlit UI.
"""
import uuid
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel

from auth.jwt_validator import decode_token
from db.engine import check_connection
from db.schema_context import get_schema_context
from graph.graph_builder import get_graph
from graph.state import AgentState
from rbac.sql_filter import get_sql_filter

app = FastAPI(title="E-Commerce Chatbot API", version="1.0.0")


class ChatRequest(BaseModel):
    message: str
    sessionId: Optional[str] = None


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
        "message_history": [],
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
