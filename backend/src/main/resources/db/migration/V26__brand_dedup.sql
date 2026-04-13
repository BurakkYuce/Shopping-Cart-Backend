-- Create brands table
CREATE TABLE IF NOT EXISTS brands (
    id           varchar(8) PRIMARY KEY,
    display_name varchar(255) NOT NULL,
    slug         varchar(255) NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_brands_slug ON brands(slug);

-- Populate brands from existing product.brand values (uppercase-trimmed dedup)
INSERT INTO brands (id, display_name, slug)
SELECT
    substr(md5(UPPER(TRIM(brand))), 1, 8) AS id,
    TRIM(brand)                            AS display_name,
    UPPER(TRIM(brand))                     AS slug
FROM (SELECT DISTINCT brand FROM products WHERE brand IS NOT NULL AND TRIM(brand) <> '') sub
ON CONFLICT (slug) DO NOTHING;

-- Add brand_id FK column to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand_id varchar(8) REFERENCES brands(id);

-- Backfill brand_id from existing brand string
UPDATE products p
SET brand_id = b.id
FROM brands b
WHERE b.slug = UPPER(TRIM(p.brand))
  AND p.brand IS NOT NULL
  AND p.brand_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products(brand_id);
