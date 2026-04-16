CREATE TABLE IF NOT EXISTS questions (
    id varchar(8) PRIMARY KEY,
    product_id varchar(8) NOT NULL REFERENCES products(id),
    user_id varchar(8) NOT NULL REFERENCES users(id),
    question varchar(2000) NOT NULL,
    answer varchar(2000),
    answered_by_user_id varchar(8) REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    answered_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_questions_product_id ON questions(product_id);
CREATE INDEX IF NOT EXISTS idx_questions_user_id ON questions(user_id);
