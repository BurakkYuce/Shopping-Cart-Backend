"""
Agent 5 — Visualization
Deterministic chart type selection + Plotly figure construction.
No LLM call — pure Python logic based on result shape and question keywords.
"""
import json

from graph.state import AgentState, ExecutionResult, VisualizationSpec

_DATE_KEYWORDS = ("date", "day", "month", "week", "year", "_at", "time", "period")
_RANKING_KEYWORDS = ("top", "rank", "most", "least", "best", "worst", "highest", "lowest")


def _is_numeric(val) -> bool:
    return isinstance(val, (int, float)) and not isinstance(val, bool)


def _is_date_col(col_name: str) -> bool:
    return any(kw in col_name.lower() for kw in _DATE_KEYWORDS)


def decide_chart(result: ExecutionResult, question: str) -> str:
    rows = result["rows"]
    cols = result["columns"]
    n_rows = len(rows)
    q = question.lower()

    if n_rows == 0:
        return "none"
    if n_rows == 1 and len(cols) == 1:
        return "table"  # single scalar value

    # Classify columns
    sample_row = rows[0] if rows else []
    numeric_cols = [c for c, v in zip(cols, sample_row) if _is_numeric(v)]
    date_cols = [c for c in cols if _is_date_col(c)]

    has_date = bool(date_cols)
    n_numeric = len(numeric_cols)

    if has_date and n_numeric >= 1:
        return "line"

    if any(kw in q for kw in _RANKING_KEYWORDS) and n_rows <= 20:
        return "bar"

    non_numeric = [c for c in cols if c not in numeric_cols]
    if non_numeric and n_numeric == 1 and n_rows <= 8:
        return "pie"

    if n_rows > 20 and n_numeric >= 2:
        return "scatter"

    if n_rows > 0:
        return "bar"

    return "table"


def _pick_columns(cols: list[str], rows: list[list], chart_type: str):
    """Pick x and y column names for a chart."""
    sample = rows[0] if rows else []
    numeric_cols = [c for c, v in zip(cols, sample) if _is_numeric(v)]
    non_numeric = [c for c in cols if c not in numeric_cols]
    date_cols = [c for c in cols if _is_date_col(c)]

    if chart_type == "line":
        x = date_cols[0] if date_cols else cols[0]
        y = numeric_cols[0] if numeric_cols else cols[-1]
    elif chart_type in ("bar", "pie"):
        x = non_numeric[0] if non_numeric else cols[0]
        y = numeric_cols[0] if numeric_cols else cols[-1]
    elif chart_type == "scatter":
        x = numeric_cols[0] if len(numeric_cols) >= 1 else cols[0]
        y = numeric_cols[1] if len(numeric_cols) >= 2 else cols[-1]
    else:
        x = cols[0]
        y = cols[-1] if len(cols) > 1 else cols[0]

    return x, y


def _build_plotly_json(result: ExecutionResult, chart_type: str, x_col: str, y_col: str, title: str) -> str:
    import plotly.graph_objects as go

    cols = result["columns"]
    rows = result["rows"]

    col_index = {c: i for i, c in enumerate(cols)}
    x_idx = col_index.get(x_col, 0)
    y_idx = col_index.get(y_col, len(cols) - 1)

    x_vals = [row[x_idx] for row in rows]
    y_vals = [row[y_idx] for row in rows]

    if chart_type == "bar":
        fig = go.Figure(go.Bar(x=x_vals, y=y_vals))
    elif chart_type == "line":
        fig = go.Figure(go.Scatter(x=x_vals, y=y_vals, mode="lines+markers"))
    elif chart_type == "pie":
        fig = go.Figure(go.Pie(labels=x_vals, values=y_vals))
    elif chart_type == "scatter":
        fig = go.Figure(go.Scatter(x=x_vals, y=y_vals, mode="markers"))
    else:
        # "table" — render as a Plotly table
        header_vals = cols
        cell_vals = [[row[i] for row in rows] for i in range(len(cols))]
        fig = go.Figure(go.Table(
            header=dict(values=header_vals),
            cells=dict(values=cell_vals),
        ))

    fig.update_layout(title=title, margin=dict(l=40, r=40, t=60, b=40))
    return fig.to_json()


def visualization_node(state: AgentState) -> AgentState:
    result = state["execution_result"]
    question = state["original_question"]
    analysis = state["analysis_text"]

    chart_type = decide_chart(result, question)
    vis_spec: VisualizationSpec | None = None

    if chart_type != "none":
        x_col, y_col = _pick_columns(result["columns"], result["rows"], chart_type)
        # Build a concise title from the question (first 60 chars)
        title = question[:60].strip().rstrip("?") + "?"
        plotly_json = _build_plotly_json(result, chart_type, x_col, y_col, title)

        vis_spec = VisualizationSpec(
            chart_type=chart_type,
            x_column=x_col,
            y_column=y_col,
            title=title,
            plotly_json=plotly_json,
        )

    # Assemble final_response
    rows_info = f"\n\n*({result['row_count']} rows returned{'— truncated to 500' if result.get('truncated') else ''})*"
    final_response = analysis + rows_info

    return {
        **state,
        "visualization_spec": vis_spec,
        "final_response": final_response,
    }
