"""
Role-based SQL access control via mandatory leading CTE blocks.

The LLM system prompt instructs the model to always prepend these CTEs and
use their aliases (_allowed_orders, _allowed_products, etc.) instead of base tables.

ADMIN:      no restriction — empty CTE string
CORPORATE:  scoped to stores owned by user_id (orders, products, order_items, reviews)
INDIVIDUAL: scoped to user's own orders, reviews, cart_items, order_items
"""
import re

CORPORATE_CTE = """WITH _allowed_stores AS (
    SELECT id AS store_id FROM stores WHERE owner_id = '{user_id}'
),
_allowed_orders AS (
    SELECT o.* FROM orders o
    JOIN _allowed_stores s ON o.store_id = s.store_id
),
_allowed_products AS (
    SELECT p.* FROM products p
    JOIN _allowed_stores s ON p.store_id = s.store_id
),
_allowed_order_items AS (
    SELECT oi.* FROM order_items oi
    JOIN _allowed_orders ao ON oi.order_id = ao.id
),
_allowed_reviews AS (
    SELECT r.* FROM reviews r
    JOIN _allowed_products ap ON r.product_id = ap.id
)"""

INDIVIDUAL_CTE = """WITH _allowed_orders AS (
    SELECT * FROM orders WHERE user_id = '{user_id}'
),
_allowed_reviews AS (
    SELECT * FROM reviews WHERE user_id = '{user_id}'
),
_allowed_cart_items AS (
    SELECT * FROM cart_items WHERE user_id = '{user_id}'
),
_allowed_order_items AS (
    SELECT oi.* FROM order_items oi
    JOIN _allowed_orders ao ON oi.order_id = ao.id
)"""

# Instructions appended to the SQL generator system prompt
CORPORATE_USAGE_HINT = (
    "When querying orders → use `_allowed_orders` instead of `orders`. "
    "When querying products → use `_allowed_products` instead of `products`. "
    "When querying stores → use `_allowed_stores` instead of `stores`. "
    "When querying order_items → use `_allowed_order_items` instead of `order_items`. "
    "When querying reviews → use `_allowed_reviews` instead of `reviews`. "
    "Do NOT query the base tables directly."
)

INDIVIDUAL_USAGE_HINT = (
    "When querying orders → use `_allowed_orders` instead of `orders`. "
    "When querying reviews → use `_allowed_reviews` instead of `reviews`. "
    "When querying cart_items → use `_allowed_cart_items` instead of `cart_items`. "
    "When querying order_items → use `_allowed_order_items` instead of `order_items`. "
    "Do NOT query the base tables directly."
)


class SqlFilter:
    def build_cte(self, role: str, user_id: str) -> str:
        """Return the CTE block string to prepend to the generated SQL."""
        if role == "ADMIN":
            return ""
        if role == "CORPORATE":
            return CORPORATE_CTE.format(user_id=user_id)
        if role == "INDIVIDUAL":
            return INDIVIDUAL_CTE.format(user_id=user_id)
        raise ValueError(f"Unknown role: {role}")

    def build_usage_hint(self, role: str) -> str:
        """Return the usage hint injected into the SQL generator prompt."""
        if role == "ADMIN":
            return "You have full access to all tables."
        if role == "CORPORATE":
            return CORPORATE_USAGE_HINT
        if role == "INDIVIDUAL":
            return INDIVIDUAL_USAGE_HINT
        raise ValueError(f"Unknown role: {role}")

    def validate_cte_present(self, sql: str, role: str) -> bool:
        """Check that the generated SQL uses the required CTE aliases."""
        if role == "ADMIN":
            return True
        return "_allowed_" in sql.lower()

    def inject_cte_if_missing(self, sql: str, role: str, user_id: str) -> str:
        """If CTE is missing (e.g. LLM forgot), prepend it automatically."""
        if self.validate_cte_present(sql, role):
            return sql
        cte = self.build_cte(role, user_id)
        if not cte:
            return sql
        # Replace leading SELECT/WITH
        stripped = sql.strip()
        return cte + "\n" + stripped

    # Per-role forbidden base tables in the main SELECT body. Every entry here
    # has an `_allowed_` alias that the LLM should use instead. `users` is
    # added universally because no non-admin role should read it directly,
    # even for joins (it holds password_hash). Tables not listed (brands,
    # categories, wishlist_items, coupons, shipments, etc.) are public
    # catalog or joined via an allowed alias — safe to reference as base tables.
    _FORBIDDEN_PER_ROLE = {
        "INDIVIDUAL": ["orders", "reviews", "cart_items", "order_items", "users"],
        "CORPORATE": ["orders", "products", "stores", "order_items", "reviews", "users"],
    }

    def enforce_scope(self, sql: str, role: str) -> str:
        """Reject queries that reach past the CTE aliases for scoped roles.

        The LLM sometimes defines the CTE correctly but then writes the main
        SELECT against a base table (e.g. `FROM orders o` instead of
        `FROM _allowed_orders o`). That silently returns the whole table —
        a real cross-role leak, not a theoretical one. This pass walks
        paren-matched CTE headers, then scans the body for any base-table
        reference, raising a ValueError if one is found.
        """
        if role == "ADMIN":
            return sql
        forbidden = self._FORBIDDEN_PER_ROLE.get(role, [])
        if not forbidden:
            return sql
        body = self._split_body(sql)
        for tbl in forbidden:
            if re.search(rf"\b(?:FROM|JOIN)\s+{tbl}\b", body, re.IGNORECASE):
                raise ValueError(
                    f"Role '{role}' cannot read base table '{tbl}'. "
                    f"Use _allowed_{tbl}."
                )
        return sql

    @staticmethod
    def _split_body(sql: str) -> str:
        """Return the portion of `sql` after the leading WITH-CTE block.

        Uses paren matching so nested SELECTs inside CTE bodies don't confuse us.
        If there's no CTE, the whole query is the body.
        """
        m = re.match(r"^\s*WITH\s", sql, re.IGNORECASE)
        if not m:
            return sql
        i = m.end()
        depth = 0
        n = len(sql)
        while i < n:
            c = sql[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    rest = sql[i + 1:].lstrip()
                    # Another CTE in the list?
                    if rest.startswith(","):
                        # Skip past the comma and keep walking
                        comma_idx = sql.index(",", i + 1)
                        i = comma_idx + 1
                        continue
                    # Otherwise we're at the body
                    return sql[i + 1:]
            i += 1
        return ""


_filter = SqlFilter()


def get_sql_filter() -> SqlFilter:
    return _filter
