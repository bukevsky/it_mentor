--liquibase formatted sql

--changeset itmentor:013-otp-password-reset
ALTER TABLE password_reset_tokens
    ALTER COLUMN token TYPE VARCHAR(6),
    DROP CONSTRAINT IF EXISTS password_reset_tokens_token_key,
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_prt_user_active ON password_reset_tokens (user_id, used, expires_at)
    WHERE used = FALSE;
--rollback ALTER TABLE password_reset_tokens ALTER COLUMN token TYPE VARCHAR(255), ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token), DROP COLUMN IF EXISTS attempts; DROP INDEX IF EXISTS idx_prt_user_active;

--changeset itmentor:013-otp-comments
COMMENT ON COLUMN password_reset_tokens.token IS 'Одноразовый 6-значный OTP-код сброса пароля';
COMMENT ON COLUMN password_reset_tokens.attempts IS 'Количество неудачных попыток ввода кода';
--rollback SELECT 1;
