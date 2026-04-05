"""
Role-based SQL access control via mandatory leading CTE blocks.

The LLM system prompt instructs the model to always prepend these CTEs and
use their aliases (_allowed_orders, _allowed_products, etc.) instead of base tables.

ADMIN:      no restriction — empty CTE string
CORPORATE:  scoped to stores owned by user_id
INDIVIDUAL: scoped to user's own orders, reviews, profile
"""

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
)"""

INDIVIDUAL_CTE = """WITH _allowed_orders AS (
    SELECT * FROM orders WHERE user_id = '{user_id}'
),
_allowed_reviews AS (
    SELECT * FROM reviews WHERE user_id = '{user_id}'
),
_allowed_profile AS (
    SELECT * FROM customer_profiles WHERE user_id = '{user_id}'
)"""

# Instructions appended to the SQL generator system prompt
CORPORATE_USAGE_HINT = (
    "When querying orders → use `_allowed_orders` instead of `orders`. "
    "When querying products → use `_allowed_products` instead of `products`. "
    "When querying stores → use `_allowed_stores` instead of `stores`. "
    "Do NOT query the base tables directly."
)

INDIVIDUAL_USAGE_HINT = (
    "When querying orders → use `_allowed_orders` instead of `orders`. "
    "When querying reviews → use `_allowed_reviews` instead of `reviews`. "
    "When querying customer_profiles → use `_allowed_profile` instead of `customer_profiles`. "
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


_filter = SqlFilter()


def get_sql_filter() -> SqlFilter:
    return _filter
