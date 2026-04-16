-- Seller-side notification flags. Every user row can carry these; they're only
-- read/dispatched for users with CORPORATE role (store owners). Defaults are TRUE
-- so that new sellers start fully opted-in and can turn things off from settings.
ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS new_order_seller    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS low_stock_alert     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS new_review_alert    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS weekly_store_digest BOOLEAN NOT NULL DEFAULT TRUE;
