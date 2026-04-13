ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS seller_response varchar(2000),
    ADD COLUMN IF NOT EXISTS seller_response_date timestamp;
