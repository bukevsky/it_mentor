--liquibase formatted sql
--changeset bukevsky:012-create-mentoring-requests
CREATE TABLE mentoring_requests (
    id                   BIGSERIAL   PRIMARY KEY,
    student_profile_id   BIGINT      NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    mentor_profile_id    BIGINT      NOT NULL REFERENCES mentor_profiles(id)  ON DELETE CASCADE,
    direction            VARCHAR(30) NOT NULL,
    status               VARCHAR(30) NOT NULL DEFAULT 'SENT',
    goal_type            VARCHAR(30) NOT NULL,
    message              TEXT        NOT NULL,
    clarification_note   TEXT,
    reason               TEXT,
    responded_at         TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE mentoring_requests;

--changeset bukevsky:012-mentoring-requests-indexes
CREATE INDEX idx_mr_mentor_status  ON mentoring_requests (mentor_profile_id, status);
CREATE INDEX idx_mr_student_status ON mentoring_requests (student_profile_id, status);
CREATE UNIQUE INDEX uq_mr_active_student_mentor
    ON mentoring_requests (student_profile_id, mentor_profile_id)
    WHERE status IN ('SENT', 'REVIEWING', 'NEEDS_CLARIFICATION', 'ACCEPTED');
--rollback DROP INDEX idx_mr_mentor_status; DROP INDEX idx_mr_student_status; DROP INDEX uq_mr_active_student_mentor;
