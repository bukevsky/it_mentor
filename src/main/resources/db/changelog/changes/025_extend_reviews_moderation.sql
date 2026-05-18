--liquibase formatted sql

--changeset bukevsky:025-extend-reviews-moderation
ALTER TABLE reviews
    ADD COLUMN moderation_status VARCHAR(20)  NOT NULL DEFAULT 'VISIBLE'
        CHECK (moderation_status IN ('VISIBLE','HIDDEN','UNDER_REVIEW')),
    ADD COLUMN moderated_by      BIGINT       REFERENCES users(id),
    ADD COLUMN moderated_at      TIMESTAMPTZ;

CREATE INDEX idx_reviews_mentor_moderation ON reviews(mentor_user_id, moderation_status);
