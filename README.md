# IT Mentor

**Платформа IT-менторинга** — веб-приложение для поиска и взаимодействия между IT-менторами и студентами.

Студенты заполняют профиль, загружают резюме и находят менторов по навыкам. Менторы публикуют свои компетенции, управляют набором учеников и выстраивают менторские программы.

---

## Технологии

| Слой              | Технология                                              |
| :---------------- | :------------------------------------------------------ |
| Язык              | Java 25                                                 |
| Фреймворк         | Spring Boot 4.0.3 (MVC, Security 7, Data JPA)           |
| База данных        | PostgreSQL 15                                           |
| Миграции           | Liquibase (formatted SQL)                               |
| Объектное хранилище | MinIO (S3-совместимое)                                  |
| Аутентификация     | JWT (HMAC-SHA256) — библиотека jjwt 0.12.6              |
| Маппинг DTO        | MapStruct 1.6.3                                         |
| Кодогенерация      | Lombok                                                  |
| API-документация   | springdoc-openapi 3.0.2 — Swagger UI                    |
| Тестирование       | JUnit 5, Mockito, AssertJ, Testcontainers 1.20.4        |
| Контейнеризация    | Docker Compose (PostgreSQL + MinIO)                     |

---

## Быстрый старт

### Предварительные требования

- **Java 25** (или новее)
- **Maven 3.9+** (или используйте встроенный `./mvnw`)
- **Docker** и **Docker Compose** — для PostgreSQL и MinIO

### 1. Запуск инфраструктуры

```bash
docker compose up -d
```

Это поднимет:

| Сервис     | Порт     | Назначение                  |
| :--------- | :------- | :-------------------------- |
| PostgreSQL | `5432`   | Основная база данных         |
| MinIO API  | `9000`   | Хранилище файлов (S3 API)   |
| MinIO UI   | `9001`   | Веб-консоль MinIO           |

### 2. Сборка проекта

```bash
./mvnw clean package -DskipTests
```

### 3. Запуск приложения

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Приложение будет доступно по адресу `http://localhost:8080`.

### 4. Swagger UI

После запуска откройте в браузере:

```
http://localhost:8080/swagger-ui.html
```

Все эндпоинты сгруппированы по тегам: Auth, Dictionaries, Profile, Student Profile, Mentor Profile, Files. Для защищённых эндпоинтов нажмите **Authorize** и вставьте JWT-токен, полученный при логине.

---

## Конфигурация

Приложение использует Spring-профили для разных окружений:

| Профиль   | Файл                       | Описание                                                    |
| :-------- | :-------------------------- | :---------------------------------------------------------- |
| `local`   | `application-local.yaml`   | PostgreSQL через docker-compose, MinIO, Liquibase включён   |
| `test`    | `application-test.yaml`    | Testcontainers (автоматический PostgreSQL), Liquibase включён |
| *(default)* | `application.yaml`       | Только общие настройки, datasource не настроен               |

### Переменные окружения

| Переменная          | По умолчанию                                     | Описание                    |
| :------------------ | :----------------------------------------------- | :-------------------------- |
| `DB_HOST`           | `localhost`                                      | Хост PostgreSQL              |
| `DB_PORT`           | `5432`                                           | Порт PostgreSQL              |
| `DB_NAME`           | `itmentor`                                       | Имя базы данных              |
| `DB_USER`           | `itmentor`                                       | Пользователь БД              |
| `DB_PASSWORD`       | `itmentor`                                       | Пароль БД                    |
| `JWT_SECRET`        | `dev-secret-key-must-be-at-least-32-characters…` | Секрет для подписи JWT       |
| `JWT_EXPIRATION_MS` | `86400000` (24 часа)                             | Время жизни токена (мс)      |
| `MINIO_ENDPOINT`    | `http://localhost:9000`                          | Адрес MinIO API              |
| `MINIO_ACCESS_KEY`  | `minioadmin`                                     | Access key MinIO             |
| `MINIO_SECRET_KEY`  | `minioadmin`                                     | Secret key MinIO             |
| `MINIO_BUCKET`      | `itmentor`                                       | Имя бакета для файлов        |

---

## API

### Аутентификация (`/auth`)

