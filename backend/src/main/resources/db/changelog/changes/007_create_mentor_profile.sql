--liquibase formatted sql

--changeset itmentor:007-create-mentor-profiles labels:stage1
CREATE TABLE mentor_profiles
(
    id                 BIGSERIAL    PRIMARY KEY,
    user_id            BIGINT       NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    middle_name        VARCHAR(100),
    position           VARCHAR(255),
    department         VARCHAR(255),
    city_id            BIGINT       REFERENCES dict_city (id),
    phone              VARCHAR(30),
    max                VARCHAR(100),
    description        TEXT,
    expectations       TEXT,
    can_help_with      TEXT,
    mentoring_type      VARCHAR(50),
    mentoring_channel   VARCHAR(50),
    mentoring_frequency VARCHAR(100),
    mentoring_duration  VARCHAR(50),
    mentee_limit       INT,
    recruitment_status VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE mentor_profiles;

--changeset itmentor:007-create-mentor-skills labels:stage1
CREATE TABLE mentor_skills
(
    id         BIGSERIAL   PRIMARY KEY,
    profile_id BIGINT      NOT NULL REFERENCES mentor_profiles (id) ON DELETE CASCADE,
    skill_id   BIGINT      NOT NULL REFERENCES dict_skill (id),
    level      VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (profile_id, skill_id)
);
--rollback DROP TABLE mentor_skills;

--changeset itmentor:007-comments-mentor-profiles labels:stage1
COMMENT ON TABLE mentor_profiles IS 'Профили менторов';
COMMENT ON COLUMN mentor_profiles.id IS 'Первичный ключ';
COMMENT ON COLUMN mentor_profiles.user_id IS 'Ссылка на аккаунт пользователя (один-к-одному)';
COMMENT ON COLUMN mentor_profiles.first_name IS 'Имя';
COMMENT ON COLUMN mentor_profiles.last_name IS 'Фамилия';
COMMENT ON COLUMN mentor_profiles.middle_name IS 'Отчество';
COMMENT ON COLUMN mentor_profiles.position IS 'Должность ментора';
COMMENT ON COLUMN mentor_profiles.department IS 'Отдел или команда';
COMMENT ON COLUMN mentor_profiles.city_id IS 'Ссылка на город из справочника';
COMMENT ON COLUMN mentor_profiles.phone IS 'Номер телефона';
COMMENT ON COLUMN mentor_profiles.max IS 'Никнейм или ссылка в Max';
COMMENT ON COLUMN mentor_profiles.description IS 'Описание опыта и экспертизы ментора';
COMMENT ON COLUMN mentor_profiles.expectations IS 'Ожидания ментора от студентов';
COMMENT ON COLUMN mentor_profiles.can_help_with IS 'С чем ментор может помочь';
COMMENT ON COLUMN mentor_profiles.mentoring_type IS 'Тип менторинга (значение enum MentoringType)';
COMMENT ON COLUMN mentor_profiles.mentoring_channel IS 'Канал общения (значение enum MentoringChannel)';
COMMENT ON COLUMN mentor_profiles.mentoring_frequency IS 'Частота встреч (например, раз в неделю)';
COMMENT ON COLUMN mentor_profiles.mentoring_duration IS 'Продолжительность сессий (значение enum MentoringDuration)';
COMMENT ON COLUMN mentor_profiles.mentee_limit IS 'Максимальное количество студентов одновременно';
COMMENT ON COLUMN mentor_profiles.recruitment_status IS 'Статус набора студентов: OPEN, CLOSED, PAUSED';
COMMENT ON COLUMN mentor_profiles.created_at IS 'Дата и время создания профиля';
COMMENT ON COLUMN mentor_profiles.updated_at IS 'Дата и время последнего обновления профиля';
--rollback SELECT 1;

--changeset itmentor:007-comments-mentor-skills labels:stage1
COMMENT ON TABLE mentor_skills IS 'Навыки ментора с уровнем владения';
COMMENT ON COLUMN mentor_skills.id IS 'Первичный ключ';
COMMENT ON COLUMN mentor_skills.profile_id IS 'Ссылка на профиль ментора';
COMMENT ON COLUMN mentor_skills.skill_id IS 'Ссылка на навык из справочника';
COMMENT ON COLUMN mentor_skills.level IS 'Уровень владения навыком (значение enum SkillLevel)';
COMMENT ON COLUMN mentor_skills.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN mentor_skills.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;
