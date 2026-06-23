-- liquibase formatted sql

-- changeset bukevsky:030-add-audit-target-index
CREATE INDEX idx_audit_target ON admin_audit_log(target_type, target_id, created_at DESC);
