-- V14: Normalize orders.status to OrderStatus enum values
-- Collapses legacy lowercase + "return_requested" variants into canonical PENDING,
-- PROCESSING, SHIPPED, DELIVERED, RETURNED, CANCELLED set expected by the state machine.
UPDATE orders
SET status = CASE
    WHEN status IS NULL OR TRIM(status) = '' THEN 'PENDING'
    WHEN UPPER(TRIM(status)) IN ('PENDING', 'NEW', 'CREATED') THEN 'PENDING'
    WHEN UPPER(TRIM(status)) IN ('PROCESSING', 'CONFIRMED', 'PAID') THEN 'PROCESSING'
    WHEN UPPER(TRIM(status)) IN ('SHIPPED', 'IN_TRANSIT', 'IN TRANSIT', 'DISPATCHED') THEN 'SHIPPED'
    WHEN UPPER(TRIM(status)) IN ('DELIVERED', 'COMPLETED') THEN 'DELIVERED'
    WHEN UPPER(TRIM(status)) IN ('RETURNED', 'RETURN_REQUESTED', 'RETURN REQUESTED', 'REFUNDED') THEN 'RETURNED'
    WHEN UPPER(TRIM(status)) IN ('CANCELLED', 'CANCELED', 'VOID') THEN 'CANCELLED'
    ELSE 'PENDING'
END;
