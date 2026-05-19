--liquibase formatted sql

--changeset bukevsky:027-1
ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
