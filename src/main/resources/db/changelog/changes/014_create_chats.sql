--liquibase formatted sql
--changeset bukevsky:014-create-chats
CREATE TABLE chats (
    id                   BIGSERIAL    PRIMARY KEY,
    mentoring_request_id BIGINT       NOT NULL UNIQUE
        REFERENCES mentoring_requests(id) ON DELETE CASCADE,
    student_user_id      BIGINT       NOT NULL REFERENCES users(id),
    mentor_user_id       BIGINT       NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE chats;

--changeset bukevsky:014-chats-indexes
CREATE INDEX idx_chats_student_user ON chats (student_user_id);
CREATE INDEX idx_chats_mentor_user  ON chats (mentor_user_id);
--rollback DROP INDEX idx_chats_student_user; DROP INDEX idx_chats_mentor_user;
