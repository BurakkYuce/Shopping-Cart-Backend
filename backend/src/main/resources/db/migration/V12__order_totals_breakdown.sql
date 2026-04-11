-- V12: Order totals breakdown (subtotal + tax_amount) to support KDV line
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_amount DOUBLE PRECISION;

-- Backfill: pre-KDV orders had no tax, treat grand_total as the tax-free subtotal
UPDATE orders
SET subtotal = grand_total,
    tax_amount = 0
WHERE subtotal IS NULL;
