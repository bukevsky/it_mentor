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

--changeset itmentor:002-comments-password-reset-tokens labels:init
COMMENT ON TABLE password_reset_tokens IS 'Токены для сброса пароля пользователя';
COMMENT ON COLUMN password_reset_tokens.id IS 'Первичный ключ';
COMMENT ON COLUMN password_reset_tokens.user_id IS 'Ссылка на пользователя, запросившего сброс пароля';
COMMENT ON COLUMN password_reset_tokens.token IS 'Уникальный токен сброса пароля (UUID)';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Дата и время истечения токена';
COMMENT ON COLUMN password_reset_tokens.used IS 'Флаг использования токена (однократное применение)';
COMMENT ON COLUMN password_reset_tokens.created_at IS 'Дата и время создания токена';
--rollback SELECT 1;
