CREATE TABLE IF NOT EXISTS return_requests (
    id varchar(8) PRIMARY KEY,
    order_id varchar(8) NOT NULL REFERENCES orders(id),
    user_id varchar(8) NOT NULL REFERENCES users(id),
    reason varchar(1000) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    created_at timestamp NOT NULL DEFAULT now(),
    resolved_at timestamp,
    admin_notes varchar(1000)
);

CREATE INDEX IF NOT EXISTS idx_return_requests_order_id ON return_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_user_id ON return_requests(user_id);
