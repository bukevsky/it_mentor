--liquibase formatted sql

--changeset itmentor:005-seed-cities labels:stage1
INSERT INTO dict_city (name, region, country)
VALUES ('Москва', 'Московская область', 'Россия'),
       ('Санкт-Петербург', 'Ленинградская область', 'Россия'),
       ('Новосибирск', 'Новосибирская область', 'Россия'),
       ('Екатеринбург', 'Свердловская область', 'Россия'),
       ('Казань', 'Республика Татарстан', 'Россия'),
       ('Нижний Новгород', 'Нижегородская область', 'Россия')
ON CONFLICT DO NOTHING;
--rollback DELETE FROM dict_city;

--changeset itmentor:005-seed-skills labels:stage1
INSERT INTO dict_skill (name, category)
VALUES ('Java', 'Backend'),
       ('Python', 'Backend'),
       ('JavaScript', 'Frontend'),
       ('TypeScript', 'Frontend'),
       ('React', 'Frontend'),
       ('Spring Boot', 'Backend'),
       ('PostgreSQL', 'Database'),
       ('Docker', 'DevOps'),
       ('Kubernetes', 'DevOps'),
       ('Git', 'Tools')
ON CONFLICT DO NOTHING;
--rollback DELETE FROM dict_skill;

--changeset itmentor:005-seed-languages labels:stage1
INSERT INTO dict_language (name, code)
VALUES ('Русский', 'ru'),
       ('Английский', 'en'),
       ('Немецкий', 'de'),
       ('Французский', 'fr'),
       ('Испанский', 'es'),
       ('Китайский', 'zh')
ON CONFLICT DO NOTHING;
--rollback DELETE FROM dict_language;

--changeset itmentor:005-seed-interaction-types labels:stage1
INSERT INTO dict_interaction_type (name, description)
VALUES ('Карьерные консультации', 'Обсуждение карьерного пути и целей'),
       ('Код-ревью', 'Ревью кода и архитектурных решений'),
       ('Подготовка к собеседованию', 'Помощь в подготовке к техническим собеседованиям'),
       ('Менторинг по проекту', 'Сопровождение при работе над конкретным проектом'),
       ('Обучение технологии', 'Изучение конкретного стека или инструмента')
ON CONFLICT DO NOTHING;
--rollback DELETE FROM dict_interaction_type;
