-- liquibase formatted sql

-- changeset bukevsky:018-extend-stored-files
ALTER TABLE stored_files ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
UPDATE stored_files SET file_type = 'CHAT_ATTACHMENT' WHERE file_type = 'ATTACHMENT';
CREATE INDEX idx_stored_files_owner_status ON stored_files (owner_id, status);
CREATE INDEX idx_stored_files_type ON stored_files (file_type);
CREATE INDEX idx_stored_files_uploaded_at ON stored_files (uploaded_at);
