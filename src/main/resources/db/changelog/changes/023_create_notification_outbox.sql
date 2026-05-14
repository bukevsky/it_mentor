--liquibase formatted sql

--changeset bukevsky:023-create-notification-outbox
CREATE TABLE notification_outbox (
    id                BIGSERIAL    PRIMARY KEY,
    recipient_email   VARCHAR(255) NOT NULL,
    subject           VARCHAR(500) NOT NULL,
    html_body         TEXT         NOT NULL,
    text_body         TEXT         NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    attempts          INT          NOT NULL DEFAULT 0,
    last_error        TEXT,
    next_attempt_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON notification_outbox(status, next_attempt_at)
    WHERE status = 'PENDING';
