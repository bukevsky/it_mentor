--liquibase formatted sql

--changeset bukevsky:022-create-notification-preferences
CREATE TABLE user_notification_preferences (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_request_events  BOOLEAN NOT NULL DEFAULT TRUE,
    email_session_events  BOOLEAN NOT NULL DEFAULT TRUE,
    email_review_events   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_unp_user ON user_notification_preferences(user_id);
