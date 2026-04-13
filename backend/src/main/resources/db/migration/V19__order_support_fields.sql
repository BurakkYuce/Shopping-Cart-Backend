ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS tracking_number varchar(100),
    ADD COLUMN IF NOT EXISTS carrier varchar(50),
    ADD COLUMN IF NOT EXISTS delivered_at timestamp,
    ADD COLUMN IF NOT EXISTS return_deadline timestamp,
    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500),
    ADD COLUMN IF NOT EXISTS customer_notes varchar(1000),
    ADD COLUMN IF NOT EXISTS refunded_at timestamp;
