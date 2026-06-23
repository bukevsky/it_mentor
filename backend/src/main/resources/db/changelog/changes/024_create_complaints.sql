--liquibase formatted sql

--changeset bukevsky:024-create-complaints
CREATE TABLE complaints (
    id               BIGSERIAL    PRIMARY KEY,
    target_type      VARCHAR(20)  NOT NULL CHECK (target_type IN ('REVIEW','USER')),
    target_id        BIGINT       NOT NULL,
    reporter_user_id BIGINT       NOT NULL REFERENCES users(id),
    reason           TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED','REJECTED')),
    resolution       TEXT,
    resolved_by      BIGINT       REFERENCES users(id),
    resolved_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_target ON complaints(target_type, target_id);
CREATE INDEX idx_complaints_reporter ON complaints(reporter_user_id);
