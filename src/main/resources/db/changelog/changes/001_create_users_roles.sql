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
