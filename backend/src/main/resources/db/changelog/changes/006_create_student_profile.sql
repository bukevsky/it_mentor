--liquibase formatted sql

--changeset itmentor:006-create-student-profiles labels:stage1
CREATE TABLE student_profiles
(
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    middle_name      VARCHAR(100),
    phone            VARCHAR(30),
    city_id          BIGINT       REFERENCES dict_city (id),
    desired_position VARCHAR(255),
    hours_per_week   INT,
    available_from   DATE,
    about            TEXT,
    max              VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE student_profiles;

--changeset itmentor:006-create-student-employment-types labels:stage1
CREATE TABLE student_employment_types
(
    profile_id      BIGINT      NOT NULL REFERENCES student_profiles (id) ON DELETE CASCADE,
    employment_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (profile_id, employment_type)
);
--rollback DROP TABLE student_employment_types;

--changeset itmentor:006-create-student-work-formats labels:stage1
CREATE TABLE student_work_formats
(
    profile_id  BIGINT      NOT NULL REFERENCES student_profiles (id) ON DELETE CASCADE,
    work_format VARCHAR(50) NOT NULL,
    PRIMARY KEY (profile_id, work_format)
);
--rollback DROP TABLE student_work_formats;

--changeset itmentor:006-create-student-educations labels:stage1
CREATE TABLE student_educations
(
    id              BIGSERIAL    PRIMARY KEY,
    profile_id      BIGINT       NOT NULL REFERENCES student_profiles (id) ON DELETE CASCADE,
    institution     VARCHAR(255) NOT NULL,
    specialty       VARCHAR(255),
    degree          VARCHAR(100),
    education_form  VARCHAR(50),
    start_year      INT,
    graduation_year INT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_student_educations_profile_id ON student_educations (profile_id);
--rollback DROP INDEX idx_student_educations_profile_id; DROP TABLE student_educations;

--changeset itmentor:006-create-student-languages labels:stage1
CREATE TABLE student_languages
(
    id          BIGSERIAL   PRIMARY KEY,
    profile_id  BIGINT      NOT NULL REFERENCES student_profiles (id) ON DELETE CASCADE,
    language_id BIGINT      NOT NULL REFERENCES dict_language (id),
    level       VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (profile_id, language_id)
);
--rollback DROP TABLE student_languages;

--changeset itmentor:006-create-student-skills labels:stage1
CREATE TABLE student_skills
(
    id         BIGSERIAL   PRIMARY KEY,
    profile_id BIGINT      NOT NULL REFERENCES student_profiles (id) ON DELETE CASCADE,
    skill_id   BIGINT      NOT NULL REFERENCES dict_skill (id),
    level      VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (profile_id, skill_id)
);
--rollback DROP TABLE student_skills;

--changeset itmentor:006-comments-student-profiles labels:stage1
COMMENT ON TABLE student_profiles IS 'Профили студентов';
COMMENT ON COLUMN student_profiles.id IS 'Первичный ключ';
COMMENT ON COLUMN student_profiles.user_id IS 'Ссылка на аккаунт пользователя (один-к-одному)';
COMMENT ON COLUMN student_profiles.first_name IS 'Имя';
COMMENT ON COLUMN student_profiles.last_name IS 'Фамилия';
COMMENT ON COLUMN student_profiles.middle_name IS 'Отчество';
COMMENT ON COLUMN student_profiles.phone IS 'Номер телефона';
COMMENT ON COLUMN student_profiles.city_id IS 'Ссылка на город из справочника';
COMMENT ON COLUMN student_profiles.desired_position IS 'Желаемая должность или направление';
COMMENT ON COLUMN student_profiles.hours_per_week IS 'Количество часов в неделю, доступных для менторинга';
COMMENT ON COLUMN student_profiles.available_from IS 'Дата, с которой студент доступен для менторинга';
COMMENT ON COLUMN student_profiles.about IS 'Краткое описание о себе';
COMMENT ON COLUMN student_profiles.max IS 'Никнейм или ссылка в Max';
COMMENT ON COLUMN student_profiles.created_at IS 'Дата и время создания профиля';
COMMENT ON COLUMN student_profiles.updated_at IS 'Дата и время последнего обновления профиля';
--rollback SELECT 1;

--changeset itmentor:006-comments-student-employment-types labels:stage1
COMMENT ON TABLE student_employment_types IS 'Предпочтительные типы занятости студента (FULL_TIME, PART_TIME и т.д.)';
COMMENT ON COLUMN student_employment_types.profile_id IS 'Ссылка на профиль студента';
COMMENT ON COLUMN student_employment_types.employment_type IS 'Тип занятости (значение enum EmploymentType)';
--rollback SELECT 1;

--changeset itmentor:006-comments-student-work-formats labels:stage1
COMMENT ON TABLE student_work_formats IS 'Предпочтительные форматы работы студента (REMOTE, OFFICE, HYBRID)';
COMMENT ON COLUMN student_work_formats.profile_id IS 'Ссылка на профиль студента';
COMMENT ON COLUMN student_work_formats.work_format IS 'Формат работы (значение enum WorkFormat)';
--rollback SELECT 1;

--changeset itmentor:006-comments-student-educations labels:stage1
COMMENT ON TABLE student_educations IS 'Образование студента';
COMMENT ON COLUMN student_educations.id IS 'Первичный ключ';
COMMENT ON COLUMN student_educations.profile_id IS 'Ссылка на профиль студента';
COMMENT ON COLUMN student_educations.institution IS 'Название учебного заведения';
COMMENT ON COLUMN student_educations.specialty IS 'Специальность или направление подготовки';
COMMENT ON COLUMN student_educations.degree IS 'Степень (Бакалавр, Магистр и т.д.)';
COMMENT ON COLUMN student_educations.education_form IS 'Форма обучения (очная, заочная и т.д.)';
COMMENT ON COLUMN student_educations.start_year IS 'Год начала обучения';
COMMENT ON COLUMN student_educations.graduation_year IS 'Год окончания обучения';
COMMENT ON COLUMN student_educations.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN student_educations.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:006-comments-student-languages labels:stage1
COMMENT ON TABLE student_languages IS 'Языки студента с уровнем владения';
COMMENT ON COLUMN student_languages.id IS 'Первичный ключ';
COMMENT ON COLUMN student_languages.profile_id IS 'Ссылка на профиль студента';
COMMENT ON COLUMN student_languages.language_id IS 'Ссылка на язык из справочника';
COMMENT ON COLUMN student_languages.level IS 'Уровень владения языком (значение enum LanguageLevel)';
COMMENT ON COLUMN student_languages.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN student_languages.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:006-comments-student-skills labels:stage1
COMMENT ON TABLE student_skills IS 'Навыки студента с уровнем владения';
COMMENT ON COLUMN student_skills.id IS 'Первичный ключ';
COMMENT ON COLUMN student_skills.profile_id IS 'Ссылка на профиль студента';
COMMENT ON COLUMN student_skills.skill_id IS 'Ссылка на навык из справочника';
COMMENT ON COLUMN student_skills.level IS 'Уровень владения навыком (значение enum SkillLevel)';
COMMENT ON COLUMN student_skills.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN student_skills.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;
