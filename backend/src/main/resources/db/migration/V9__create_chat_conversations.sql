-- V9: Chat conversation persistence

CREATE TABLE IF NOT EXISTS chat_conversations (
    id          VARCHAR(255) PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    title       VARCHAR(500),
    created_at  TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_chat_conv_user ON chat_conversations(user_id);
CREATE INDEX idx_chat_conv_updated ON chat_conversations(updated_at DESC);

CREATE TABLE IF NOT EXISTS chat_messages (
    id              VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT         NOT NULL,
    plotly_json     TEXT,
    generated_sql   TEXT,
    intent          VARCHAR(50),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_messages_conv FOREIGN KEY (conversation_id)
        REFERENCES chat_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_msg_conv ON chat_messages(conversation_id);

-- Grant read access to chatbot service (keeps chatbot_reader role read-only)
GRANT SELECT ON chat_conversations TO chatbot_reader;
GRANT SELECT ON chat_messages TO chatbot_reader;
