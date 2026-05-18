# IT Mentor

**Платформа IT-менторинга** — веб-приложение для поиска и взаимодействия между IT-менторами и студентами.

Студенты заполняют профиль, загружают резюме и находят менторов по навыкам. Менторы публикуют свои компетенции, управляют набором учеников и выстраивают менторские программы. Участники общаются в режиме реального времени через чат с SSE-событиями, оставляют отзывы и отслеживают прогресс на персональном дашборде.

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
| Кеш               | Caffeine через Spring Cache                             |
| Логирование        | SLF4J, MDC-трейсинг, структурированный JSON-формат      |
| API-документация   | springdoc-openapi 3.0.2 — Swagger UI                    |
| Метрики            | Spring Boot Actuator + Micrometer Prometheus            |
| JSON                | Jackson 3 (`tools.jackson.*`, через `spring-boot-starter-jackson`) |
| Email               | `spring-boot-starter-mail` + outbox-таблица для надёжной доставки |
| Тестирование       | JUnit 5, Mockito, AssertJ, Testcontainers 1.21.3, docker-java 3.5.1 |
| Контейнеризация    | Docker Compose (PostgreSQL + MinIO + MailHog)           |
| CI                 | GitHub Actions (Java 25 + Testcontainers)               |
| Качество кода      | JaCoCo + SonarQube + sonarqube-community-branch-plugin  |

---

## Быстрый старт

### Предварительные требования

- **Java 25** (или новее)
- **Maven 3.9+** (или используйте встроенный `./mvnw`)
- **Docker** и **Docker Compose** — для PostgreSQL, MinIO и MailHog

### 1. Запуск инфраструктуры

```bash
docker compose up -d postgres minio mailhog
```

| Сервис       | Порт(ы) | Назначение                |
| :----------- | :------ | :------------------------ |
| PostgreSQL   | `5432`  | Основная база данных       |
| MinIO API    | `9000`  | Хранилище файлов (S3 API) |
| MinIO UI     | `9001`  | Веб-консоль MinIO         |
| MailHog SMTP | `1025`  | Перехват email (OTP-коды) |
| MailHog UI   | `8025`  | Просмотр писем в браузере |

