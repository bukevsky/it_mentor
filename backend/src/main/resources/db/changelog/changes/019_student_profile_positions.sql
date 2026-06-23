-- liquibase formatted sql

-- changeset bukevsky:019-student-profile-positions
ALTER TABLE student_skills ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
ALTER TABLE student_languages ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
