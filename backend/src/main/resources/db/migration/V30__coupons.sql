-- Coupon system tables

CREATE TABLE coupons (
    id              VARCHAR(8) PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    type            VARCHAR(20) NOT NULL,
    value           DOUBLE PRECISION NOT NULL,
    description     VARCHAR(255),
    min_order_amount DOUBLE PRECISION DEFAULT 0,
    max_discount    DOUBLE PRECISION,
    max_uses        INT,
    current_uses    INT DEFAULT 0,
    valid_from      TIMESTAMP,
    valid_to        TIMESTAMP,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE coupon_usages (
    id          VARCHAR(8) PRIMARY KEY,
    coupon_id   VARCHAR(8) NOT NULL REFERENCES coupons(id),
    user_id     VARCHAR(255) NOT NULL REFERENCES users(id),
    order_id    VARCHAR(8) NOT NULL REFERENCES orders(id),
    used_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE(coupon_id, user_id)
);

-- Add discount fields to orders
ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50);
ALTER TABLE orders ADD COLUMN discount_amount DOUBLE PRECISION DEFAULT 0;

-- Seed demo coupons
INSERT INTO coupons (id, code, type, value, description, min_order_amount, max_discount, max_uses, current_uses, valid_from, valid_to, active, created_at)
VALUES
    ('cpn00001', 'WELCOME10', 'PERCENTAGE', 10, 'Welcome discount - 10% off', 0, 50, NULL, 0, '2024-01-01', '2027-12-31', TRUE, NOW()),
    ('cpn00002', 'SAVE20',    'FIXED_AMOUNT', 20, '$20 off orders over $50', 50, NULL, NULL, 0, '2024-01-01', '2027-12-31', TRUE, NOW()),
    ('cpn00003', 'VIP50',     'PERCENTAGE', 50, 'VIP 50% discount (max $200)', 100, 200, 100, 0, '2024-01-01', '2027-12-31', TRUE, NOW()),
    ('cpn00004', 'CEREN100',  'PERCENTAGE', 100, 'Full discount - 100% off', 0, NULL, NULL, 0, '2024-01-01', '2027-12-31', TRUE, NOW());
