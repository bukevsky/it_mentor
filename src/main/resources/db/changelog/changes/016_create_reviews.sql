-- liquibase formatted sql

-- changeset bukevsky:016-create-reviews
CREATE TABLE reviews
(
    id                   BIGSERIAL PRIMARY KEY,
    mentoring_request_id BIGINT   NOT NULL UNIQUE REFERENCES mentoring_requests (id),
    reviewer_id          BIGINT   NOT NULL REFERENCES users (id),
    mentor_user_id       BIGINT   NOT NULL REFERENCES users (id),
    rating               INTEGER  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment              TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reviews_mentor_user_id ON reviews (mentor_user_id);
