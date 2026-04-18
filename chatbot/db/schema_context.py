"""
Provides a curated, LLM-optimised schema description for the e-commerce database.
The static annotations below supplement SQLAlchemy introspection with business context
and FK relationships that the LLM needs to write correct JOIN queries.
"""

SCHEMA_CONTEXT = """
## Business Glossary (canonical metric definitions)

- Revenue           = SUM(order_items.price * order_items.quantity)
                      (`order_items.price` is the per-unit sale price captured at checkout;
                      `products.unit_price` is the current catalog price — do NOT use it for
                      historical revenue.)
- Order total       = orders.grand_total (includes tax + shipping; differs from item subtotal)
- Units sold        = SUM(order_items.quantity)
- Order count       = COUNT(DISTINCT orders.id) across ALL statuses (PENDING, CANCELLED, RETURNED
                      included) — neutral question "kaç sipariş var"
- Successful orders = COUNT(DISTINCT orders.id) WHERE status IN
                      ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED')
- Active customer   = a user with ≥1 DELIVERED order in the last 30 days
- Cancelled / Returned / Pending orders contribute ZERO to revenue.

TABLE: users
  Columns: id (VARCHAR PK), email (VARCHAR UNIQUE), role_type (VARCHAR: ADMIN|CORPORATE|INDIVIDUAL), gender (VARCHAR)
  Note: The column `password_hash` EXISTS but must NEVER be queried or returned. Treat it as non-existent.

TABLE: stores
  Columns: id (VARCHAR PK), owner_id (FK → users.id), name (VARCHAR), status (VARCHAR)

TABLE: categories
  Columns: id (VARCHAR PK), name (VARCHAR), parent_id (FK → categories.id, nullable — null means top-level)

TABLE: brands
  Columns: id (VARCHAR PK), display_name (VARCHAR), slug (VARCHAR UNIQUE)
  Note: Normalised brand master data. JOIN via products.brand_id.

TABLE: products
  Columns: id (VARCHAR PK), store_id (FK → stores.id), category_id (FK → categories.id),
           brand_id (FK → brands.id, nullable), brand (VARCHAR — legacy text, prefer JOIN brands),
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

TABLE: cart_items
  Columns: id (VARCHAR PK), user_id (FK → users.id), product_id (FK → products.id), quantity (INT)
  Note: Active shopping cart. Each row is one product in a user's bag. JOIN products for names/prices.
  UNIQUE constraint: (user_id, product_id) — one row per product per user.

TABLE: wishlist_items
  Columns: id (VARCHAR PK), user_id (FK → users.id), product_id (FK → products.id)
  Note: User's saved/favorited products. JOIN products for details.

TABLE: reviews
  Columns: id (VARCHAR PK), user_id (FK → users.id), product_id (FK → products.id),
           star_rating (INT 1-5), helpful_votes (INT), total_votes (INT),
           review_headline (VARCHAR), review_text (TEXT), sentiment (VARCHAR: positive|neutral|negative),
           verified_purchase (VARCHAR: Y|N), review_date (DATE),
           seller_response (VARCHAR, nullable), seller_response_date (TIMESTAMP, nullable)

TABLE: coupons
  Columns: id (VARCHAR PK), code (VARCHAR UNIQUE), type (VARCHAR: PERCENTAGE|FIXED_AMOUNT),
           value (DECIMAL), description (VARCHAR), min_order_amount (DECIMAL),
           max_discount (DECIMAL, nullable), max_uses (INT, nullable), current_uses (INT),
           valid_from (TIMESTAMP), valid_to (TIMESTAMP), active (BOOLEAN)
  Note: Platform-wide discount coupons created by admins.

COMMON JOIN PATTERNS:
  - Revenue by product: orders o → order_items oi ON oi.order_id = o.id → products p ON p.id = oi.product_id
  - Category ratings: products p → categories c ON c.id = p.category_id → reviews r ON r.product_id = p.id
  - Shipping analysis: orders o → shipments s ON s.order_id = o.id (use DISTINCT or aggregate due to multiple shipments)
  - Top products by quantity: order_items oi JOIN products p ON p.id = oi.product_id GROUP BY p.name ORDER BY SUM(oi.quantity) DESC
  - User's cart: cart_items ci JOIN products p ON p.id = ci.product_id JOIN stores s ON s.id = p.store_id WHERE ci.user_id = '<userId>'
  - User's wishlist: wishlist_items wi JOIN products p ON p.id = wi.product_id WHERE wi.user_id = '<userId>'
  - Products by brand: products p JOIN brands b ON b.id = p.brand_id WHERE b.display_name ILIKE '%Nike%'
  - Active coupons: SELECT code, type, value, description FROM coupons WHERE active = TRUE AND (valid_to IS NULL OR valid_to > NOW())

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
