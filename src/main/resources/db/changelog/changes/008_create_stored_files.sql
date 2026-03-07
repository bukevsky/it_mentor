--liquibase formatted sql

--changeset itmentor:008-create-stored-files labels:stage1
CREATE TABLE stored_files
(
    id                BIGSERIAL    PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size              BIGINT       NOT NULL,
    storage_key       VARCHAR(255) NOT NULL UNIQUE,
    file_type         VARCHAR(50)  NOT NULL,
    owner_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stored_files_owner_id ON stored_files (owner_id);
--rollback DROP INDEX idx_stored_files_owner_id; DROP TABLE stored_files;

--changeset itmentor:008-add-resume-to-student-profile labels:stage1
ALTER TABLE student_profiles
    ADD COLUMN resume_file_id BIGINT REFERENCES stored_files (id) ON DELETE SET NULL;
--rollback ALTER TABLE student_profiles DROP COLUMN resume_file_id;

--changeset itmentor:008-comment-resume-file-id labels:stage1
COMMENT ON COLUMN student_profiles.resume_file_id IS 'Ссылка на загруженное резюме (файл)';
--rollback SELECT 1;

--changeset itmentor:008-comments-stored-files labels:stage1
COMMENT ON TABLE stored_files IS 'Хранилище загруженных файлов (резюме и другие документы)';
COMMENT ON COLUMN stored_files.id IS 'Первичный ключ';
COMMENT ON COLUMN stored_files.original_filename IS 'Оригинальное имя файла при загрузке';
COMMENT ON COLUMN stored_files.content_type IS 'MIME-тип файла (например, application/pdf)';
COMMENT ON COLUMN stored_files.size IS 'Размер файла в байтах';
COMMENT ON COLUMN stored_files.storage_key IS 'Уникальный ключ файла в хранилище (S3 key или путь)';
COMMENT ON COLUMN stored_files.file_type IS 'Тип файла в контексте системы (значение enum FileType)';
COMMENT ON COLUMN stored_files.owner_id IS 'Ссылка на пользователя, загрузившего файл';
COMMENT ON COLUMN stored_files.uploaded_at IS 'Дата и время загрузки файла';
COMMENT ON COLUMN stored_files.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN stored_files.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;
