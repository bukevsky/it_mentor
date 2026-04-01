--liquibase formatted sql
--changeset bukevsky:015-create-chat-messages
CREATE TABLE chat_messages (
    id                   BIGSERIAL   PRIMARY KEY,
    chat_id              BIGINT      NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_user_id       BIGINT      NOT NULL REFERENCES users(id),
    body                 TEXT,
    attachment_file_id   BIGINT      REFERENCES stored_files(id) ON DELETE SET NULL,
    deleted              BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_message_has_content CHECK (body IS NOT NULL OR attachment_file_id IS NOT NULL)
);
--rollback DROP TABLE chat_messages;

--changeset bukevsky:015-chat-messages-indexes
CREATE INDEX idx_cm_chat_created ON chat_messages (chat_id, created_at DESC) WHERE deleted = FALSE;
--rollback DROP INDEX idx_cm_chat_created;
