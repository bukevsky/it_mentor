--liquibase formatted sql

--changeset bukevsky:010-add-mentor-search-indexes
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_mentor_profiles_first_name_trgm ON mentor_profiles USING gin (first_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_last_name_trgm ON mentor_profiles USING gin (last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_recruitment_status ON mentor_profiles (recruitment_status);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_mentoring_type ON mentor_profiles (mentoring_type);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_mentoring_channel ON mentor_profiles (mentoring_channel);
CREATE INDEX IF NOT EXISTS idx_mentor_skills_profile_id ON mentor_skills (profile_id);
CREATE INDEX IF NOT EXISTS idx_mentor_skills_skill_id ON mentor_skills (skill_id);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_created_at ON mentor_profiles (created_at DESC);
