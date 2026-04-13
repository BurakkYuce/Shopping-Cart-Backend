ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS email_verification_token varchar(64),
    ADD COLUMN IF NOT EXISTS email_verification_expires_at timestamp;

-- Backfill all existing (seeded) users as already verified
UPDATE users SET email_verified = true WHERE email_verified = false;

CREATE INDEX IF NOT EXISTS idx_users_email_verification_token ON users(email_verification_token);
