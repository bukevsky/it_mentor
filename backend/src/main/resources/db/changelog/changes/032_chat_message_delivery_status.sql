-- liquibase formatted sql

-- changeset bukevsky:032-chat-message-delivery-status
ALTER TABLE chat_messages
    ADD COLUMN client_message_id UUID,
    ADD COLUMN delivery_status VARCHAR(16) NOT NULL DEFAULT 'SENT',
    ADD COLUMN delivered_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN read_at TIMESTAMP WITH TIME ZONE;

UPDATE chat_messages
SET client_message_id = gen_random_uuid()
WHERE client_message_id IS NULL;

UPDATE chat_messages m
SET delivery_status = 'READ',
    delivered_at = rs.last_read_at,
    read_at = rs.last_read_at
FROM chat_read_states rs
WHERE rs.chat_id = m.chat_id
  AND rs.user_id <> m.sender_user_id
  AND m.created_at <= rs.last_read_at;

ALTER TABLE chat_messages
    ALTER COLUMN client_message_id SET NOT NULL,
    ADD CONSTRAINT uq_chat_message_sender_client_id UNIQUE (sender_user_id, client_message_id),
    ADD CONSTRAINT chk_chat_message_delivery_status
        CHECK (delivery_status IN ('SENT', 'DELIVERED', 'READ'));

CREATE INDEX idx_chat_messages_sync
    ON chat_messages (chat_id, id ASC)
    WHERE deleted = FALSE;

CREATE INDEX idx_chat_messages_pending_status
    ON chat_messages (chat_id, sender_user_id, id ASC)
    WHERE deleted = FALSE AND delivery_status <> 'READ';

DROP TABLE chat_read_states;

-- rollback CREATE TABLE chat_read_states (id BIGSERIAL PRIMARY KEY, chat_id BIGINT NOT NULL REFERENCES chats(id), user_id BIGINT NOT NULL REFERENCES users(id), last_read_at TIMESTAMP WITH TIME ZONE NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, UNIQUE (chat_id, user_id));
-- rollback CREATE INDEX idx_chat_read_states_user ON chat_read_states (user_id);
-- rollback DROP INDEX IF EXISTS idx_chat_messages_pending_status;
-- rollback DROP INDEX IF EXISTS idx_chat_messages_sync;
-- rollback ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chk_chat_message_delivery_status, DROP CONSTRAINT IF EXISTS uq_chat_message_sender_client_id, DROP COLUMN IF EXISTS read_at, DROP COLUMN IF EXISTS delivered_at, DROP COLUMN IF EXISTS delivery_status, DROP COLUMN IF EXISTS client_message_id;
