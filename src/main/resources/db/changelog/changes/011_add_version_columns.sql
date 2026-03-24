--liquibase formatted sql

--changeset bukevsky:011-add-version-columns
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE student_profiles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mentor_profiles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