| Метод  | Путь                    | Доступ   | Описание                              |
| :----- | :---------------------- | :------- | :------------------------------------ |
| `POST` | `/auth/register`        | Публичный | Регистрация нового пользователя       |
| `POST` | `/auth/login`           | Публичный | Вход, возвращает JWT-токен            |
| `GET`  | `/auth/me`              | JWT       | Информация о текущем пользователе     |
| `POST` | `/auth/password/forgot` | Публичный | Запрос на сброс пароля                |
| `POST` | `/auth/password/reset`  | Публичный | Подтверждение сброса пароля           |

### Справочники (`/dictionaries`)

| Метод | Путь                             | Доступ    | Описание                |
| :---- | :------------------------------- | :-------- | :---------------------- |
| `GET` | `/dictionaries/cities`           | Публичный | Список городов           |
| `GET` | `/dictionaries/skills`           | Публичный | Список навыков           |
| `GET` | `/dictionaries/languages`        | Публичный | Список языков            |
| `GET` | `/dictionaries/interaction-types` | Публичный | Типы взаимодействия     |

### Профиль — сводка

| Метод | Путь           | Доступ | Описание                                    |
| :---- | :------------- | :----- | :------------------------------------------ |
| `GET` | `/profile/me`  | JWT    | Роль, наличие профиля, ID профиля           |

### Профиль студента

| Метод | Путь                     | Доступ | Описание                           |
| :---- | :----------------------- | :----- | :--------------------------------- |
| `PUT` | `/profile/student`       | JWT    | Создать или обновить профиль       |
| `GET` | `/profile/student/me`    | JWT    | Получить свой профиль              |
| `GET` | `/profiles/students/{id}` | JWT   | Получить профиль студента по ID    |

### Профиль ментора

| Метод | Путь                     | Доступ | Описание                                         |
| :---- | :----------------------- | :----- | :----------------------------------------------- |
| `PUT` | `/profile/mentor`        | JWT    | Создать или обновить профиль                     |
| `GET` | `/profile/mentor/me`     | JWT    | Получить свой профиль                            |
| `GET` | `/profiles/mentors/{id}` | JWT    | Получить профиль ментора по ID                   |
| `GET` | `/profiles/mentors`      | JWT    | Поиск менторов с фильтрацией и пагинацией        |

Query params для поиска: `q`, `skillIds[]`, `cityId`, `recruitmentStatus`, `mentoringType`, `mentoringChannel`, `page` (default `0`), `size` (default `20`, max `50`), `sort` (default `createdAt,desc`). Возвращает `PagedResponse<MentorCardResponse>`.

### Заявки на менторство (`/mentoring/requests`)

| Метод    | Путь                                            | Доступ | Описание                                            |
| :------- | :---------------------------------------------- | :----- | :-------------------------------------------------- |
| `POST`   | `/mentoring/requests`                           | JWT    | Создать заявку (студент→ментор или ментор→студент)  |
| `GET`    | `/mentoring/requests`                           | JWT    | Мои заявки (фильтрация по статусу, пагинация)       |
| `GET`    | `/mentoring/requests/{id}`                      | JWT    | Получить заявку по ID                               |
| `PUT`    | `/mentoring/requests/{id}/view`                 | JWT    | Адресат: перевести в REVIEWING                      |
| `PUT`    | `/mentoring/requests/{id}/needs-clarification`  | JWT    | Адресат: запросить уточнение (NEEDS_CLARIFICATION)  |
| `PUT`    | `/mentoring/requests/{id}/accept`               | JWT    | Адресат: принять заявку                             |
| `PUT`    | `/mentoring/requests/{id}/reject`               | JWT    | Адресат: отклонить заявку                           |
| `PUT`    | `/mentoring/requests/{id}/cancel`               | JWT    | Инициатор: отменить заявку                          |
| `PUT`    | `/mentoring/requests/{id}/complete`             | JWT    | Любой участник: завершить менторство                |

Статусы заявки: `SENT` → `REVIEWING` → `NEEDS_CLARIFICATION` → `ACCEPTED` → `COMPLETED`. Терминальные: `REJECTED`, `CANCELLED`.

### Чат (`/chats`)

| Метод  | Путь                                | Доступ | Описание                                       |
| :----- | :---------------------------------- | :----- | :--------------------------------------------- |
| `GET`  | `/chats`                            | JWT    | Мои чаты (пагинация по createdAt DESC)         |
| `GET`  | `/chats/{chatId}`                   | JWT    | Получить чат по ID                             |
| `GET`  | `/chats/by-request/{requestId}`     | JWT    | Получить чат по ID заявки                      |
| `GET`  | `/chats/{chatId}/messages`          | JWT    | Сообщения чата (пагинация по createdAt DESC)   |
| `POST` | `/chats/{chatId}/messages`          | JWT    | Отправить сообщение                            |

