CREATE TABLE IF NOT EXISTS audit_logs (
    id          varchar(8) PRIMARY KEY,
    user_id     varchar(255),
    action      varchar(100) NOT NULL,
    resource    varchar(200),
    details     text,
    ip_address  varchar(45),
    created_at  timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
