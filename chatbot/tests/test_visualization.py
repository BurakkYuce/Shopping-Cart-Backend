"""Unit tests for the deterministic chart-type decision function."""
import pytest
from graph.nodes.visualization import decide_chart
from graph.state import ExecutionResult


def _result(cols, rows) -> ExecutionResult:
    return ExecutionResult(columns=cols, rows=rows, row_count=len(rows), truncated=False)


def test_empty_result_is_none():
    r = _result(["product", "revenue"], [])
    assert decide_chart(r, "top products") == "none"


def test_single_value_is_table():
    r = _result(["total"], [[42.5]])
    assert decide_chart(r, "what is total revenue") == "table"


def test_date_plus_numeric_is_line():
    rows = [["2024-01-01", 100.0], ["2024-01-02", 200.0]]
    r = _result(["order_date", "revenue"], rows)
    assert decide_chart(r, "show revenue over time") == "line"


def test_top_ranking_bar():
    rows = [[f"product_{i}", float(i * 10)] for i in range(10)]
    r = _result(["product_name", "quantity"], rows)
    assert decide_chart(r, "top 10 products by quantity") == "bar"


def test_small_categorical_is_pie():
    rows = [["delivered", 50], ["pending", 20], ["cancelled", 5]]
    r = _result(["status", "count"], rows)
    assert decide_chart(r, "order distribution by status") == "pie"


def test_large_scatter():
    rows = [[float(i), float(i * 2)] for i in range(30)]
    r = _result(["total_spend", "average_rating"], rows)
    assert decide_chart(r, "spend vs rating") == "scatter"
