--liquibase formatted sql

--changeset bukevsky:020-create-user-presence
CREATE TABLE user_presence (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    last_seen_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE user_presence;

--changeset bukevsky:020-user-presence-index
CREATE INDEX idx_user_presence_user ON user_presence (user_id);
--rollback DROP INDEX idx_user_presence_user;
