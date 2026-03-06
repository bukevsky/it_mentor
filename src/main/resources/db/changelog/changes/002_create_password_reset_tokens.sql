--liquibase formatted sql

--changeset itmentor:002-create-password-reset-tokens-table labels:init
CREATE TABLE password_reset_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
--rollback DROP INDEX idx_password_reset_tokens_user_id; DROP TABLE password_reset_tokens;
