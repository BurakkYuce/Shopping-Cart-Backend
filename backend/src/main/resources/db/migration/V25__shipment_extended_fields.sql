ALTER TABLE shipments
    ADD COLUMN IF NOT EXISTS warehouse_block  varchar(10),
    ADD COLUMN IF NOT EXISTS cost             double precision,
    ADD COLUMN IF NOT EXISTS prior_purchases  integer,
    ADD COLUMN IF NOT EXISTS importance       varchar(20),
    ADD COLUMN IF NOT EXISTS discount_offered double precision;