Чат создаётся автоматически при принятии заявки на менторство. Доступ только участникам.

### Файлы (`/files`)

| Метод  | Путь                       | Доступ | Описание                                              |
| :----- | :------------------------- | :----- | :---------------------------------------------------- |
| `POST` | `/files/resume`            | JWT    | Загрузить резюме (PDF, до 5 МБ)                       |
| `POST` | `/files/portfolio`         | JWT    | Загрузить портфолио (PDF/JPEG/PNG, до 10 МБ)          |
| `POST` | `/files/avatar`            | JWT    | Загрузить аватар (JPEG/PNG/WebP, до 2 МБ)             |
| `POST` | `/files/chat-attachment`   | JWT    | Загрузить вложение в чат (любой формат, без ограничений размера) |

Все файлы сохраняются в MinIO. Метаданные (тип, размер, storage key) хранятся в таблице `stored_files`.

### Администрирование (`/admin`)

| Метод | Путь                         | Доступ      | Описание                                            |
| :---- | :--------------------------- | :---------- | :-------------------------------------------------- |
| `PUT` | `/admin/users/{userId}/role` | JWT (ADMIN) | Назначить пользователю роль (STUDENT или MENTOR)    |

Логика: роли STUDENT и MENTOR взаимоисключающие. При назначении MENTOR роль STUDENT снимается (и наоборот). Назначить ADMIN через этот endpoint нельзя (422). Автоматически создаёт профиль при необходимости.

---

## Архитектура

### Общая схема

```
┌─────────────┐     ┌───────────────────────────────────────────────────┐
│   Клиент    │────>│                  Spring Boot                      │
│  (браузер,  │<────│                                                   │
│  мобильное  │     │  Controller ──> Service ──> Repository ──> JPA    │
│  приложение)│     │      │             │                       │      │
│             │     │      │             │                  PostgreSQL   │
│             │     │  JWT Filter   FileStorage                         │
│             │     │                    │                               │
│             │     │                  MinIO                             │
└─────────────┘     └───────────────────────────────────────────────────┘
```

### Пакетная структура

```
com.example.it.mentor
├── config/              Конфигурация (Security, JPA, OpenAPI, Storage)
├── security/            JWT-фильтр, провайдер, обработчики 401/403
├── controller/          REST-контроллеры (9 шт.)
├── service/             Бизнес-логика (11 шт.)
├── repository/          Spring Data JPA репозитории (15 шт.) + MentorProfileSpecification
├── entity/              JPA-сущности (15 шт.)
│   ├── dict/            Справочники (City, Skill, Language, InteractionType)
│   └── enums/           Перечисления (13 шт. + RoleCode, UserStatus)
├── dto/                 Java records — request/response (35 шт.)
│   ├── dict/            DTO справочников
│   ├── student/         DTO профиля студента
│   ├── mentor/          DTO профиля ментора (+ MentorCardResponse, PagedResponse)
│   └── mentoring/       DTO заявок на менторство (6 шт.)
├── mapper/              MapStruct-маперы (6 шт.)
└── exception/           Иерархия исключений + GlobalExceptionHandler
```

### Слои и ответственность

- **Controller** — тонкий слой: валидация входных данных (`@Valid`), делегирование в сервис, HTTP-семантика (статусы, заголовки).
- **Service** — вся бизнес-логика: получение текущего пользователя из SecurityContext, upsert-операции, валидация бизнес-правил, взаимодействие с хранилищем файлов.
- **Repository** — Spring Data JPA интерфейсы с `@EntityGraph` для предотвращения N+1 запросов.
- **Entity** — JPA-сущности, наследуют `BaseEntity` (id, createdAt, updatedAt через JPA Auditing).

---

## Безопасность

Приложение использует **stateless JWT-аутентификацию**:

1. Пользователь логинится через `POST /auth/login` и получает JWT-токен.
2. Токен передаётся в заголовке `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` перехватывает каждый запрос, извлекает и валидирует токен.
4. При невалидном/отсутствующем токене возвращается JSON-ответ с кодом `401`, а не HTML-страница Spring.

### Роли

