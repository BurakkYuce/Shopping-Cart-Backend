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
  - Top products by quantity: order_items GROUP BY product_id ORDER BY SUM(quantity) DESC

GENERAL SQL RULES:
  - Always use explicit table aliases (e.g. orders o, order_items oi)
  - Prefer explicit column names over SELECT *
  - Always include LIMIT (max 500) unless aggregating to a small result set
  - Use CAST(... AS DECIMAL) when dividing integers
  - Date filtering example: WHERE o.created_at >= '2024-01-01' AND o.created_at < '2025-01-01'
"""


def get_schema_context() -> str:
    return SCHEMA_CONTEXT.strip()
