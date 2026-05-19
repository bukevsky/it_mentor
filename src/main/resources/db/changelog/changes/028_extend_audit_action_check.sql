--liquibase formatted sql

--changeset bukevsky:028-1
ALTER TABLE admin_audit_log DROP CONSTRAINT IF EXISTS admin_audit_log_action_check;

--changeset bukevsky:028-2
ALTER TABLE admin_audit_log
    ADD CONSTRAINT admin_audit_log_action_check
        CHECK (action IN (
            'ROLE_CHANGED',
            'REVIEW_MODERATED',
            'COMPLAINT_RESOLVED',
            'USER_STATUS_CHANGED',
            'DICTIONARY_CHANGED'
        ));
