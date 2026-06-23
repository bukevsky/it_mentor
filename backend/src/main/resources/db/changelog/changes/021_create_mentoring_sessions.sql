--liquibase formatted sql
--changeset bukevsky:021-create-mentoring-sessions
CREATE TABLE mentoring_sessions (
    id                   BIGSERIAL   PRIMARY KEY,
    mentoring_request_id BIGINT      NOT NULL REFERENCES mentoring_requests(id) ON DELETE CASCADE,
    student_user_id      BIGINT      NOT NULL REFERENCES users(id),
    mentor_user_id       BIGINT      NOT NULL REFERENCES users(id),
    scheduled_at         TIMESTAMPTZ NOT NULL,
    duration_minutes     INTEGER     NOT NULL CHECK (duration_minutes BETWEEN 15 AND 480),
    status               VARCHAR(32) NOT NULL CHECK (status IN ('SCHEDULED','RESCHEDULED','COMPLETED','CANCELLED','NO_SHOW')),
    cancel_reason        TEXT,
    reschedule_reason    TEXT,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE mentoring_sessions;

--changeset bukevsky:021-mentoring-sessions-indexes
CREATE INDEX idx_mentoring_sessions_mentor_scheduled  ON mentoring_sessions (mentor_user_id, scheduled_at);
CREATE INDEX idx_mentoring_sessions_student_scheduled ON mentoring_sessions (student_user_id, scheduled_at);
CREATE INDEX idx_mentoring_sessions_request           ON mentoring_sessions (mentoring_request_id);
--rollback DROP INDEX idx_mentoring_sessions_mentor_scheduled; DROP INDEX idx_mentoring_sessions_student_scheduled; DROP INDEX idx_mentoring_sessions_request;
