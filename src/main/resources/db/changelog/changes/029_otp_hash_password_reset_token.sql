--liquibase formatted sql

--changeset itmentor:029-otp-hash
-- Invalidate all existing plain-text OTP tokens before dropping the column
UPDATE password_reset_tokens SET used = TRUE;
ALTER TABLE password_reset_tokens
    ADD COLUMN token_hash VARCHAR(60) NOT NULL DEFAULT '',
    DROP COLUMN token;
ALTER TABLE password_reset_tokens ALTER COLUMN token_hash DROP DEFAULT;

COMMENT ON COLUMN password_reset_tokens.token_hash IS 'BCrypt-хеш одноразового OTP-кода сброса пароля';
--rollback ALTER TABLE password_reset_tokens ADD COLUMN token VARCHAR(6) NOT NULL DEFAULT '', DROP COLUMN token_hash; ALTER TABLE password_reset_tokens ALTER COLUMN token DROP DEFAULT; UPDATE password_reset_tokens SET used = TRUE;
