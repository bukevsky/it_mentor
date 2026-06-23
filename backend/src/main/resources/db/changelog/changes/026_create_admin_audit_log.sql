--liquibase formatted sql

--changeset bukevsky:026-create-admin-audit-log
CREATE TABLE admin_audit_log (
    id            BIGSERIAL    PRIMARY KEY,
    admin_user_id BIGINT       NOT NULL REFERENCES users(id),
    action        VARCHAR(40)  NOT NULL CHECK (action IN ('ROLE_CHANGED','REVIEW_MODERATED','COMPLAINT_RESOLVED')),
    target_type   VARCHAR(30)  NOT NULL,
    target_id     BIGINT,
    payload       JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_admin_created ON admin_audit_log(admin_user_id, created_at DESC);
CREATE INDEX idx_audit_action_created ON admin_audit_log(action, created_at DESC);
CREATE INDEX idx_audit_created ON admin_audit_log(created_at DESC);
