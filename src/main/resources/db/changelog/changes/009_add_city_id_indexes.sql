--liquibase formatted sql

--changeset bukevsky:009-add-city-id-indexes
CREATE INDEX idx_student_profiles_city_id ON student_profiles (city_id);
CREATE INDEX idx_mentor_profiles_city_id ON mentor_profiles (city_id);
