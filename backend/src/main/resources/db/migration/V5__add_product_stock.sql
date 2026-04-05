-- V5: Add stock quantity to products (default 9999 for existing products)
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_quantity INTEGER NOT NULL DEFAULT 9999;
