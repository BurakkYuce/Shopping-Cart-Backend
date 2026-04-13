ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS currency varchar(3) DEFAULT 'TRY',
    ADD COLUMN IF NOT EXISTS exchange_rate double precision DEFAULT 1.0;
