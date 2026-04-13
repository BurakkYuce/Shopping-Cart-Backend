ALTER TABLE customer_profiles
    ADD COLUMN IF NOT EXISTS discount_applied varchar(10);