| Роль      | Описание                                               |
| :-------- | :----------------------------------------------------- |
| `STUDENT` | Роль по умолчанию. Создание профиля, загрузка резюме    |
| `MENTOR`  | Назначается администратором. Создание менторского профиля, управление заявками. |
| `ADMIN`   | Управление ролями пользователей через `/admin/**`.       |

### Обработка ошибок

Все ошибки возвращаются в едином JSON-формате:

```json
{
  "status": 404,
  "errorCode": "NOT_FOUND",
  "message": "Профиль студента не найден",
  "path": "/profile/student/me",
  "details": null
}
```

| HTTP-код | Исключение                       | Когда                                |
| :------- | :------------------------------- | :----------------------------------- |
| `400`    | `MethodArgumentNotValidException` | Ошибки валидации `@Valid`            |
| `401`    | `UnauthorizedException`          | Невалидный или отсутствующий JWT      |
| `403`    | `ForbiddenException`             | Недостаточно прав                     |
| `404`    | `NotFoundException`              | Ресурс не найден                      |
| `409`    | `ConflictException`              | Дублирование (например, email)        |
| `422`    | `BusinessRuleViolationException` | Нарушение бизнес-правила              |
| `500`    | `StorageException`               | Ошибка файлового хранилища            |

---

## База данных

### ER-диаграмма (упрощённая)

```
users ──< user_roles >── roles
  │
  ├── student_profiles ──< student_educations
  │       ├──< student_languages >── dict_language
  │       ├──< student_skills >── dict_skill
  │       ├──< student_employment_types
  │       ├──< student_work_formats
  │       └──── dict_city
  │
  ├── mentor_profiles ──< mentor_skills >── dict_skill
  │       └──── dict_city
  │
  ├── stored_files (resume, portfolio, avatar, chat_attachment)
  │
  ├── mentoring_requests (student_profile_id, mentor_profile_id)
  │
  └── chats ──< chat_messages (chatId, senderUserId, body, attachmentFileId)
```

### Миграции

Liquibase-миграции расположены в `src/main/resources/db/changelog/changes/`:

| Файл                             | Описание                                                      |
| :------------------------------- | :------------------------------------------------------------ |
| `001_create_users_roles.sql`     | Таблицы `users`, `roles`, `user_roles`                        |
| `002_create_password_reset_tokens.sql` | Токены сброса пароля                                    |
| `003_seed_roles.sql`             | Начальные роли: STUDENT, MENTOR, ADMIN                        |
| `004_create_dict_tables.sql`     | Справочники: города, навыки, языки, типы взаимодействия        |
| `005_seed_dict_data.sql`         | Наполнение справочников                                        |
| `006_create_student_profile.sql` | Профиль студента + связанные таблицы                           |
| `007_create_mentor_profile.sql`  | Профиль ментора + навыки ментора                               |
| `008_create_stored_files.sql`    | Хранилище файлов, FK `resume_file_id` в `student_profiles`           |
| `009_add_city_id_indexes.sql`    | Индексы на FK `city_id` для student/mentor profiles                  |
| `010_add_mentor_search_indexes.sql` | Индексы для поиска менторов: status, type, channel, GIN-тргм по имени |
| `011_add_version_columns.sql`    | Оптимистичная блокировка: колонки `version` в users, profiles        |
| `012_create_mentoring_requests.sql` | Таблица `mentoring_requests` + индексы по mentor/student + статусу  |
| `013_update_password_reset_tokens.sql` | OTP: поле `code` (6 цифр), `attempts`, длина токена изменена |
| `014_create_chats.sql`              | Таблица `chats` (mentoringRequestId, studentUserId, mentorUserId)   |
| `015_create_chat_messages.sql`      | Таблица `chat_messages` (chatId, senderUserId, body, attachmentFileId) |

Миграции выполняются автоматически при запуске с профилем `local` или `test`.

---

## Тестирование

### Запуск тестов

```bash
# Все тесты (Testcontainers автоматически поднимет PostgreSQL)
./mvnw test

# Один класс
./mvnw test -Dtest=AuthControllerIT

# Один метод
./mvnw test -Dtest=AuthServiceTest#register_happyPath_shouldReturnResponse
```

### Структура тестов

| Тип                  | Пакет             | Количество | Подход                                            |
| :------------------- | :---------------- | :--------- | :------------------------------------------------ |
| Unit-тесты сервисов  | `service/`        | 8 классов  | `@ExtendWith(MockitoExtension.class)` + Mockito   |
| Интеграционные тесты | `controller/`     | 13 классов | `@SpringBootTest` + TestRestTemplate + Testcontainers |
| Unit-тесты           | `security/`, `entity/` | 2 класса | JwtProvider, Enum labels                          |

