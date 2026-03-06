--liquibase formatted sql

--changeset itmentor:003-seed-roles labels:init runOnChange:false
INSERT INTO roles (code, name)
VALUES ('STUDENT', 'Студент'),
       ('MENTOR', 'Ментор'),
       ('ADMIN', 'Администратор')
ON CONFLICT (code) DO NOTHING;
--rollback DELETE FROM roles WHERE code IN ('STUDENT', 'MENTOR', 'ADMIN');
