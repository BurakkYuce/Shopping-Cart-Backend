"""
Provides a curated, LLM-optimised schema description for the e-commerce database.
The static annotations below supplement SQLAlchemy introspection with business context
and FK relationships that the LLM needs to write correct JOIN queries.
"""

SCHEMA_CONTEXT = """
TABLE: users
  Columns: id (VARCHAR PK), email (VARCHAR UNIQUE), role_type (VARCHAR: ADMIN|CORPORATE|INDIVIDUAL), gender (VARCHAR)
  Note: The column `password_hash` EXISTS but must NEVER be queried or returned. Treat it as non-existent.

TABLE: stores
  Columns: id (VARCHAR PK), owner_id (FK → users.id), name (VARCHAR), status (VARCHAR)

TABLE: customer_profiles
  Columns: id (VARCHAR PK), user_id (FK → users.id UNIQUE), age (INT), city (VARCHAR),
           membership_type (VARCHAR), total_spend (DECIMAL), items_purchased (INT),
           average_rating (DECIMAL), satisfaction_level (VARCHAR)

TABLE: categories
  Columns: id (VARCHAR PK), name (VARCHAR), parent_id (FK → categories.id, nullable — null means top-level)

TABLE: products
  Columns: id (VARCHAR PK), store_id (FK → stores.id), category_id (FK → categories.id),
           sku (VARCHAR), name (VARCHAR), unit_price (DECIMAL), description (TEXT)

TABLE: orders
  Columns: id (VARCHAR PK), user_id (FK → users.id), store_id (FK → stores.id),
           status (VARCHAR: e.g. delivered, pending, cancelled), grand_total (DECIMAL),
           created_at (TIMESTAMP), payment_method (VARCHAR)

TABLE: order_items
  Columns: id (VARCHAR PK), order_id (FK → orders.id), product_id (FK → products.id),
           quantity (INT), price (DECIMAL)
  Note: Each row is one line item. Revenue = SUM(price), units = SUM(quantity).

TABLE: shipments
  Columns: id (VARCHAR PK), order_id (FK → orders.id), warehouse (VARCHAR), mode (VARCHAR),
           status (VARCHAR), customer_care_calls (INT), customer_rating (INT 1-5), weight_gms (INT)
  Note: MULTIPLE shipments can exist per order. Always aggregate (MAX, COUNT, etc.) or use a subquery
        to avoid row duplication when joining shipments to orders.

TABLE: reviews
  Columns: id (VARCHAR PK), user_id (FK → users.id), product_id (FK → products.id),
           star_rating (INT 1-5), helpful_votes (INT), total_votes (INT),
           review_headline (VARCHAR), review_text (TEXT), sentiment (VARCHAR: positive|neutral|negative),
           verified_purchase (VARCHAR: Y|N), review_date (DATE)

COMMON JOIN PATTERNS:
  - Revenue by product: orders o → order_items oi ON oi.order_id = o.id → products p ON p.id = oi.product_id
  - Category ratings: products p → categories c ON c.id = p.category_id → reviews r ON r.product_id = p.id
  - Customer spend: users u → customer_profiles cp ON cp.user_id = u.id
  - Shipping analysis: orders o → shipments s ON s.order_id = o.id (use DISTINCT or aggregate due to multiple shipments)
  - Top products by quantity: order_items oi JOIN products p ON p.id = oi.product_id GROUP BY p.name ORDER BY SUM(oi.quantity) DESC

IMPORTANT — HUMAN-READABLE NAMES:
  - NEVER use raw IDs (product_id, category_id, store_id) as display columns in query results.
  - Always JOIN the referenced table to include the human-readable name (p.name, c.name, s.name).
  - Example: instead of `SELECT oi.product_id, SUM(oi.quantity)` use `SELECT p.name, SUM(oi.quantity) ... JOIN products p ON p.id = oi.product_id`.

GENERAL SQL RULES:
  - Always use explicit table aliases (e.g. orders o, order_items oi)
  - Prefer explicit column names over SELECT *
  - Always include LIMIT (max 500) unless aggregating to a small result set
  - Use CAST(... AS DECIMAL) when dividing integers
  - Date filtering example: WHERE o.created_at >= '2024-01-01' AND o.created_at < '2025-01-01'
"""


_DYNAMIC_CONTEXT_CACHE: str | None = None


def _load_dynamic_context() -> str:
    """Fetch real category names and top brands from DB at startup."""
    try:
        from db.executor import execute_query
        categories = execute_query("SELECT DISTINCT name FROM categories ORDER BY name")
        brands = execute_query("SELECT DISTINCT brand FROM products WHERE brand IS NOT NULL ORDER BY brand LIMIT 50")
        cat_names = [row.get("name", "") for row in categories if row.get("name")]
        brand_names = [row.get("brand", "") for row in brands if row.get("brand")]
        parts = []
        if cat_names:
            parts.append(f"\nKNOWN CATEGORIES: {', '.join(cat_names)}")
        if brand_names:
            parts.append(f"\nKNOWN BRANDS (top 50): {', '.join(brand_names)}")
        if parts:
            parts.append("\nUse these exact names when filtering by category or brand (case-insensitive ILIKE is acceptable).")
        return "\n".join(parts)
    except Exception:
        return ""


def get_schema_context() -> str:
    global _DYNAMIC_CONTEXT_CACHE
    if _DYNAMIC_CONTEXT_CACHE is None:
        _DYNAMIC_CONTEXT_CACHE = SCHEMA_CONTEXT.strip() + _load_dynamic_context()
    return _DYNAMIC_CONTEXT_CACHE
