CREATE TABLE IF NOT EXISTS complaints (
    id varchar(8) PRIMARY KEY,
    order_id varchar(8) NOT NULL REFERENCES orders(id),
    user_id varchar(8) NOT NULL REFERENCES users(id),
    subject varchar(200) NOT NULL,
    message varchar(2000) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'OPEN',
    created_at timestamp NOT NULL DEFAULT now(),
    resolved_at timestamp,
    resolution_notes varchar(1000)
);

CREATE INDEX IF NOT EXISTS idx_complaints_order_id ON complaints(order_id);
CREATE INDEX IF NOT EXISTS idx_complaints_user_id ON complaints(user_id);
