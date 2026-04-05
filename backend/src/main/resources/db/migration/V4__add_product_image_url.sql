-- V4: Add image URL column to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);
