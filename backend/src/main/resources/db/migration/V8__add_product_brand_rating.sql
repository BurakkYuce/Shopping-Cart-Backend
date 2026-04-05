-- V8: Add brand, rating, retail_price columns for Amazon product compatibility
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS rating DOUBLE PRECISION;
ALTER TABLE products ADD COLUMN IF NOT EXISTS retail_price DOUBLE PRECISION;
