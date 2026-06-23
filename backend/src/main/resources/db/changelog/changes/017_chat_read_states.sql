-- liquibase formatted sql

-- changeset bukevsky:017-chat-read-states
ALTER TABLE chats ADD COLUMN last_message_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE chats ADD COLUMN last_sender_user_id BIGINT;
CREATE INDEX idx_chats_last_message_at ON chats (last_message_at DESC NULLS LAST);

CREATE TABLE chat_read_states
(
    id           BIGSERIAL PRIMARY KEY,
    chat_id      BIGINT                   NOT NULL REFERENCES chats (id),
    user_id      BIGINT                   NOT NULL REFERENCES users (id),
    last_read_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (chat_id, user_id)
);

CREATE INDEX idx_chat_read_states_user ON chat_read_states (user_id);
