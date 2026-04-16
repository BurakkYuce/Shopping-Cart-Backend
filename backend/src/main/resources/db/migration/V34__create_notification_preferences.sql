CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id        VARCHAR(64)  PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    order_updates  BOOLEAN      NOT NULL DEFAULT TRUE,
    new_arrivals   BOOLEAN      NOT NULL DEFAULT TRUE,
    promotions     BOOLEAN      NOT NULL DEFAULT FALSE,
    newsletter     BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
