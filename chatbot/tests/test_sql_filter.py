"""Unit tests for role-based SQL CTE filter."""
import pytest
from rbac.sql_filter import SqlFilter


@pytest.fixture
def f():
    return SqlFilter()


def test_admin_empty_cte(f):
    assert f.build_cte("ADMIN", "any-id") == ""


def test_corporate_cte_contains_store_filter(f):
    cte = f.build_cte("CORPORATE", "owner-123")
    assert "owner_id = 'owner-123'" in cte
    assert "_allowed_stores" in cte
    assert "_allowed_orders" in cte
    assert "_allowed_products" in cte


def test_individual_cte_contains_user_filter(f):
    cte = f.build_cte("INDIVIDUAL", "user-456")
    assert "user_id = 'user-456'" in cte
    assert "_allowed_orders" in cte
    assert "_allowed_reviews" in cte
    assert "_allowed_profile" in cte


def test_admin_cte_validates_without_aliases(f):
    assert f.validate_cte_present("SELECT * FROM orders", "ADMIN") is True


def test_corporate_missing_cte_detected(f):
    assert f.validate_cte_present("SELECT * FROM orders", "CORPORATE") is False


def test_corporate_valid_cte_passes(f):
    sql = "WITH _allowed_orders AS (SELECT * FROM orders) SELECT * FROM _allowed_orders"
    assert f.validate_cte_present(sql, "CORPORATE") is True


def test_inject_cte_if_missing(f):
    bare_sql = "SELECT * FROM orders LIMIT 10"
    injected = f.inject_cte_if_missing(bare_sql, "INDIVIDUAL", "user-789")
    assert "_allowed_" in injected
    assert "SELECT * FROM orders" in injected  # original query preserved


def test_unknown_role_raises(f):
    with pytest.raises(ValueError):
        f.build_cte("SUPERUSER", "x")