### Соглашения

- **Именование:** `methodName_scenario_expectedOutcome`
  ```
  register_duplicateEmail_shouldThrowConflict
  upsertProfile_recruitmentStatusNull_shouldDefaultToOpen
  ```
- **Assertions:** AssertJ (`assertThat(...).isEqualTo(...)`)
- **Nested-классы:** `@Nested @DisplayName` для группировки тестов по методам
- **SecurityContext в тестах:** устанавливается в `@BeforeEach`, очищается в `@AfterEach`

---

## Ключевые паттерны

### Upsert-стратегия профилей

Профили студента и ментора используют единый `PUT`-эндпоинт для создания и обновления:

```
findByUserId → (нет?) → builder.build()
                (есть?) → берём существующий
→ обновляем поля
→ save (получаем ID)
→ удаляем старые коллекции (навыки, языки, образование)
→ создаём новые коллекции
→ save
→ findWithDetails → mapper.toResponse()
```

### Soft Delete

Пользователи не удаляются физически. Поле `is_deleted` + частичный уникальный индекс на `email WHERE is_deleted = FALSE` гарантирует уникальность email только среди активных пользователей.

### EntityGraph для N+1

Профили с вложенными коллекциями (навыки, языки, образование) загружаются через `@NamedEntityGraph`, чтобы избежать N+1 запросов при маппинге в DTO.

### Валидация файлов

Каждый `FileType` (RESUME, PORTFOLIO, AVATAR) содержит допустимые MIME-типы и максимальный размер. Валидация вызывается до обращения к MinIO.

---

## Структура проекта

```
it.mentor/
├── docker-compose.yml                 PostgreSQL + MinIO
├── pom.xml                            Maven конфигурация
├── mvnw                               Maven Wrapper
├── CLAUDE.md                          Инструкции для AI-ассистента
│
├── src/main/
│   ├── java/com/example/it/mentor/
│   │   ├── Application.java
│   │   ├── config/                    (5 классов)
│   │   ├── security/                  (5 классов)
│   │   ├── controller/                (9 контроллеров)
│   │   ├── service/                   (11 сервисов)
│   │   ├── repository/                (15 репозиториев + 1 Specification)
│   │   ├── entity/                    (8 сущностей + dict/ + enums/)
│   │   ├── dto/                       (35 records)
│   │   ├── mapper/                    (6 маперов)
│   │   └── exception/                 (7 исключений + handler)
│   │
│   └── resources/
│       ├── application.yaml
│       ├── application-local.yaml
│       ├── application-test.yaml
│       └── db/changelog/
│           ├── db.changelog-master.yaml
│           └── changes/               (15 SQL-миграций)
│
└── src/test/java/com/example/it/mentor/
    ├── ApplicationTests.java
    ├── controller/                    (13 IT-тестов)
    ├── service/                       (8 unit-тестов)
    ├── security/                      (1 тест)
    └── entity/                        (1 тест)
```

---

## Статистика кодовой базы

| Категория               | Количество |
| :---------------------- | :--------- |
| Java-файлы (main)       | 125        |
| Тестовые классы          | ~27        |
| Тестов (JUnit 5)         | 217        |
| REST-эндпоинтов          | ~36        |
| Таблиц в БД             | 20         |
| SQL-миграций             | 15         |
| Контроллеров             | 9          |
| Сервисов                 | 11         |
| Репозиториев             | 15         |
| JPA-сущностей            | 15         |
| Перечислений (enum)      | 15         |
| DTO (records)            | 35         |
| MapStruct-маперов        | 6          |

---

## Roadmap

- [x] **Stage 1** — Аутентификация (регистрация, логин, JWT, сброс пароля)
- [x] **Stage 2** — Справочники, профили студента/ментора, загрузка файлов
- [x] **Stage 3** — Поиск менторов: фильтрация, пагинация, двухфазная загрузка, GIN-индекс
- [x] **Stage 4** — Система заявок на менторство: двунаправленные заявки, state machine, 7 статусов
- [x] **Stage 5** — OTP сброс пароля (6-значный код на email, 3 попытки, 15 минут TTL)
- [x] **Stage 6** — Чат: автосоздание при принятии заявки, сообщения, вложения
- [ ] **Stage 7** — Админ-панель, поиск студентов, уведомления, отзывы
