--liquibase formatted sql

--changeset itmentor:004-create-dict-city labels:stage1
CREATE TABLE dict_city
(
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    region     VARCHAR(150),
    country    VARCHAR(100) NOT NULL DEFAULT 'Россия',
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_dict_city_name ON dict_city (name) WHERE active = TRUE;
--rollback DROP INDEX uq_dict_city_name; DROP TABLE dict_city;

--changeset itmentor:004-create-dict-skill labels:stage1
CREATE TABLE dict_skill
(
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    category   VARCHAR(100),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_dict_skill_name ON dict_skill (name) WHERE active = TRUE;
--rollback DROP INDEX uq_dict_skill_name; DROP TABLE dict_skill;

--changeset itmentor:004-create-dict-language labels:stage1
CREATE TABLE dict_language
(
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(10)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_dict_language_code ON dict_language (code) WHERE active = TRUE;
--rollback DROP INDEX uq_dict_language_code; DROP TABLE dict_language;

--changeset itmentor:004-create-dict-interaction-type labels:stage1
CREATE TABLE dict_interaction_type
(
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_dict_interaction_type_name ON dict_interaction_type (name) WHERE active = TRUE;
--rollback DROP INDEX uq_dict_interaction_type_name; DROP TABLE dict_interaction_type;

--changeset itmentor:004-comments-dict-city labels:stage1
COMMENT ON TABLE dict_city IS 'Справочник городов';
COMMENT ON COLUMN dict_city.id IS 'Первичный ключ';
COMMENT ON COLUMN dict_city.name IS 'Название города';
COMMENT ON COLUMN dict_city.region IS 'Регион или область';
COMMENT ON COLUMN dict_city.country IS 'Страна';
COMMENT ON COLUMN dict_city.active IS 'Признак активности записи; уникальность name проверяется только среди активных';
COMMENT ON COLUMN dict_city.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN dict_city.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:004-comments-dict-skill labels:stage1
COMMENT ON TABLE dict_skill IS 'Справочник навыков и технологий';
COMMENT ON COLUMN dict_skill.id IS 'Первичный ключ';
COMMENT ON COLUMN dict_skill.name IS 'Название навыка или технологии';
COMMENT ON COLUMN dict_skill.category IS 'Категория навыка (Backend, Frontend, DevOps и т.д.)';
COMMENT ON COLUMN dict_skill.active IS 'Признак активности записи; уникальность name проверяется только среди активных';
COMMENT ON COLUMN dict_skill.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN dict_skill.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:004-comments-dict-language labels:stage1
COMMENT ON TABLE dict_language IS 'Справочник языков';
COMMENT ON COLUMN dict_language.id IS 'Первичный ключ';
COMMENT ON COLUMN dict_language.name IS 'Название языка на русском';
COMMENT ON COLUMN dict_language.code IS 'ISO 639-1 код языка (ru, en, de и т.д.)';
COMMENT ON COLUMN dict_language.active IS 'Признак активности записи; уникальность code проверяется только среди активных';
COMMENT ON COLUMN dict_language.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN dict_language.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;

--changeset itmentor:004-comments-dict-interaction-type labels:stage1
COMMENT ON TABLE dict_interaction_type IS 'Справочник типов взаимодействия ментора со студентом';
COMMENT ON COLUMN dict_interaction_type.id IS 'Первичный ключ';
COMMENT ON COLUMN dict_interaction_type.name IS 'Название типа взаимодействия';
COMMENT ON COLUMN dict_interaction_type.description IS 'Описание типа взаимодействия';
COMMENT ON COLUMN dict_interaction_type.active IS 'Признак активности записи; уникальность name проверяется только среди активных';
COMMENT ON COLUMN dict_interaction_type.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN dict_interaction_type.updated_at IS 'Дата и время последнего обновления записи';
--rollback SELECT 1;