SonarQube запускается отдельно, только когда нужен анализ кода (см. раздел [Анализ кода](#анализ-кода-sonarqube)).

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

```
http://localhost:8080/swagger-ui.html
```

Для защищённых эндпоинтов нажмите **Authorize** и вставьте JWT-токен, полученный при логине.

---

## Конфигурация

| Профиль   | Файл                     | Описание                                                    |
| :-------- | :----------------------- | :---------------------------------------------------------- |
| `local`   | `application-local.yaml` | PostgreSQL через docker-compose, MinIO, Liquibase включён   |
| `test`    | `application-test.yaml`  | Testcontainers (автоматический PostgreSQL), Liquibase включён |
| *(default)* | `application.yaml`     | Общие настройки; datasource и секреты — из переменных окружения |

### Переменные окружения

| Переменная          | Описание                       |
| :------------------ | :----------------------------- |
| `JWT_SECRET`        | Секрет для подписи JWT (обязательная) |
| `DB_PASSWORD`       | Пароль PostgreSQL (обязательная) |
| `MINIO_ACCESS_KEY`  | Access key MinIO (обязательная) |
| `MINIO_SECRET_KEY`  | Secret key MinIO (обязательная) |
| `CORS_ALLOWED_ORIGINS` | Разрешённые origins (обязательная) |
| `DB_HOST`           | Хост PostgreSQL (default: `localhost`) |
| `DB_PORT`           | Порт PostgreSQL (default: `5432`) |
| `DB_NAME`           | Имя базы данных (default: `itmentor`) |
| `JWT_EXPIRATION_MS` | Время жизни токена в мс (default: `86400000`) |
| `MINIO_ENDPOINT`    | Адрес MinIO API (default: `http://localhost:9000`) |
| `MINIO_BUCKET`      | Имя бакета (default: `itmentor`) |

---

## API

### Аутентификация (`/auth`)

| Метод  | Путь                    | Доступ    | Описание                        |
| :----- | :---------------------- | :-------- | :------------------------------ |
| `POST` | `/auth/register`        | Публичный | Регистрация нового пользователя |
| `POST` | `/auth/login`           | Публичный | Вход, возвращает JWT-токен      |
| `GET`  | `/auth/me`              | JWT       | Информация о текущем пользователе |
| `POST` | `/auth/password/forgot` | Публичный | Запрос сброса пароля (OTP на email) |
| `POST` | `/auth/password/reset`  | Публичный | Подтверждение сброса: email + 6-значный код |

### Справочники (`/dictionaries`)

| Метод | Путь                              | Доступ    | Описание              |
| :---- | :-------------------------------- | :-------- | :-------------------- |
| `GET` | `/dictionaries/cities`            | Публичный | Список городов         |
| `GET` | `/dictionaries/skills`            | Публичный | Список навыков         |
| `GET` | `/dictionaries/languages`         | Публичный | Список языков          |
| `GET` | `/dictionaries/interaction-types` | Публичный | Типы взаимодействия   |

### Профиль — сводка

| Метод | Путь          | Доступ | Описание                                 |
| :---- | :------------ | :----- | :--------------------------------------- |
| `GET` | `/profile/me` | JWT    | Роль, наличие профиля, ID профиля        |

### Профиль студента

| Метод   | Путь                                   | Доступ          | Описание                                     |
| :------ | :------------------------------------- | :-------------- | :------------------------------------------- |
| `PUT`   | `/profile/student`                     | JWT             | Создать или обновить профиль (полный upsert) |
| `PATCH` | `/profile/student`                     | JWT             | Частичное обновление (только переданные поля) |
| `GET`   | `/profile/student/me`                  | JWT             | Получить свой профиль                        |
| `GET`   | `/profile/student/me/completion`       | JWT             | Процент заполненности профиля                |
| `PUT`   | `/profile/student/skills`              | JWT             | Заменить список навыков (с позицией)         |
| `PUT`   | `/profile/student/languages`           | JWT             | Заменить список языков (с позицией)          |
| `GET`   | `/profile/student/me/files`            | JWT             | Список файлов студента (резюме, портфолио, аватар) |
| `GET`   | `/profiles/students/{id}`              | JWT             | Профиль студента по ID                       |
| `GET`   | `/profiles/students`                   | MENTOR + ADMIN  | Каталог студентов с фильтрацией и пагинацией |

Query params для поиска студентов: `q`, `cityId`, `skillIds[]`, `employmentType`, `workFormat`, `page`, `size`, `sort` (whitelist: `createdAt`, `firstName`, `lastName`, `desiredPosition`).

### Профиль ментора

| Метод | Путь                     | Доступ    | Описание                                       |
| :---- | :----------------------- | :-------- | :--------------------------------------------- |
| `PUT` | `/profile/mentor`        | JWT       | Создать или обновить профиль                   |
| `GET` | `/profile/mentor/me`     | JWT       | Получить свой профиль                          |
| `GET` | `/profiles/mentors/{id}` | JWT       | Получить профиль ментора по ID                 |
| `GET` | `/profiles/mentors`      | Публичный | Поиск менторов с фильтрацией и пагинацией      |

Query params для поиска менторов: `q`, `skillIds[]`, `cityId`, `recruitmentStatus`, `mentoringType`, `mentoringChannel`, `page`, `size`, `sort` (whitelist: `createdAt`, `firstName`, `lastName`).

### Отзывы (`/reviews`)

| Метод    | Путь                                 | Доступ    | Описание                                    |
| :------- | :----------------------------------- | :-------- | :------------------------------------------ |
| `POST`   | `/reviews`                           | JWT       | Создать отзыв (только студент, статус COMPLETED) |
| `GET`    | `/reviews/by-request/{requestId}`    | JWT       | Отзыв по ID заявки (видят только участники) |
| `GET`    | `/profiles/mentors/{id}/reviews`     | Публичный | Публичные отзывы ментора (только `moderationStatus = VISIBLE`), пагинация, сортировка по createdAt DESC |
| `DELETE` | `/reviews/{id}`                      | JWT       | Удалить свой отзыв                          |

`ReviewResponse` содержит поле `moderationStatus` (`VISIBLE` / `HIDDEN` / `UNDER_REVIEW`). Публичные списки и средний рейтинг считаются только по `VISIBLE` — администратор может скрыть нарушение через `/admin/reviews/{id}/moderate`.

### Календарные сессии (`/sessions`)

| Метод  | Путь                            | Доступ | Описание                                                |
| :----- | :------------------------------ | :----- | :------------------------------------------------------ |
| `POST` | `/sessions`                     | JWT    | Создать сессию для принятой заявки (`mentoringRequestId`, `scheduledAt`, `durationMinutes` 15–480) |
| `GET`  | `/sessions`                     | JWT    | Сессии текущего пользователя (пагинация, sort whitelist: `scheduledAt`, `createdAt`, `status`) |
| `GET`  | `/sessions/{id}`                | JWT    | Сессия по ID (только участник)                          |
| `PUT`  | `/sessions/{id}/reschedule`     | JWT    | Перенести: `newScheduledAt`, `durationMinutes`, `reason` |
| `PUT`  | `/sessions/{id}/cancel`         | JWT    | Отменить с опциональной причиной                        |
| `PUT`  | `/sessions/{id}/complete`       | JWT    | Зафиксировать проведённую сессию                        |

Статусы: `SCHEDULED` → `RESCHEDULED` → `COMPLETED` / `CANCELLED` / `NO_SHOW`. Сессии можно создавать только для заявок в `ACCEPTED`.

### Жалобы (`/complaints`)

| Метод  | Путь                                  | Доступ | Описание                                            |
| :----- | :------------------------------------ | :----- | :-------------------------------------------------- |
| `POST` | `/complaints`                         | JWT    | Подать жалобу: `targetType` (`REVIEW` / `USER`), `targetId`, `reason` |

### Настройки уведомлений (`/profile/me/notifications`)

| Метод | Путь                              | Доступ | Описание                                              |
| :---- | :-------------------------------- | :----- | :---------------------------------------------------- |
| `GET` | `/profile/me/notifications`       | JWT    | Текущие флаги: `emailRequestEvents`, `emailSessionEvents`, `emailReviewEvents` |
| `PUT` | `/profile/me/notifications`       | JWT    | Обновить флаги (любое подмножество, `null` поля игнорируются) |

Outbox-паттерн: события публикуются как `NotificationOutbox`-записи, шедулер их рассылает с retry. Local-профиль использует MailHog (`http://localhost:8025`).

### Файлы (`/files`)

| Метод    | Путь                          | Доступ | Описание                                        |
| :------- | :---------------------------- | :----- | :---------------------------------------------- |
| `POST`   | `/files/resume`               | JWT    | Загрузить резюме (PDF, 5 МБ; требует профиль)   |
| `POST`   | `/files/portfolio`            | JWT    | Загрузить портфолио (PDF/JPEG/PNG, 10 МБ)       |
| `POST`   | `/files/avatar`               | JWT    | Загрузить аватар (JPEG/PNG/WebP, 2 МБ)          |
| `POST`   | `/files/chat-attachment`      | JWT    | Загрузить вложение в чат (любой формат, 20 МБ)  |
| `GET`    | `/files`                      | JWT    | Список активных файлов текущего пользователя    |
| `GET`    | `/files/{fileId}/download`    | JWT    | Скачать файл из MinIO (владелец или участник чата) |
| `DELETE` | `/files/{fileId}`             | JWT    | Soft-delete файла (204)                         |
| `PUT`    | `/files/{fileId}/replace`     | JWT    | Заменить файл (тот же тип, та же валидация)     |

Query params для `/files`: `type`, `page` (default `0`), `size` (default `50`), `sort` (whitelist: `uploadedAt`, `originalFilename`, `fileType`, `size`).

### Заявки на менторство (`/mentoring/requests`)

| Метод  | Путь                                           | Доступ | Описание                                           |
| :----- | :--------------------------------------------- | :----- | :------------------------------------------------- |
| `POST` | `/mentoring/requests`                          | JWT    | Создать заявку (студент→ментор или ментор→студент) |
| `GET`  | `/mentoring/requests`                          | JWT    | Мои заявки (фильтрация по статусу, пагинация)      |
| `GET`  | `/mentoring/requests/{id}`                     | JWT    | Получить заявку по ID                              |
| `PUT`  | `/mentoring/requests/{id}/view`                | JWT    | Адресат: SENT→REVIEWING                            |
| `PUT`  | `/mentoring/requests/{id}/needs-clarification` | JWT    | Адресат: запросить уточнение                       |
| `PUT`  | `/mentoring/requests/{id}/accept`              | JWT    | Адресат: принять заявку                            |
| `PUT`  | `/mentoring/requests/{id}/reject`              | JWT    | Адресат: отклонить заявку                          |
| `PUT`  | `/mentoring/requests/{id}/cancel`              | JWT    | Инициатор: отменить заявку                         |
| `PUT`  | `/mentoring/requests/{id}/complete`            | JWT    | Любой участник: завершить менторство               |

Статусы: `SENT` → `REVIEWING` → `NEEDS_CLARIFICATION` → `ACCEPTED` → `COMPLETED`. Терминальные: `REJECTED`, `CANCELLED`.

### Чат (`/chats`)

| Метод  | Путь                                  | Доступ | Описание                                                   |
| :----- | :------------------------------------ | :----- | :--------------------------------------------------------- |
| `GET`  | `/chats`                              | JWT    | Мои чаты (сортировка по lastMessageAt DESC NULLS LAST)     |
| `GET`  | `/chats/{chatId}`                     | JWT    | Получить чат по ID                                         |
| `GET`  | `/chats/by-request/{requestId}`       | JWT    | Получить чат по ID заявки                                  |
| `GET`  | `/chats/{chatId}/messages`            | JWT    | Сообщения чата (пагинация, createdAt DESC)                 |
| `GET`  | `/chats/{chatId}/messages/cursor`     | JWT    | Курсорная пагинация (`beforeMessageId`, `limit=20`)        |
| `POST` | `/chats/{chatId}/messages`            | JWT    | Отправить сообщение (`body`, `attachmentFileId`)           |
| `POST` | `/chats/{chatId}/read`                | JWT    | Отметить чат как прочитанный (204)                         |
| `POST` | `/chats/{chatId}/typing`              | JWT    | Событие печати `{ typing: true/false }` (SSE-пуш, 204)    |
| `GET`  | `/chats/events`                       | JWT    | SSE-поток событий реального времени                        |

SSE-события: `chat.message.created`, `chat.read`, `chat.typing`, `presence.changed`.

Ответ `/chats` содержит: `id`, `mentoringRequestId`, `studentUserId`, `mentorUserId`, `createdAt`, `lastMessage`, `lastMessageAt`, `lastSenderUserId`, `unreadCount`, `studentName`, `mentorName`, `mentoringRequestStatus`.

### Дашборд (`/dashboard`)

| Метод | Путь                       | Доступ | Описание                               |
| :---- | :------------------------- | :----- | :------------------------------------- |
| `GET` | `/dashboard/summary`       | JWT    | Сводка: заявки, чаты, заполненность профиля |
| `GET` | `/dashboard/activity`      | JWT    | Лента активности (limit=20, сортировка по occurredAt DESC) |

### Статистика ментора (`/mentor-stats`)

| Метод | Путь                | Доступ | Описание                                                   |
| :---- | :------------------ | :----- | :--------------------------------------------------------- |
| `GET` | `/mentor-stats/me`  | MENTOR | Рейтинг, кол-во отзывов, завершённые заявки, уровень BEGINNER/INTERMEDIATE/EXPERT |

Уровни: `BEGINNER` (0–4 завершённых), `INTERMEDIATE` (5–19), `EXPERT` (20+).

### Присутствие (`/presence`)

| Метод | Путь                   | Доступ | Описание                                  |
| :---- | :--------------------- | :----- | :---------------------------------------- |
| `GET` | `/presence/{userId}`   | JWT    | Статус online/offline и время последней активности |

### Администрирование (`/admin`)

| Метод | Путь                                  | Доступ | Описание                                             |
| :---- | :------------------------------------ | :----- | :--------------------------------------------------- |
| `PUT` | `/admin/users/{userId}/role`          | ADMIN  | Назначить роль STUDENT или MENTOR                    |
| `GET` | `/admin/users`                        | ADMIN  | Список пользователей с фильтрами (q, role, status)   |
| `GET` | `/admin/users/stats`                  | ADMIN  | Статистика пользователей (по ролям и статусам)       |
| `GET` | `/admin/complaints`                   | ADMIN  | Список жалоб (фильтры `status`, `targetType`, пагинация) |
| `PUT` | `/admin/complaints/{id}/resolve`      | ADMIN  | Разрешить жалобу: `status` (`RESOLVED` / `REJECTED`), `resolution` |
| `PUT` | `/admin/reviews/{id}/moderate`        | ADMIN  | Модерация отзыва: `moderationStatus` (`VISIBLE` / `HIDDEN` / `UNDER_REVIEW`) |
| `GET` | `/admin/audit`                        | ADMIN  | Аудит-лог: фильтры `action`, `adminUserId`, `from`, `to`, пагинация |
| `GET` | `/admin/notifications/outbox`         | ADMIN  | Outbox писем: фильтр `status` (`PENDING` / `SENT` / `FAILED`) |

Query params для `/admin/users`: `q`, `role`, `status`, `page`, `size`, `sort` (whitelist: `createdAt`, `email`, `status`).
Sort whitelist `/admin/complaints`: `createdAt`, `status`, `resolvedAt`. Sort whitelist `/admin/audit`: `createdAt`, `action`.

Модерация и разрешение жалоб публикуют события (`RoleChangedAuditEvent`, `ReviewModeratedAuditEvent`, `ComplaintResolvedAuditEvent`) → `AuditWriter` пишет запись в `admin_audit_log` через `@Transactional(REQUIRES_NEW)`, payload — JSONB.

---

## Архитектура

### Общая схема

```
┌─────────────┐     ┌────────────────────────────────────────────────────────┐
│   Клиент    │────>│                   Spring Boot                          │
│  (браузер,  │<────│                                                        │
│  мобильное  │     │  Controller ──> Service ──> Repository ──> JPA         │
│  приложение)│     │      │              │                        │         │
│             │     │  JWT Filter   FileStorage                PostgreSQL    │
│             │     │      │                │                                │
│             │     │  SSE Emitter        MinIO                              │
└─────────────┘     └────────────────────────────────────────────────────────┘
```

### Пакетная структура

```
com.example.it.mentor
├── config/         SecurityConfig, JpaConfig, OpenApiConfig, StorageConfig,
│                   StorageProperties, CacheConfig, OtpConfig, OtpProperties,
│                   MailExecutorConfig, NotificationOutboxProperties
├── filter/         MdcFilter (X-Request-Id → MDC requestId)
├── security/       JwtProvider, JwtAuthenticationFilter, UserDetailsServiceImpl,
│                   AppUserDetails, Http401EntryPoint, Http403AccessDeniedHandler
├── controller/     Auth, Dictionary, StudentProfile, MentorProfile, Profile,
│                   File, Mentoring, MentoringSession, Chat, Review, Complaint,
│                   Admin, AdminReview, AdminComplaint, AdminAudit,
│                   AdminNotification, NotificationPreferences, Dashboard,
│                   MentorStats, Presence                       (~20 шт.)
├── service/        Auth, User, Dictionary, StudentProfile, MentorProfile, Profile,
│                   MentoringRequest, MentoringSession, Chat, ChatSse, Typing,
│                   Review, Complaint, Admin, Dashboard, MentorStats, Presence,
│                   NotificationPreferences, FileStorage(Service),
│                   EmailService / LogEmailService / SmtpEmailService
│   ├── audit/      AuditWriter, AdminAuditEventListener (sealed AuditEvent → JSONB)
│   └── notification/  NotificationOutboxService, NotificationOutboxEntryProcessor,
│                       NotificationEventListener (события заявок/сессий/отзывов)
├── repository/     Spring Data репозитории + MentorProfileSpecification (~25 шт.)
├── event/audit/    sealed AuditEvent: RoleChangedAuditEvent,
│                   ReviewModeratedAuditEvent, ComplaintResolvedAuditEvent
├── entity/         BaseEntity, User, Role, UserStatus, RoleCode, PasswordResetToken,
│                   StudentProfile + связанные сущности, MentorProfile + MentorSkill,
│                   MentoringRequest, MentoringSession, Chat, ChatMessage,
│                   ChatReadState, Review, StoredFile, UserPresence, Complaint,
│                   AdminAuditLog, NotificationOutbox, UserNotificationPreferences
│   ├── dict/       DictCity, DictSkill, DictLanguage, DictInteractionType
│   └── enums/      WorkFormat, EmploymentType, EducationDegree, EducationForm,
│                   LanguageLevel, SkillLevel, RecruitmentStatus, FileType,
│                   FileStatus, MentoringType, MentoringChannel,
│                   MentoringDuration, MentoringRequestStatus,
│                   MentoringRequestDirection, MentoringSessionStatus,
│                   ReviewModerationStatus, ComplaintStatus,
│                   ComplaintTargetType, AuditAction, NotificationOutboxStatus
├── dto/            Java records: student/, mentor/, mentoring/, session/, chat/,
│                   review/, complaint/, audit/, notification/, dict/,
│                   dashboard/, admin/, presence/, sse/
├── mapper/         MapStruct: Auth, Dictionary, StudentProfile, MentorProfile,
│                   MentoringRequest, MentoringSession, Chat, Review,
│                   Complaint, AuditLog, Enum
└── exception/      ApiException → NotFoundException, ConflictException,
                    UnauthorizedException, ForbiddenException,
                    BusinessRuleViolationException, StorageException;
                    GlobalExceptionHandler
```

### Слои и ответственность

- **Controller** — тонкий слой: валидация `@Valid`, делегирование в сервис, HTTP-семантика.
- **Service** — вся бизнес-логика: получение текущего пользователя из SecurityContext, upsert-паттерн, бизнес-правила.
- **Repository** — Spring Data JPA с `@EntityGraph` для N+1 prevention, `Specification` для фильтрации.
- **Entity** — наследуют `BaseEntity` (id, createdAt, updatedAt через JPA Auditing).

---

## Безопасность

Stateless JWT-аутентификация:

1. Пользователь логинится через `POST /auth/login` — получает JWT.
2. Токен передаётся в заголовке `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` валидирует токен при каждом запросе.

### Публичные эндпоинты

```
/auth/**, /dictionaries/**
/swagger-ui/**, /v3/api-docs/**
/actuator/health
GET /profiles/mentors/*/reviews
```

### Ролевой доступ

| Роль      | Описание                                                 |
| :-------- | :------------------------------------------------------- |
| `STUDENT` | Роль по умолчанию. Профиль, поиск менторов, заявки, чат  |
| `MENTOR`  | Профиль ментора, управление заявками, статистика         |
| `ADMIN`   | Управление пользователями через `/admin/**`              |

### Формат ответа об ошибке

```json
{
  "status": 404,
  "errorCode": "NOT_FOUND",
  "message": "Профиль студента не найден",
  "path": "/profile/student/me",
  "details": null
}
```

| HTTP | Исключение                       | Когда                          |
| :--- | :------------------------------- | :----------------------------- |
| 400  | `MethodArgumentNotValidException` | Ошибки валидации `@Valid`      |
| 401  | `UnauthorizedException`          | Невалидный/отсутствующий JWT   |
| 403  | `ForbiddenException`             | Недостаточно прав              |
| 404  | `NotFoundException`              | Ресурс не найден               |
| 409  | `ConflictException`              | Дублирование (email)           |
| 422  | `BusinessRuleViolationException` | Нарушение бизнес-правила       |
| 500  | `StorageException`               | Ошибка файлового хранилища     |

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
  ├── stored_files (RESUME, PORTFOLIO, AVATAR, CHAT_ATTACHMENT)
  │
  ├── mentoring_requests (student_profile_id, mentor_profile_id)
  │       ├──── reviews (mentoring_request_id, reviewer_user_id, mentor_user_id, moderation_status)
  │       └──< mentoring_sessions (scheduled_at, duration_minutes, status)
  │
  ├── chats ──< chat_messages (senderUserId, body, attachmentFileId)
  │       └──< chat_read_states (userId, lastReadMessageId)
  │
  ├── user_presence (userId, status, lastSeenAt)
  ├── user_notification_preferences (emailRequestEvents, emailSessionEvents, emailReviewEvents)
  ├── notification_outbox (recipientEmail, subject, eventType, status, attempts, nextAttemptAt)
  │
  ├── complaints (target_type, target_id, reporter_user_id, status, resolved_by)
  └── admin_audit_log (admin_user_id, action, target_type, target_id, payload jsonb)
```

### Миграции

| Файл                                        | Описание                                                   |
| :------------------------------------------ | :--------------------------------------------------------- |
| `001_create_users_roles.sql`                | Таблицы `users`, `roles`, `user_roles`                     |
| `002_create_password_reset_tokens.sql`      | Токены сброса пароля                                       |
| `003_seed_roles.sql`                        | Начальные роли: STUDENT, MENTOR, ADMIN                     |
| `004_create_dict_tables.sql`                | Справочники: города, навыки, языки, типы взаимодействия    |
| `005_seed_dict_data.sql`                    | Наполнение справочников                                    |
| `006_create_student_profile.sql`            | Профиль студента + связанные таблицы                       |
| `007_create_mentor_profile.sql`             | Профиль ментора + навыки ментора                           |
| `008_create_stored_files.sql`               | Хранилище файлов                                           |
| `009_add_city_id_indexes.sql`               | Индексы на FK `city_id`                                    |
| `010_add_mentor_search_indexes.sql`         | Индексы для поиска менторов (GIN-тргм по имени)            |
| `011_add_version_columns.sql`               | Оптимистичная блокировка: колонки `version`                |
| `012_create_mentoring_requests.sql`         | Таблица `mentoring_requests` + индексы                     |
| `013_otp_password_reset.sql`               | OTP: поле `code` (6 цифр), `attempts`                      |
| `014_create_chats.sql`                      | Таблица `chats`                                            |
| `015_create_chat_messages.sql`              | Таблица `chat_messages`                                    |
| `016_create_reviews.sql`                    | Таблица `reviews` (unique по `mentoring_request_id`)       |
| `017_chat_read_states.sql`                  | Таблица `chat_read_states` + `last_message_at` в `chats`   |
| `018_extend_stored_files.sql`               | Колонка `status` (ACTIVE/DELETED), индексы                 |
| `019_student_profile_positions.sql`         | Поле `position` в `student_skills` и `student_languages`   |
| `020_create_user_presence.sql`              | Таблица `user_presence`                                    |
| `021_create_mentoring_sessions.sql`         | Таблица `mentoring_sessions` (планирование сессий)         |
| `022_create_notification_preferences.sql`   | `user_notification_preferences` (per-user email-флаги)     |
| `023_create_notification_outbox.sql`        | `notification_outbox` (надёжная доставка писем, retry)     |
| `024_create_complaints.sql`                 | Таблица `complaints` + индексы                             |
| `025_extend_reviews_moderation.sql`         | Колонки `moderation_status`, `moderated_by`, `moderated_at` в `reviews` |
| `026_create_admin_audit_log.sql`            | `admin_audit_log` (JSONB payload, индексы по `action`/`createdAt`) |

---

## Анализ кода (SonarQube)

SonarQube Community Edition с плагином [sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin).

```bash
# 1. Поднять SonarQube
docker compose up -d sonarqube

# 2. Открыть http://localhost:9090, войти admin/admin, сгенерировать токен

# 3. Запустить анализ
./mvnw clean verify sonar:sonar -Dsonar.branch.name=develop -Dsonar.token=<TOKEN>
```

---

## Тестирование

```bash
# Все unit-тесты (Surefire)
./mvnw test

# Один класс / метод
./mvnw test -Dtest=AuthControllerIT
./mvnw test -Dtest=AuthServiceTest#register_happyPath_shouldReturnResponse

# Полная верификация: unit + IT + JaCoCo (Failsafe)
./mvnw clean verify
```

### Docker Desktop ≥ 4.40 на macOS

После апгрейда Docker Desktop старая связка Testcontainers 1.20.4 + docker-java 3.4.2 отдаёт `BadRequest 400: client version 1.32 is too old`. Решение уже зашито в проект:

- `pom.xml`: `testcontainers.version=1.21.3` + явный override `docker-java-api`/`-transport-zerodep` на `3.5.1` через `<exclusions>`;
- `maven-surefire-plugin` и `maven-failsafe-plugin` пробрасывают `-Dapi.version=1.43` через `<systemPropertyVariables>` (env-переменная `DOCKER_API_VERSION` docker-java'ой игнорируется);
- На локальной macOS-машине нужен `~/.testcontainers.properties` с `docker.host=unix:///Users/<you>/Library/Containers/com.docker.docker/Data/docker.raw.sock` — обход «gate»-сокета Docker Desktop, который non-CLI клиентам отдаёт пустой `Info` со статусом 400. На Linux CI этот файл не нужен.

### Структура тестов

| Тип                  | Количество | Подход                                               |
| :------------------- | :--------- | :--------------------------------------------------- |
| Unit-тесты сервисов  | ~25 классов | `@ExtendWith(MockitoExtension.class)` + Mockito      |
| Интеграционные тесты | ~25 классов (309 кейсов) | `@SpringBootTest` + TestRestTemplate + Testcontainers |

IT-тест классы: `AdminControllerIT`, `AdminNotificationControllerIT`, `AdminUsersControllerIT`, `AuthControllerIT`, `AuthPasswordResetIT`, `ChatControllerIT`, `ChatReadControllerIT`, `DashboardControllerIT`, `DictionaryControllerIT`, `ErrorResponseFormatIT`, `FileControllerIT`, `MentorProfileControllerIT`, `MentorSearchControllerIT`, `MentoringRequestControllerIT`, `MentoringRequestMentorToStudentIT`, `MentoringRequestPaginationIT`, `MentoringRequestStateTransitionsIT`, `MentoringSessionControllerIT`, `NotificationPreferencesControllerIT`, `PresenceControllerIT`, `ProfileSummaryIT`, `ReviewControllerIT`, `StudentCatalogControllerIT`, `StudentProfileControllerIT`, `TypingControllerIT`.

### Соглашения

- **Именование:** `methodName_scenario_expectedOutcome`
- **Assertions:** AssertJ (`assertThat(...).isEqualTo(...)`)
- **Nested-классы:** `@Nested @DisplayName` для группировки
- **SecurityContext в тестах:** `SecurityContextHolder.setContext(new SecurityContextImpl(...))`

---

## Ключевые паттерны

### Upsert-стратегия профилей

```
findByUserId → (нет?) → builder.build()
                (есть?) → берём существующий
→ обновляем поля
→ save (получаем ID)
→ удаляем старые коллекции → создаём новые
→ save
→ findWithDetails → mapper.toResponse()
```

### Замена коллекций

```
childRepo.deleteAllByParent(profile)
profile.getChildren().clear()
profile.getChildren().addAll(newSet)
```

### N+1 prevention

`@NamedEntityGraph` на сущностях + `@EntityGraph` на репозиториях для профилей с вложенными коллекциями.

### Soft Delete + уникальный email

Поле `is_deleted` + частичный уникальный индекс `WHERE is_deleted = FALSE` гарантирует уникальность только среди активных пользователей.

### Кеш

`CacheConfig` определяет: `userDetails` (60 сек, 1000 записей), `dictionaries` (300 сек, 100 записей). После смены роли вызывается `UserDetailsServiceImpl.evictUserCache(email)`.

### SSE-события реального времени

При отправке сообщения и при событии `typing` сервис публикует SSE-событие другому участнику чата. Клиент подключается к `GET /chats/events` с JWT-токеном.

---

## Структура проекта

```
it.mentor/
├── docker-compose.yml                 PostgreSQL + MinIO + MailHog + SonarQube
├── pom.xml                            Maven-конфигурация
├── mvnw                               Maven Wrapper
├── CLAUDE.md                          Инструкции для AI-ассистента
│
├── src/main/
│   ├── java/com/example/it/mentor/
│   │   ├── Application.java
│   │   ├── config/                    (~9 классов)
│   │   ├── filter/                    (MdcFilter)
│   │   ├── security/                  (5 классов)
│   │   ├── controller/                (~18 контроллеров)
│   │   ├── service/                   (~15 сервисов)
│   │   ├── repository/                (~20 репозиториев + Specification)
│   │   ├── entity/                    (~20 сущностей + dict/ + enums/)
│   │   ├── dto/                       (~60 records)
│   │   ├── mapper/                    (8 маперов)
│   │   └── exception/                 (7 исключений + handler)
│   │
│   └── resources/
│       ├── application.yaml
│       ├── application-local.yaml
│       ├── application-test.yaml
│       └── db/changelog/
│           ├── db.changelog-master.yaml
│           └── changes/               (20 SQL-миграций)
│
└── src/test/java/com/example/it/mentor/
    ├── ApplicationTests.java
    ├── controller/                    (~22 IT-теста)
    ├── service/                       (~11 unit-тестов)
    ├── security/                      (1 тест)
    └── entity/                        (1 тест)
```

---

## Статистика кодовой базы

| Категория             | Количество |
| :-------------------- | :--------- |
| Тестов (JUnit 5)      | 309 IT + ~140 unit |
| IT-тест классов       | ~25        |
| Unit-тест классов     | ~25        |
| REST-эндпоинтов       | ~80        |
| SQL-миграций          | 26         |
| Таблиц в БД           | ~28        |
| Контроллеров          | ~20        |
| Сервисов              | ~20        |
| Репозиториев          | ~25        |
| JPA-сущностей         | ~25        |
| Перечислений (enum)   | ~20        |

---

## Roadmap

- [x] **Stage 1** — Аутентификация (регистрация, логин, JWT)
- [x] **Stage 2** — Справочники, профили студента/ментора, загрузка файлов
- [x] **Stage 3** — Поиск менторов: фильтрация, пагинация, GIN-индекс
- [x] **Stage 4** — Система заявок на менторство: state machine, 7 статусов
- [x] **Stage 5** — OTP сброс пароля (6-значный код, 3 попытки, 15 мин TTL)
- [x] **Stage 6** — Чат: автосоздание при принятии заявки, сообщения, вложения
- [x] **Stage 7** — Логирование (MDC, structured JSON), Caffeine-кеш, JaCoCo, SonarQube
- [x] **Stage 8** — GitHub Actions CI (Java 25 + Testcontainers, RYUK disabled)
- [x] **Stage 9** — Отзывы на менторов: рейтинг 1–5, публичные страницы
- [x] **Phase 1** — Дашборд, SSE-чат в реальном времени, каталог студентов, profile UX (PATCH + completion), file manager (list/download/delete/replace), статистика ментора, Admin users/stats
- [x] **Phase 2** — Typing events, user presence (online/offline)
- [x] **Stage 10** — Календарные сессии менторинга (`/sessions/**`), email-уведомления (outbox + retry + per-user preferences)
- [x] **Phase 4** — Admin Moderation & Audit: жалобы (`/complaints`, `/admin/complaints/**`), модерация отзывов (`/admin/reviews/{id}/moderate`), аудит-лог админ-действий (`/admin/audit`), просмотр email-outbox (`/admin/notifications/outbox`)
