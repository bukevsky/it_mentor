--liquibase formatted sql

--changeset itmentor:001-create-roles-table labels:init
CREATE TABLE roles
(
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);
--rollback DROP TABLE roles;

--changeset itmentor:001-create-users-table labels:init
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'EMAIL_NOT_CONFIRMED',
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_users_email_active ON users (email) WHERE is_deleted = FALSE;
--rollback DROP INDEX uq_users_email_active; DROP TABLE users;

--changeset itmentor:001-create-user-roles-table labels:init
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
--rollback DROP TABLE user_roles;

--changeset itmentor:001-comments-roles labels:init
COMMENT ON TABLE roles IS 'Справочник ролей пользователей';
COMMENT ON COLUMN roles.id IS 'Первичный ключ';
COMMENT ON COLUMN roles.code IS 'Уникальный код роли (STUDENT, MENTOR, ADMIN)';
COMMENT ON COLUMN roles.name IS 'Отображаемое название роли';
--rollback SELECT 1;

--changeset itmentor:001-comments-users labels:init
COMMENT ON TABLE users IS 'Пользователи системы';
COMMENT ON COLUMN users.id IS 'Первичный ключ';
COMMENT ON COLUMN users.email IS 'Email адрес пользователя (хранится в нижнем регистре)';
COMMENT ON COLUMN users.password_hash IS 'Хэш пароля (bcrypt)';
COMMENT ON COLUMN users.status IS 'Статус аккаунта: ACTIVE, BLOCKED, EMAIL_NOT_CONFIRMED';
COMMENT ON COLUMN users.is_deleted IS 'Флаг мягкого удаления; уникальность email проверяется только среди активных записей';
COMMENT ON COLUMN users.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN users.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:001-comments-user-roles labels:init
COMMENT ON TABLE user_roles IS 'Связь пользователей и ролей (многие-ко-многим)';
COMMENT ON COLUMN user_roles.user_id IS 'Ссылка на пользователя';
COMMENT ON COLUMN user_roles.role_id IS 'Ссылка на роль';
--rollback SELECT 1;
