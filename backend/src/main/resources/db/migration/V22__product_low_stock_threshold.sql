ALTER TABLE products ADD COLUMN IF NOT EXISTS low_stock_threshold integer DEFAULT 10;
