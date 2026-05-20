# IT Mentor API — Frontend Integration Guide

> Полная спецификация backend API для разработки фронтенда.
> Все контракты верифицированы по исходному коду (Spring Boot 4.0.3, Java 25).
> Интерактивная документация: Swagger UI — `{BASE_URL}/swagger-ui.html`

---

## Оглавление

- [1. Общая информация](#1-общая-информация)
- [2. Аутентификация](#2-аутентификация)
- [3. Обработка ошибок](#3-обработка-ошибок)
- [4. API Reference](#4-api-reference)
  - [4.1 Auth](#41-auth)
  - [4.2 Dictionaries](#42-dictionaries)
  - [4.3 Profile Summary](#43-profile-summary)
  - [4.4 Student Profile](#44-student-profile)
  - [4.5 Mentor Profile](#45-mentor-profile)
  - [4.6 Files](#46-files)
  - [4.7 Mentor Search](#47-mentor-search)
  - [4.8 Mentoring Requests](#48-mentoring-requests)
  - [4.9 Chat](#49-chat)
  - [4.10 Admin](#410-admin)
  - [4.11 Reviews](#411-reviews)
  - [4.12 Dashboard](#412-dashboard)
  - [4.13 Mentor Stats](#413-mentor-stats)
  - [4.14 Presence](#414-presence)
  - [4.15 Mentoring Sessions](#415-mentoring-sessions)
  - [4.16 Notification Preferences](#416-notification-preferences)
  - [4.17 Complaints](#417-complaints)
  - [4.18 Admin: Moderation & Audit](#418-admin-moderation--audit)
  - [4.19 Admin: User Status (Stage 11)](#419-admin-user-status-stage-11)
  - [4.20 Admin: Dictionary CRUD (Stage 11)](#420-admin-dictionary-crud-stage-11)
- [5. TypeScript-типы (полная карта DTO)](#5-typescript-типы-полная-карта-dto)
- [6. Enum-справочник](#6-enum-справочник)
- [7. Загрузка файлов](#7-загрузка-файлов)
- [8. Типичные сценарии (User Flows)](#8-типичные-сценарии-user-flows)
- [9. Частые вопросы и подводные камни](#9-частые-вопросы-и-подводные-камни)

---

## 1. Общая информация

| Параметр | Значение |
|---|---|
| Base URL (local) | `http://localhost:8080` |
| Content-Type | `application/json` (кроме file upload — `multipart/form-data`) |
| Авторизация | `Authorization: Bearer <JWT>` |
| Время жизни токена | 24 часа (86 400 000 ms) |
| Swagger UI | `{BASE_URL}/swagger-ui.html` |
| OpenAPI JSON | `{BASE_URL}/v3/api-docs` |
| Max file upload | по типу файла (см. раздел 7) |
| Даты | ISO 8601: `"2025-03-19"` (LocalDate), `"2025-03-19T14:30:00+03:00"` (OffsetDateTime), `"2025-03-19T11:30:00Z"` (Instant) |
| Null-поля | В DTO null-поля **присутствуют** как `null`; в ErrorResponse отсутствуют (`@JsonInclude(NON_NULL)`) |

---

## 2. Аутентификация

### Схема: Stateless JWT (HMAC-SHA256)

```
┌─────────┐     POST /auth/login       ┌──────────┐
│ Frontend │ ──────────────────────────→ │ Backend  │
│          │ ←────────────────────────── │          │
│          │   { accessToken, user }     │          │
│          │                             │          │
│          │   GET /profile/me           │          │
│          │   Authorization: Bearer ... │          │
│          │ ──────────────────────────→ │          │
│          │ ←────────────────────────── │          │
│          │   { role, profileId, ... }  │          │
└─────────┘                             └──────────┘
```

### Публичные эндпоинты (без токена)

```
POST   /auth/register
POST   /auth/login
POST   /auth/password/forgot
POST   /auth/password/reset
GET    /dictionaries/**
GET    /swagger-ui/**
GET    /v3/api-docs/**
GET    /actuator/health
GET    /profiles/mentors/*/reviews
```

**Все остальные эндпоинты требуют JWT в заголовке.**

### Как отправлять токен

```http
GET /profile/me HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIi...
```

```typescript
// fetch
const response = await fetch(`${BASE_URL}/profile/me`, {
  headers: {
    'Authorization': `Bearer ${token}`,
  },
});

// axios
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
```

### Что делать при 401

Токен истёк или невалиден. Очистить хранилище, перенаправить на `/login`.

### JWT `tokenVersion` (Stage 11)

Backend помещает в JWT claim `tv` — текущий `tokenVersion` пользователя. На каждом запросе `JwtAuthenticationFilter` сверяет `jwt.tv` с актуальным значением в БД. Когда админ блокирует/удаляет пользователя через `PUT /admin/users/{userId}/status`, `tokenVersion` инкрементится, и все ранее выписанные токены этого пользователя становятся невалидными мгновенно — фронт получит `401` на ближайшем запросе. После повторного логина в новом JWT будет уже актуальный `tv`. Возврат пользователя в `ACTIVE` (unblock) не возвращает `tokenVersion` назад — старые токены остаются мёртвыми.

Что это значит для фронтенда:
- Не пытайтесь продлевать JWT локально — единственный источник правды по `tv` это `/auth/login`.
- На `401` всегда чистите хранилище и переводите пользователя на `/login` (даже если визуально «только что был залогинен»).
- Если у вас есть SSE-сессия (`/chats/events`) — она тоже обвалится с `401`, переподключайтесь только после успешного логина.

---

## 3. Обработка ошибок

### Единый формат ответа об ошибке

Бэкенд **всегда** возвращает JSON (никогда HTML). Формат единый для всех ошибок:

```typescript
interface ErrorResponse {
  timestamp: string;   // ISO 8601 Instant, e.g. "2025-03-19T11:30:00.123Z"
  status: number;      // HTTP status code
  error: string;       // Код ошибки (см. таблицу ниже)
  message: string;     // Человекочитаемое сообщение (на русском)
  path: string;        // URI запроса
  details?: string[];  // Только для VALIDATION_ERROR — список полей
}
```

### Таблица кодов ошибок

| HTTP Status | `error` | Когда возникает | `details` |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Невалидные поля в request body | `["firstName: must not be blank", "email: Некорректный формат email"]` |
| 400 | `MALFORMED_JSON` | Невалидный JSON в теле запроса | — |
| 401 | `UNAUTHORIZED` | Нет токена / токен невалиден / токен истёк | — |
| 403 | `FORBIDDEN` | Нет прав доступа | — |
| 429 | `TOO_MANY_REQUESTS` | Превышен rate-limit на `/auth/login`, `/auth/password/**` (10 req/min per IP) | — |
| 404 | `NOT_FOUND` | Ресурс не найден | — |
| 405 | `METHOD_NOT_ALLOWED` | Неправильный HTTP-метод | — |
| 409 | `CONFLICT` | Конфликт (например, email уже зарегистрирован) | — |
| 422 | `UNPROCESSABLE_ENTITY` | Бизнес-правило нарушено | — |
| 500 | `INTERNAL_SERVER_ERROR` | Ошибка сервера | — |

### Пример: ошибка валидации

```json
{
  "timestamp": "2025-03-19T11:30:00.123Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Ошибка валидации",
  "path": "/auth/register",
  "details": [
    "email: Некорректный формат email",
    "password: Пароль должен содержать не менее 8 символов"
  ]
}
```

### Рекомендация для фронтенда

```typescript
async function apiCall<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`,
      ...options?.headers,
    },
  });

  if (!res.ok) {
    const error: ErrorResponse = await res.json();

    if (res.status === 401) {
      redirectToLogin();
      throw error;
    }

    if (res.status === 400 && error.error === 'VALIDATION_ERROR') {
      // error.details = ["firstName: must not be blank", ...]
      throw error;
    }

    throw error;
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return res.json();
}
```

---

## 4. API Reference

### 4.1 Auth

#### `POST /auth/register` — Регистрация

**Auth:** Не требуется

**Request:**
```json
{
  "email": "student@example.com",
  "password": "securePass123",
  "firstName": "Иван",
  "lastName": "Петров"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `email` | string | да | `@NotBlank`, `@Email` |
| `password` | string | да | `@NotBlank`, min 8 символов |
| `firstName` | string | да | `@NotBlank`, max 100 символов |
| `lastName` | string | да | `@NotBlank`, max 100 символов |

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "student@example.com",
  "roles": ["STUDENT"]
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 409 | Email уже зарегистрирован |

**Важно:** Email нормализуется к нижнему регистру. `"User@Example.COM"` → `"user@example.com"`.

---

#### `POST /auth/login` — Вход

**Auth:** Не требуется

**Request:**
```json
{
  "email": "student@example.com",
  "password": "securePass123"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "roles": ["STUDENT"],
    "status": "EMAIL_NOT_CONFIRMED"
  }
}
```

> ℹ️ Сразу после `POST /auth/register` пользователь имеет `status: "EMAIL_NOT_CONFIRMED"`.
> `ACTIVE` появляется только после подтверждения email или ручной активации админом.

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 401 | Неверный email или пароль |

---

#### `GET /auth/me` — Текущий пользователь

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "student@example.com",
  "roles": ["STUDENT"],
  "status": "ACTIVE"
}
```

---

#### `POST /auth/password/forgot` — Запрос сброса пароля

**Auth:** Не требуется

**Request:**
```json
{ "email": "student@example.com" }
```

**Response:** `200 OK` (пустое тело)

**Поведение:** Всегда возвращает 200, даже если email не найден (защита от enumeration).

---

#### `POST /auth/password/reset` — Сброс пароля (OTP)

**Auth:** Не требуется

**Поток:** Пользователь получает на email 6-значный числовой код (TTL 15 минут, 3 попытки).

**Request:**
```json
{
  "email": "student@example.com",
  "code": "123456",
  "newPassword": "NewSecurePass123"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `email` | string | да | `@NotBlank`, `@Email` |
| `code` | string | да | ровно 6 цифр (`\d{6}`) |
| `newPassword` | string | да | 8–128 символов, минимум 1 заглавная + 1 строчная + 1 цифра |

**Response:** `200 OK` (пустое тело)

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 404 | Код не найден / истёк / превышен лимит попыток |

---

### 4.2 Dictionaries

Справочники публичные, кешируются клиентом. Используются для выпадающих списков.

#### `GET /dictionaries/cities`

**Response:** `200 OK`
```json
[
  { "id": 1, "name": "Москва", "region": "Московская область", "country": "Россия" }
]
```

#### `GET /dictionaries/skills`

**Response:** `200 OK`
```json
[
  { "id": 1, "name": "Java", "category": "Backend" }
]
```

#### `GET /dictionaries/languages`

**Response:** `200 OK`
```json
[
  { "id": 1, "name": "Английский", "code": "en" }
]
```

#### `GET /dictionaries/interaction-types`

**Response:** `200 OK`
```json
[
  { "id": 1, "name": "Менторинг", "description": "Индивидуальное сопровождение ментором" }
]
```

---

### 4.3 Profile Summary

#### `GET /profile/me` — Сводка профиля текущего пользователя

**Auth:** Требуется JWT

Первый запрос после логина — определяет роль и наличие профиля.

**Response:** `200 OK`
```json
{
  "role": "STUDENT",
  "profileId": 42,
  "profileExists": true
}
```

| Поле | Тип | Описание |
|---|---|---|
| `role` | string | `"STUDENT"` / `"MENTOR"` / `"ADMIN"` |
| `profileId` | number \| null | ID профиля (null если профиль не создан) |
| `profileExists` | boolean | Есть ли заполненный профиль |

**Логика:** `ADMIN` > `MENTOR` > `STUDENT` (возвращается наивысшая роль).

---

### 4.4 Student Profile

#### `PUT /profile/student` — Создать или обновить профиль студента

**Auth:** Требуется JWT

**Семантика:** Upsert. Если профиль не существует — создаётся. Если существует — полностью перезаписывается (все поля, включая коллекции).

**Request:**
```json
{
  "firstName": "Иван",
  "lastName": "Петров",
  "middleName": "Сергеевич",
  "phone": "+7 999 123-45-67",
  "cityId": 1,
  "desiredPosition": "Backend-разработчик",
  "hoursPerWeek": 40,
  "availableFrom": "2025-04-01",
  "about": "Студент 4 курса, ищу практику...",
  "max": "linkedin.com/in/ivan-petrov",
  "employmentTypes": ["FULL_TIME", "INTERNSHIP"],
  "workFormats": ["REMOTE", "HYBRID"],
  "educations": [
    {
      "institution": "МГУ им. М.В. Ломоносова",
      "specialty": "Прикладная математика и информатика",
      "degree": "BACHELOR",
      "educationForm": "FULL_TIME",
      "startYear": 2021,
      "graduationYear": 2025
    }
  ],
  "languages": [
    { "languageId": 1, "level": "B2" }
  ],
  "skills": [
    { "skillId": 1, "level": "INTERMEDIATE" },
    { "skillId": 2, "level": "BEGINNER" }
  ]
}
```

**Response:** `200 OK`
```json
{
  "id": 42,
  "userId": 1,
  "firstName": "Иван",
  "lastName": "Петров",
  "middleName": "Сергеевич",
  "phone": "+7 999 123-45-67",
  "city": { "id": 1, "name": "Москва", "region": "Московская область", "country": "Россия" },
  "desiredPosition": "Backend-разработчик",
  "hoursPerWeek": 40,
  "availableFrom": "2025-04-01",
  "about": "Студент 4 курса, ищу практику...",
  "max": "linkedin.com/in/ivan-petrov",
  "employmentTypes": ["FULL_TIME", "INTERNSHIP"],
  "workFormats": ["REMOTE", "HYBRID"],
  "educations": [
    {
      "id": 10,
      "institution": "МГУ им. М.В. Ломоносова",
      "specialty": "Прикладная математика и информатика",
      "degree": "BACHELOR",
      "educationForm": "FULL_TIME",
      "startYear": 2021,
      "graduationYear": 2025
    }
  ],
  "languages": [
    {
      "id": 5,
      "language": { "id": 1, "name": "Английский", "code": "en" },
      "level": "B2",
      "position": 0
    }
  ],
  "skills": [
    {
      "id": 7,
      "skill": { "id": 1, "name": "Java", "category": "Backend" },
      "level": "INTERMEDIATE",
      "position": 0
    }
  ],
  "resumeFileId": null
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 401 | Нет токена |
| 404 | `cityId`, `languageId` или `skillId` не найден |

**Важные нюансы:**
- `educations`, `languages`, `skills` при каждом PUT **полностью заменяются**.
- `cityId` в request → развёрнутый `city` в response.
- `resumeFileId` обновляется через `POST /files/resume`.

---

#### `PATCH /profile/student` — Частичное обновление профиля студента

**Auth:** Требуется JWT

**Семантика:** Обновляются только явно переданные поля. Коллекции (`educations`, `languages`, `skills`, `employmentTypes`, `workFormats`) НЕ затрагиваются этим методом.

**Request:** Все поля опциональны.
```json
{
  "firstName": "Иван",
  "desiredPosition": "Senior Backend Developer",
  "hoursPerWeek": 20
}
```

| Поле | Тип | Описание |
|---|---|---|
| `firstName` | string \| null | Имя |
| `lastName` | string \| null | Фамилия |
| `middleName` | string \| null | Отчество |
| `phone` | string \| null | Телефон |
| `desiredPosition` | string \| null | Желаемая должность |
| `hoursPerWeek` | number \| null | Часов в неделю (0..168) |
| `availableFrom` | string \| null | Дата готовности (`"YYYY-MM-DD"`) |
| `about` | string \| null | О себе |
| `maxContact` | string \| null | Доп. контакт |
| `cityId` | number \| null | ID города (`null` — не трогать; `0` — очистить город) |
| `employmentTypes` | string[] \| null | Типы занятости (полная замена) |
| `workFormats` | string[] \| null | Форматы работы (полная замена) |

**Response:** `200 OK` — полный `StudentProfileResponse`

---

#### `GET /profile/student/me` — Мой профиль студента

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична ответу `PUT /profile/student`

**Ошибки:** `404` если профиль не создан.

---

#### `GET /profile/student/me/completion` — Процент заполненности профиля

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "percent": 75,
  "mainDone": true,
  "aboutDone": true,
  "skillsDone": true,
  "resumeDone": false
}
```

| Поле | Тип | Описание |
|---|---|---|
| `percent` | number | Итоговый процент заполненности (0–100) |
| `mainDone` | boolean | Основные поля заполнены (firstName, lastName, city, desiredPosition, phone) |
| `aboutDone` | boolean | Поле `about` заполнено |
| `skillsDone` | boolean | Добавлен хотя бы один навык |
| `resumeDone` | boolean | Загружено резюме |

---

#### `PUT /profile/student/skills` — Заменить список навыков

**Auth:** Требуется JWT

**Семантика:** Полная замена. Передайте пустой массив для удаления всех навыков.

**Request:**
```json
{
  "skills": [
    { "skillId": 1, "level": "INTERMEDIATE", "position": 0 },
    { "skillId": 2, "level": "BEGINNER", "position": 1 }
  ]
}
```

| Поле | Тип | Обязательное | Описание |
|---|---|---|---|
| `skillId` | number | да | FK → `/dictionaries/skills` |
| `level` | string | да | Enum `SkillLevel` |
| `position` | number | да | Порядок отображения (0-based) |

**Response:** `200 OK` — полный `StudentProfileResponse`

---

#### `PUT /profile/student/languages` — Заменить список языков

**Auth:** Требуется JWT

**Request:**
```json
{
  "languages": [
    { "languageId": 1, "level": "B2", "position": 0 }
  ]
}
```

**Response:** `200 OK` — полный `StudentProfileResponse`

---

#### `GET /profile/student/me/files` — Файлы студента

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "resume": {
    "id": 101,
    "originalFilename": "Resume_Ivan.pdf",
    "contentType": "application/pdf",
    "size": 245760,
    "fileType": "RESUME",
    "status": "ACTIVE",
    "previewUrl": null,
    "uploadedAt": "2025-03-19T14:30:00+03:00"
  },
  "portfolioCount": 2,
  "avatarFileId": 100
}
```

| Поле | Тип | Описание |
|---|---|---|
| `resume` | FileResponse \| null | Активное резюме или null |
| `portfolioCount` | number | Количество файлов портфолио |
| `avatarFileId` | number \| null | ID аватара или null |

---

#### `GET /profiles/students/{id}` — Профиль студента по ID

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична `PUT /profile/student`

---

#### `GET /profiles/students` — Каталог студентов

**Auth:** MENTOR или ADMIN

**Query params:**

| Параметр | Тип | Описание |
|---|---|---|
| `q` | string | Поиск по имени/фамилии/желаемой должности |
| `cityId` | number | Фильтр по городу |
| `skillIds` | number[] | Фильтр по навыкам (`?skillIds=1&skillIds=3`) |
| `employmentType` | string | Фильтр по типу занятости |
| `workFormat` | string | Фильтр по формату работы |
| `page` | number | default `0` |
| `size` | number | default `20`, max `50` |
| `sort` | string | whitelist: `createdAt`, `firstName`, `lastName`, `desiredPosition` |

**Response:** `200 OK` — `PagedResponse<StudentProfileResponse>`

---

### 4.5 Mentor Profile

#### `PUT /profile/mentor` — Создать или обновить профиль ментора

**Auth:** Требуется JWT

**Request:**
```json
{
  "firstName": "Алексей",
  "lastName": "Смирнов",
  "middleName": "Дмитриевич",
  "position": "Senior Backend Developer",
  "department": "Platform Engineering",
  "cityId": 1,
  "phone": "+7 999 987-65-43",
  "max": "t.me/alexey_smirnov",
  "description": "10+ лет опыта в Java/Spring.",
  "expectations": "Минимум 10 часов в неделю на практику.",
  "canHelpWith": "Backend, System Design, собеседования",
  "mentoringType": "INTERNSHIP",
  "mentoringChannel": "MIXED",
  "mentoringFrequency": "2 раза в неделю",
  "mentoringDuration": "THREE_MONTHS",
  "menteeLimit": 5,
  "recruitmentStatus": "OPEN",
  "skills": [
    { "skillId": 1, "level": "CONFIDENT" }
  ]
}
```

**Response:** `200 OK`
```json
{
  "id": 10,
  "userId": 2,
  "firstName": "Алексей",
  "lastName": "Смирнов",
  "middleName": "Дмитриевич",
  "position": "Senior Backend Developer",
  "department": "Platform Engineering",
  "city": { "id": 1, "name": "Москва", "region": "Московская область", "country": "Россия" },
  "phone": "+7 999 987-65-43",
  "max": "t.me/alexey_smirnov",
  "description": "10+ лет опыта в Java/Spring.",
  "expectations": "Минимум 10 часов в неделю на практику.",
  "canHelpWith": "Backend, System Design, собеседования",
  "mentoringType": "INTERNSHIP",
  "mentoringChannel": "MIXED",
  "mentoringFrequency": "2 раза в неделю",
  "mentoringDuration": "THREE_MONTHS",
  "menteeLimit": 5,
  "recruitmentStatus": "OPEN",
  "skills": [
    {
      "id": 15,
      "skill": { "id": 1, "name": "Java", "category": "Backend" },
      "level": "CONFIDENT"
    }
  ]
}
```

**Важные нюансы:**
- `recruitmentStatus`: при `null` — сбрасывается к `"OPEN"` (NOT NULL поле).
- `skills` при каждом PUT полностью заменяются.

---

#### `GET /profile/mentor/me` — Мой профиль ментора

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична `PUT /profile/mentor`

---

#### `GET /profiles/mentors/{id}` — Профиль ментора по ID

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична `PUT /profile/mentor`

---

### 4.6 Files

Файлы загружаются как `multipart/form-data`. Имя поля формы: **`file`**.

#### `POST /files/resume` — Загрузить резюме

**Auth:** Требуется JWT

- Формат: только PDF (`application/pdf`)
- Размер: max 5 MB
- Предусловие: должен существовать профиль студента
- Поведение: старое резюме автоматически soft-deletes, новое становится активным

**Request:** `Content-Type: multipart/form-data`, поле `file`

**Response:** `201 Created`
```json
{
  "id": 100,
  "originalFilename": "Resume_Ivan_Petrov.pdf",
  "contentType": "application/pdf",
  "size": 245760,
  "fileType": "RESUME",
  "status": "ACTIVE",
  "previewUrl": null,
  "uploadedAt": "2025-03-19T14:30:00+03:00"
}
```

---

#### `POST /files/portfolio` — Загрузить файл портфолио

**Auth:** Требуется JWT

- Формат: PDF, JPEG, PNG
- Размер: max 10 MB

**Response:** `201 Created` — структура идентична `FileResponse`

---

#### `POST /files/avatar` — Загрузить аватар

**Auth:** Требуется JWT

- Формат: JPEG, PNG, WebP
- Размер: max 2 MB
- Поведение: старый аватар автоматически soft-deletes

**Response:** `201 Created` — структура идентична `FileResponse`

---

#### `POST /files/chat-attachment` — Загрузить вложение в чат

**Auth:** Требуется JWT

- Формат: любой (explicit safe MIME allow-list на сервере)
- Размер: max 20 MB

**Response:** `201 Created` — `FileResponse` с `fileType: "CHAT_ATTACHMENT"`

---

#### `GET /files` — Список файлов текущего пользователя

**Auth:** Требуется JWT

Возвращает только файлы со статусом `ACTIVE` (удалённые не показываются).

**Query params:**

| Параметр | Тип | Описание |
|---|---|---|
| `type` | string | Фильтр по `FileType`: `RESUME`, `PORTFOLIO`, `AVATAR`, `CHAT_ATTACHMENT` |
| `page` | number | default `0` |
| `size` | number | default `50` |
| `sort` | string | whitelist: `uploadedAt`, `originalFilename`, `fileType`, `size` (default: `uploadedAt,desc`) |

**Response:** `200 OK` — `PagedResponse<FileResponse>`

```typescript
// Пример: получить только портфолио
const portfolios = await apiCall<PagedResponse<FileResponse>>(
  `${BASE_URL}/files?type=PORTFOLIO&sort=uploadedAt,desc`
);
```

---

#### `GET /files/{fileId}/download` — Скачать файл

**Auth:** Требуется JWT (владелец файла или участник чата для `CHAT_ATTACHMENT`)

**Response:** `200 OK` — бинарный поток с заголовками:
- `Content-Type`: MIME-тип файла
- `Content-Disposition: attachment; filename="..."`

```typescript
async function downloadFile(fileId: number, token: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/files/${fileId}/download`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });

  if (!response.ok) throw await response.json();

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = getFilenameFromContentDisposition(response.headers.get('Content-Disposition') ?? '');
  a.click();
  URL.revokeObjectURL(url);
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 403 | Нет доступа к файлу |
| 404 | Файл не найден или удалён |

---

#### `DELETE /files/{fileId}` — Удалить файл

**Auth:** Требуется JWT (только владелец)

**Response:** `204 No Content`

Удаление мягкое (soft delete). Файл помечается статусом `DELETED` и не возвращается в `GET /files`. Физически удалить из MinIO через этот API нельзя.

---

#### `PUT /files/{fileId}/replace` — Заменить файл

**Auth:** Требуется JWT (только владелец)

Загружает новый файл, заменяя существующий. Тип должен совпадать — для замены резюме нужен PDF, для аватара — JPEG/PNG/WebP.

**Request:** `Content-Type: multipart/form-data`, поле `file`

**Response:** `200 OK` — обновлённый `FileResponse`

---

### 4.7 Mentor Search

#### `GET /profiles/mentors` — Поиск менторов

**Auth:** Не требуется (публичный endpoint)

**Query params:**

| Параметр | Тип | Описание |
|---|---|---|
| `q` | string | Поиск по имени/фамилии (ILIKE, GIN-тргм) |
| `skillIds` | number[] | Фильтр по навыкам (OR-логика): `?skillIds=1&skillIds=3` |
| `cityId` | number | Фильтр по городу |
| `recruitmentStatus` | string | `OPEN` / `PAUSED` / `CLOSED` |
| `mentoringType` | string | `PRACTICE` / `INTERNSHIP` / `PROJECT` |
| `mentoringChannel` | string | `CHAT` / `CALLS` / `MIXED` |
| `page` | number | default `0`, min `0` |
| `size` | number | default `20`, min `1`, max `50` |
| `sort` | string | default `createdAt,desc`; whitelist: `createdAt`, `firstName`, `lastName` |

**Response:** `200 OK` — `PagedResponse<MentorCardResponse>`

```json
{
  "content": [
    {
      "id": 10,
      "userId": 2,
      "firstName": "Алексей",
      "lastName": "Смирнов",
      "middleName": "Дмитриевич",
      "position": "Java Tech Lead",
      "department": "Platform Engineering",
      "city": { "id": 1, "name": "Москва", "region": "Московская область", "country": "Россия" },
      "mentoringType": "PRACTICE",
      "mentoringChannel": "CALLS",
      "mentoringDuration": "THREE_MONTHS",
      "menteeLimit": 5,
      "recruitmentStatus": "OPEN",
      "skills": [
        { "id": 15, "skill": { "id": 1, "name": "Java", "category": "Backend" }, "level": "CONFIDENT" }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

**Важно:** `MentorCardResponse` — облегчённая карточка, **не содержит** `description`, `expectations`, `canHelpWith`, `phone`, `max`. Для полного профиля — `GET /profiles/mentors/{id}`.

---

### 4.8 Mentoring Requests

#### Жизненный цикл заявки

```
SENT ──────────── (cancel инициатор) ──────────── CANCELLED
  │
  └── view (адресат) ──→ REVIEWING
        │
        ├── needs-clarification ──→ NEEDS_CLARIFICATION
        │         └── (возврат через сообщение)
        ├── accept ──→ ACCEPTED ──→ complete ──→ COMPLETED
        └── reject ──→ REJECTED
```

**Правила доступа:**
- `view`, `needs-clarification`, `accept`, `reject` — только **адресат**
- `cancel` — только **инициатор** (в статусах SENT / REVIEWING / NEEDS_CLARIFICATION)
- `complete` — любой **участник** (только в статусе ACCEPTED)

---

#### `POST /mentoring/requests` — Создать заявку

**Auth:** Требуется JWT (STUDENT или MENTOR)

**Request:**
```json
{
  "targetProfileId": 10,
  "goalType": "PRACTICE",
  "message": "Хочу пройти практику по Java/Spring. Готов уделять 20 часов в неделю."
}
```

| Поле | Тип | Обязательное | Описание |
|---|---|---|---|
| `targetProfileId` | number | да | ID профиля адресата (mentor_profile.id или student_profile.id) |
| `goalType` | string | да | `MentoringType` |
| `message` | string | да | max 2000 символов |

**Response:** `201 Created` — `MentoringRequestResponse`

**Ошибки:**
| Код | Когда |
|---|---|
| 404 | Профиль отправителя или получателя не найден |
| 409 | Активная заявка уже существует |
| 422 | Ментор не принимает студентов / заявка самому себе |

---

#### `GET /mentoring/requests` — Мои заявки

**Auth:** Требуется JWT

**Query params:** `status`, `page` (default `0`), `size` (default `20`, max `100`)

**Response:** `200 OK` — `PagedResponse<MentoringRequestResponse>` (сортировка по `createdAt DESC`)

---

#### `GET /mentoring/requests/{id}` — Заявка по ID

**Response:** `200 OK` — `MentoringRequestResponse`. `403` если не участник.

---

#### `PUT /mentoring/requests/{id}/view`

**Auth:** Только адресат. SENT → REVIEWING.

**Response:** `200 OK` — `MentoringRequestResponse`

---

#### `PUT /mentoring/requests/{id}/needs-clarification`

**Auth:** Только адресат.

**Request:** `{ "clarificationNote": "..." }` (обязательно, max 2000)

**Response:** `200 OK` — `MentoringRequestResponse` со статусом `NEEDS_CLARIFICATION`

---

#### `PUT /mentoring/requests/{id}/accept`

**Auth:** Только адресат.

**Особенность:** проверяется `menteeLimit` (422 если превышен).

**Response:** `200 OK` — `MentoringRequestResponse` со статусом `ACCEPTED`

---

#### `PUT /mentoring/requests/{id}/reject`

**Auth:** Только адресат.

**Request:** `{ "reason": "..." }` (опциональный, max 2000)

**Response:** `200 OK` — `MentoringRequestResponse` со статусом `REJECTED`

---

#### `PUT /mentoring/requests/{id}/cancel`

**Auth:** Только инициатор. Только в SENT / REVIEWING / NEEDS_CLARIFICATION.

**Response:** `200 OK` — `MentoringRequestResponse` со статусом `CANCELLED`

---

#### `PUT /mentoring/requests/{id}/complete`

**Auth:** Любой участник. Только в ACCEPTED.

**Response:** `200 OK` — `MentoringRequestResponse` со статусом `COMPLETED`

---

#### Структура `MentoringRequestResponse`

```json
{
  "id": 1,
  "studentProfileId": 42,
  "mentorProfileId": 10,
  "studentProfile": { "id": 42, "firstName": "Иван", "lastName": "Петров" },
  "mentorProfile": { "id": 10, "firstName": "Алексей", "lastName": "Смирнов", "position": "Java Tech Lead" },
  "direction": "STUDENT_TO_MENTOR",
  "status": "SENT",
  "goalType": "PRACTICE",
  "message": "Хочу пройти практику...",
  "clarificationNote": null,
  "reason": null,
  "createdAt": "2025-03-19T14:30:00+03:00",
  "respondedAt": null,
  "completedAt": null
}
```

---

### 4.9 Chat

Чат создаётся автоматически при принятии заявки. Доступ — только участники.

#### `GET /chats` — Мои чаты

**Auth:** Требуется JWT

**Query params:** `page` (default `0`), `size` (default `20`, max `100`)

**Response:** `200 OK` — `PagedResponse<ChatResponse>`, сортировка по `lastMessageAt DESC NULLS LAST, createdAt DESC`

#### Структура `ChatResponse`

```json
{
  "id": 1,
  "mentoringRequestId": 5,
  "studentUserId": 1,
  "mentorUserId": 2,
  "createdAt": "2025-03-19T14:30:00+03:00",
  "lastMessage": "Привет! Как дела?",
  "lastMessageAt": "2025-03-20T10:00:00+03:00",
  "lastSenderUserId": 2,
  "unreadCount": 3,
  "studentName": "Иван Петров",
  "mentorName": "Алексей Смирнов",
  "mentoringRequestStatus": "ACCEPTED"
}
```

| Поле | Тип | Описание |
|---|---|---|
| `id` | number | ID чата |
| `mentoringRequestId` | number | ID заявки |
| `studentUserId` | number | ID пользователя-студента |
| `mentorUserId` | number | ID пользователя-ментора |
| `createdAt` | string | Дата создания |
| `lastMessage` | string \| null | Текст последнего сообщения |
| `lastMessageAt` | string \| null | Время последнего сообщения |
| `lastSenderUserId` | number \| null | ID отправителя последнего сообщения |
| `unreadCount` | number | Количество непрочитанных сообщений |
| `studentName` | string | Имя студента (firstName + lastName) |
| `mentorName` | string | Имя ментора (firstName + lastName) |
| `mentoringRequestStatus` | string | Статус заявки |

---

#### `GET /chats/{chatId}` — Чат по ID

**Response:** `200 OK` — `ChatResponse`. `403` если не участник.

---

#### `GET /chats/by-request/{requestId}` — Чат по ID заявки

**Response:** `200 OK` — `ChatResponse`. `404` если заявка не принята.

---

#### `GET /chats/{chatId}/messages` — Сообщения (постраничная пагинация)

**Auth:** Требуется JWT

**Query params:** `page` (default `0`), `size` (default `20`, max `100`)

**Response:** `200 OK` — `PagedResponse<ChatMessageResponse>` (сортировка по `createdAt DESC`)

---

#### `GET /chats/{chatId}/messages/cursor` — Сообщения (курсорная пагинация)

**Auth:** Требуется JWT

Для реализации бесконечной прокрутки вверх — загружаем сообщения старше указанного ID.

**Query params:**

| Параметр | Тип | Описание |
|---|---|---|
| `beforeMessageId` | number | Возвращать сообщения со `id < beforeMessageId` |
| `limit` | number | Количество (default `20`) |

**Response:** `200 OK` — `ChatMessageResponse[]` (сортировка по `createdAt DESC`)

```typescript
// Пример: первая загрузка
const initial = await apiCall<ChatMessageResponse[]>(
  `${BASE_URL}/chats/${chatId}/messages/cursor?limit=20`
);

// Подгрузка более старых
const oldest = initial[initial.length - 1];
const older = await apiCall<ChatMessageResponse[]>(
  `${BASE_URL}/chats/${chatId}/messages/cursor?beforeMessageId=${oldest.id}&limit=20`
);
```

---

#### `POST /chats/{chatId}/messages` — Отправить сообщение

**Auth:** Требуется JWT

**Request:**
```json
{
  "body": "Привет! Как дела с домашним заданием?",
  "attachmentFileId": null
}
```

| Поле | Тип | Описание |
|---|---|---|
| `body` | string \| null | Текст (хотя бы одно из `body` или `attachmentFileId`) |
| `attachmentFileId` | number \| null | ID файла из `POST /files/chat-attachment` |

**Response:** `201 Created` — `ChatMessageResponse`

**Побочный эффект:** SSE-событие `chat.message.created` отправляется второму участнику через `/chats/events`.

---

#### `POST /chats/{chatId}/read` — Отметить как прочитанный

**Auth:** Требуется JWT

**Response:** `204 No Content`

Сбрасывает `unreadCount` для текущего пользователя в этом чате. Отправляет SSE-событие `chat.read` второму участнику.

---

#### `POST /chats/{chatId}/typing` — Событие печати

**Auth:** Требуется JWT

**Request:**
```json
{ "typing": true }
```

| Поле | Тип | Обязательное | Описание |
|---|---|---|---|
| `typing` | boolean | да (`@NotNull`) | `true` — начал печатать, `false` — остановился |

**Response:** `204 No Content`

Отправляет SSE-событие `chat.typing` второму участнику чата.

```typescript
// Реализация индикатора печати
let typingTimer: ReturnType<typeof setTimeout> | null = null;

function onInputChange(chatId: number) {
  sendTyping(chatId, true);
  if (typingTimer) clearTimeout(typingTimer);
  typingTimer = setTimeout(() => sendTyping(chatId, false), 3000);
}

async function sendTyping(chatId: number, typing: boolean): Promise<void> {
  await apiCall(`${BASE_URL}/chats/${chatId}/typing`, {
    method: 'POST',
    body: JSON.stringify({ typing }),
  });
}
```

---

#### `GET /chats/events` — SSE-поток событий реального времени

**Auth:** Требуется JWT (передаётся как query param или заголовок)

**Важно:** Браузерный `EventSource` не поддерживает заголовки. Используйте `fetch` с `ReadableStream` или библиотеку `@microsoft/fetch-event-source`.

```typescript
import { fetchEventSource } from '@microsoft/fetch-event-source';

fetchEventSource(`${BASE_URL}/chats/events`, {
  headers: { 'Authorization': `Bearer ${token}` },
  onmessage(event) {
    const data = JSON.parse(event.data);
    switch (event.event) {
      case 'chat.message.created':
        // data: ChatMessageResponse
        handleNewMessage(data);
        break;
      case 'chat.read':
        // data: { chatId, userId }
        handleChatRead(data);
        break;
      case 'chat.typing':
        // data: { chatId, userId, typing: boolean }
        handleTyping(data);
        break;
      case 'presence.changed':
        // data: { userId, status: 'online'|'offline', lastSeenAt }
        handlePresenceChange(data);
        break;
    }
  },
  onerror(err) {
    console.error('SSE error', err);
  },
});
```

**SSE-события:**

| Тип события | Данные | Когда |
|---|---|---|
| `chat.message.created` | `ChatMessageResponse` | Новое сообщение в чате |
| `chat.read` | `{ chatId, userId }` | Другой участник прочитал чат |
| `chat.typing` | `{ chatId, userId, typing }` | Участник печатает / перестал печатать |
| `presence.changed` | `{ userId, status, lastSeenAt }` | Изменился online-статус пользователя |

---

#### Структура `ChatMessageResponse`

```json
{
  "id": 10,
  "chatId": 1,
  "senderUserId": 1,
  "body": "Привет! Как дела с домашним заданием?",
  "attachment": null,
  "createdAt": "2025-03-19T15:00:00+03:00"
}
```

`attachment`:
```json
{
  "fileId": 100,
  "originalFilename": "task.pdf",
  "contentType": "application/pdf",
  "size": 245760
}
```

---

### 4.10 Admin

Все эндпоинты доступны только пользователям с ролью `ADMIN`. Тут собраны базовые админ-операции по пользователям. Дополнительно см. разделы 4.18 (модерация + аудит + outbox), 4.19 (управление статусом, Stage 11) и 4.20 (CRUD справочников, Stage 11).

#### `PUT /admin/users/{userId}/role` — Назначить роль пользователю

**Auth:** Требуется JWT (ADMIN)

**Логика:** Роли `STUDENT` и `MENTOR` взаимоисключающие. Роль `ADMIN` назначить нельзя (422). При смене роли создаётся профиль нового типа (если нет). Кеш `userDetails` инвалидируется. Публикуется `RoleChangedAuditEvent` → запись в `admin_audit_log`.

> **Внимание:** старый JWT остаётся технически валидным, но содержит старые роли. Пользователь должен **перелогиниться** для получения нового токена. Это отличается от смены статуса (см. раздел 4.19), где `tokenVersion` инвалидирует токен мгновенно.

**Request:**
```json
{ "role": "MENTOR" }
```

**Response:** `200 OK` (пустое тело)

---

#### `GET /admin/users` — Список пользователей

**Auth:** Требуется JWT (ADMIN)

**Query params:**

| Параметр | Тип | Описание |
|---|---|---|
| `q` | string | Поиск по email, имени, фамилии |
| `role` | string | Фильтр по роли: `STUDENT`, `MENTOR`, `ADMIN` |
| `status` | string | Фильтр по статусу: `ACTIVE`, `BLOCKED`, `DELETED` |
| `page` | number | default `0` |
| `size` | number | default `20` |
| `sort` | string | whitelist: `createdAt`, `email`, `status` (default: `createdAt,desc`) |

**Response:** `200 OK` — `PagedResponse<AdminUserResponse>`

```json
{
  "content": [
    {
      "id": 1,
      "email": "student@example.com",
      "status": "ACTIVE",
      "roles": ["STUDENT"],
      "firstName": "Иван",
      "lastName": "Петров",
      "createdAt": "2025-03-19T14:30:00+03:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

---

#### `GET /admin/users/stats` — Статистика пользователей

**Auth:** Требуется JWT (ADMIN)

**Response:** `200 OK`
```json
{
  "totalUsers": 150,
  "byRole": {
    "STUDENT": 120,
    "MENTOR": 28,
    "ADMIN": 2
  },
  "byStatus": {
    "ACTIVE": 145,
    "BLOCKED": 3,
    "DELETED": 2
  }
}
```

---

### 4.11 Reviews

#### `POST /reviews` — Создать отзыв

**Auth:** Требуется JWT (только студент)

**Правила:**
- Отзыв может оставить только студент из заявки.
- Заявка должна быть в статусе `COMPLETED`.
- Один отзыв на одну заявку (уникальность по `mentoring_request_id`).

**Request:**
```json
{
  "mentoringRequestId": 5,
  "rating": 5,
  "comment": "Отличный ментор, очень помог разобраться в Spring Boot!"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `mentoringRequestId` | number | да | `@NotNull` |
| `rating` | number | да | `@NotNull`, min 1, max 5 |
| `comment` | string | нет | max 2000 символов |

**Response:** `201 Created`
```json
{
  "id": 1,
  "mentoringRequestId": 5,
  "reviewerUserId": 1,
  "mentorUserId": 2,
  "rating": 5,
  "comment": "Отличный ментор!",
  "moderationStatus": "VISIBLE",
  "createdAt": "2025-03-19T14:30:00+03:00"
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 403 | Текущий пользователь не студент из этой заявки |
| 404 | Заявка не найдена |
| 409 | Отзыв на эту заявку уже существует |
| 422 | Заявка не завершена (статус не COMPLETED) |

---

#### `GET /reviews/by-request/{requestId}` — Отзыв по ID заявки

**Auth:** Требуется JWT (видят только студент-автор или ментор из заявки)

**Response:** `200 OK` — `ReviewResponse` (включает `moderationStatus`)

**Ошибки:** `403` если нет доступа, `404` если отзыва нет.

---

#### `GET /profiles/mentors/{id}/reviews` — Отзывы ментора

**Auth:** Не требуется (публичный)

**Query params:** `page` (default `0`), `size` (default `20`)

**Response:** `200 OK` — `PagedResponse<ReviewResponse>` (только записи с `moderationStatus = VISIBLE`, сортировка по `createdAt DESC`). Скрытые администратором отзывы не отдаются и не учитываются в среднем рейтинге.

```typescript
// Пример: загрузить отзывы ментора
const reviews = await apiCall<PagedResponse<ReviewResponse>>(
  `${BASE_URL}/profiles/mentors/10/reviews?page=0&size=10`
);
```

---

#### `DELETE /reviews/{id}` — Удалить отзыв

**Auth:** Требуется JWT (только автор отзыва)

**Response:** `204 No Content`

**Ошибки:** `403` если не автор, `404` если не найден.

---

### 4.12 Dashboard

#### `GET /dashboard/summary` — Сводка дашборда

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "role": "STUDENT",
  "sentRequests": 2,
  "pendingRequests": 1,
  "acceptedRequests": 1,
  "totalChats": 1,
  "unreadChats": 0,
  "profileCompletion": 75,
  "nextSession": null
}
```

| Поле | Тип | Описание |
|---|---|---|
| `role` | string | Роль текущего пользователя |
| `sentRequests` | number | Отправленные заявки (статус SENT) |
| `pendingRequests` | number | Заявки на рассмотрении (REVIEWING / NEEDS_CLARIFICATION) |
| `acceptedRequests` | number | Принятые заявки (ACCEPTED) |
| `totalChats` | number | Всего чатов |
| `unreadChats` | number | Чаты с непрочитанными сообщениями |
| `profileCompletion` | number | Процент заполненности профиля (0–100) |
| `nextSession` | null | Зарезервировано (всегда null) |

---

#### `GET /dashboard/activity` — Лента активности

**Auth:** Требуется JWT

**Query params:** `limit` (default `20`, max `50`)

**Response:** `200 OK` — `ActivityItemResponse[]` (сортировка по `occurredAt DESC`)

```json
[
  {
    "type": "REQUEST_ACCEPTED",
    "refId": 5,
    "description": "Заявка на менторство принята",
    "occurredAt": "2025-03-20T10:00:00+03:00"
  },
  {
    "type": "MESSAGE_RECEIVED",
    "refId": 10,
    "description": "Новое сообщение в чате",
    "occurredAt": "2025-03-20T09:30:00+03:00"
  }
]
```

| Поле | Тип | Описание |
|---|---|---|
| `type` | string | Тип события активности |
| `refId` | number | ID связанного объекта (заявка, чат и т.д.) |
| `description` | string | Текстовое описание события |
| `occurredAt` | string | Время события (ISO 8601 OffsetDateTime) |

---

### 4.13 Mentor Stats

#### `GET /mentor-stats/me` — Статистика ментора

**Auth:** Требуется JWT (только MENTOR)

**Response:** `200 OK`
```json
{
  "averageRating": 4.7,
  "reviewCount": 12,
  "completedRequests": 8,
  "responseRate": 0.92,
  "level": "INTERMEDIATE",
  "progress": 0.6
}
```

| Поле | Тип | Описание |
|---|---|---|
| `averageRating` | number \| null | Средний рейтинг (1.0–5.0), null если отзывов нет |
| `reviewCount` | number | Количество отзывов |
| `completedRequests` | number | Завершённых менторских программ |
| `responseRate` | number | Доля принятых заявок (0.0–1.0) |
| `level` | string | `BEGINNER` / `INTERMEDIATE` / `EXPERT` |
| `progress` | number | Прогресс до следующего уровня (0.0–1.0) |

**Уровни:**
| Уровень | Условие |
|---|---|
| `BEGINNER` | completedRequests 0–4 |
| `INTERMEDIATE` | completedRequests 5–19 |
| `EXPERT` | completedRequests 20+ |

---

### 4.14 Presence

#### `GET /presence/{userId}` — Статус присутствия пользователя

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "userId": 2,
  "status": "online",
  "lastSeenAt": "2025-03-20T10:00:00+03:00"
}
```

| Поле | Тип | Описание |
|---|---|---|
| `userId` | number | ID пользователя |
| `status` | string | `"online"` или `"offline"` |
| `lastSeenAt` | string \| null | Время последней активности (OffsetDateTime) |

**Использование:** Запрашивайте перед открытием чата, чтобы показать статус собеседника. Обновления приходят через SSE-событие `presence.changed`.

---

### 4.15 Mentoring Sessions

Календарные сессии можно создавать только для заявок в статусе `ACCEPTED`. Доступ имеют только участники заявки (студент и ментор).

#### `POST /sessions` — Создать сессию

**Auth:** Требуется JWT (участник заявки)

**Request:**
```json
{
  "mentoringRequestId": 5,
  "scheduledAt": "2026-06-01T14:00:00+03:00",
  "durationMinutes": 60
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `mentoringRequestId` | number | да | `@NotNull` |
| `scheduledAt` | string (OffsetDateTime) | да | `@Future` |
| `durationMinutes` | number | да | 15 ≤ value ≤ 480 |

**Response:** `201 Created` — `SessionResponse`
```json
{
  "id": 100,
  "mentoringRequestId": 5,
  "studentUserId": 1,
  "mentorUserId": 2,
  "studentName": "Анна Смирнова",
  "mentorName": "Иван Петров",
  "scheduledAt": "2026-06-01T14:00:00+03:00",
  "durationMinutes": 60,
  "status": "SCHEDULED",
  "cancelReason": null,
  "rescheduleReason": null,
  "createdAt": "2026-05-18T20:00:00+03:00",
  "updatedAt": "2026-05-18T20:00:00+03:00"
}
```

**Ошибки:** `403` (не участник заявки), `404` (заявка не найдена), `422` (заявка не `ACCEPTED`).

---

#### `GET /sessions` — Мои сессии

**Auth:** Требуется JWT

**Query params:** `page` (default `0`), `size` (default `20`, max `100`), `sort` (whitelist: `scheduledAt`, `createdAt`, `status`; default `scheduledAt,asc`)

**Response:** `200 OK` — `PagedResponse<SessionResponse>`

---

#### `GET /sessions/{id}` — Сессия по ID

**Auth:** Требуется JWT (только участник)

**Response:** `200 OK` — `SessionResponse`. `403` если не участник, `404` если не найдена.

---

#### `PUT /sessions/{id}/reschedule` — Перенести сессию

**Auth:** Требуется JWT (участник)

**Request:**
```json
{
  "newScheduledAt": "2026-06-02T15:00:00+03:00",
  "durationMinutes": 90,
  "reason": "Не успеваю по предыдущему слоту"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `newScheduledAt` | string | да | `@Future` |
| `durationMinutes` | number | да | 15–480 |
| `reason` | string | нет | max 500 |

**Response:** `200 OK` — `SessionResponse` со статусом `RESCHEDULED`, заполненный `rescheduleReason`.

---

#### `PUT /sessions/{id}/cancel` — Отменить сессию

**Auth:** Требуется JWT (участник)

**Request:** опционально
```json
{ "reason": "Не получится — заболел" }
```

**Response:** `200 OK` — `SessionResponse` со статусом `CANCELLED` (и опциональным `cancelReason`).

---

#### `PUT /sessions/{id}/complete` — Зафиксировать проведённую сессию

**Auth:** Требуется JWT (участник)

**Response:** `200 OK` — `SessionResponse` со статусом `COMPLETED`.

> После `COMPLETED` сессии и `COMPLETED` заявки студент может оставить отзыв (`POST /reviews`).

#### Жизненный цикл

```
SCHEDULED ─reschedule─→ RESCHEDULED ─complete─→ COMPLETED
    │                       │
    └─────── cancel ────────┴────→ CANCELLED
```

`NO_SHOW` — выставляется бэкендом, если время прошло, а сессию никто не пометил `COMPLETED`/`CANCELLED` (фоновый процесс).

---

### 4.16 Notification Preferences

Каждый пользователь может настроить, какие email-уведомления получать. По умолчанию все флаги `true`. Доставка идёт через outbox-таблицу с retry — local-инстанс перехватывает письма в MailHog (`http://localhost:8025`).

#### `GET /profile/me/notifications` — Текущие настройки

**Auth:** Требуется JWT

**Response:** `200 OK`
```json
{
  "emailRequestEvents": true,
  "emailSessionEvents": true,
  "emailReviewEvents": false
}
```

| Поле | Описание |
|---|---|
| `emailRequestEvents` | События по заявкам менторинга (создана, принята, отклонена) |
| `emailSessionEvents` | События по календарным сессиям (создана, перенесена, отменена) |
| `emailReviewEvents` | События по отзывам (новый отзыв на ментора) |

---

#### `PUT /profile/me/notifications` — Обновить настройки

**Auth:** Требуется JWT

**Request:** любое подмножество флагов (`null` поля игнорируются — частичный апдейт)
```json
{ "emailRequestEvents": false }
```

| Поле | Тип | Обязательное | Описание |
|---|---|---|---|
| `emailRequestEvents` | boolean \| null | нет | `null` = не менять |
| `emailSessionEvents` | boolean \| null | нет | `null` = не менять |
| `emailReviewEvents` | boolean \| null | нет | `null` = не менять |

**Response:** `200 OK` — `NotificationPreferencesResponse` (актуальный набор).

---

### 4.17 Complaints

Любой авторизованный пользователь может пожаловаться на отзыв или другого пользователя. Жалобы обрабатываются администратором (см. 4.18).

#### `POST /complaints` — Подать жалобу

**Auth:** Требуется JWT

**Request:**
```json
{
  "targetType": "REVIEW",
  "targetId": 42,
  "reason": "В отзыве оскорбления и нецензурная лексика"
}
```

| Поле | Тип | Обязательное | Значения |
|---|---|---|---|
| `targetType` | string | да | `REVIEW` (жалоба на отзыв), `USER` (жалоба на пользователя) |
| `targetId` | number | да | ID отзыва или пользователя |
| `reason` | string | да | `@NotBlank`, max 2000 символов |

**Response:** `201 Created` — `ComplaintResponse`
```json
{
  "id": 7,
  "targetType": "REVIEW",
  "targetId": 42,
  "reporterUserId": 1,
  "reason": "В отзыве оскорбления...",
  "status": "OPEN",
  "resolution": null,
  "resolvedBy": null,
  "resolvedAt": null,
  "createdAt": "2026-05-18T20:00:00+03:00"
}
```

**Ошибки:** `404` если `targetId` не существует.

---

### 4.18 Admin: Moderation & Audit

> Все эндпоинты требуют роль `ADMIN`. Действия (смена роли, модерация отзыва, разрешение жалобы) автоматически фиксируются в аудит-логе через события `RoleChangedAuditEvent` / `ReviewModeratedAuditEvent` / `ComplaintResolvedAuditEvent` после `AFTER_COMMIT`.

#### `GET /admin/complaints` — Список жалоб

**Query params:** `status` (`OPEN` / `RESOLVED` / `REJECTED`), `targetType` (`REVIEW` / `USER`), `page`, `size`, `sort` (whitelist: `createdAt`, `status`, `resolvedAt`; default `createdAt,desc`).

**Response:** `200 OK` — `PagedResponse<ComplaintResponse>`.

---

#### `PUT /admin/complaints/{id}/resolve` — Закрыть жалобу

**Request:**
```json
{
  "status": "RESOLVED",
  "resolution": "Отзыв скрыт, нарушитель предупреждён"
}
```

| Поле | Тип | Обязательное | Значения |
|---|---|---|---|
| `status` | string | да | `RESOLVED` или `REJECTED` |
| `resolution` | string | нет | max 2000 символов |

**Response:** `200 OK` — `ComplaintResponse` со `status`, `resolvedBy`, `resolvedAt`, `resolution`.

**Ошибки:** `404` (нет жалобы), `422` (жалоба уже закрыта).

---

#### `PUT /admin/reviews/{id}/moderate` — Модерация отзыва

**Request:**
```json
{ "moderationStatus": "HIDDEN" }
```

| Поле | Тип | Значения |
|---|---|---|
| `moderationStatus` | string | `VISIBLE`, `HIDDEN`, `UNDER_REVIEW` |

**Response:** `200 OK` — `ReviewResponse` с обновлёнными `moderationStatus`, `moderatedBy`, `moderatedAt`. Скрытые отзывы исчезают из публичных списков и не учитываются в среднем рейтинге ментора.

---

#### `GET /admin/audit` — Аудит-лог

**Query params:**
| Параметр | Тип | Описание |
|---|---|---|
| `action` | string | Фильтр по типу действия (`ROLE_CHANGED`, `REVIEW_MODERATED`, `COMPLAINT_RESOLVED`, `USER_STATUS_CHANGED`, `DICTIONARY_CHANGED`) |
| `adminUserId` | number | Фильтр по администратору |
| `from` | string (ISO datetime) | С какого момента |
| `to` | string (ISO datetime) | По какой момент |
| `page` | number | default `0` |
| `size` | number | default `20`, max `100` |
| `sort` | string | whitelist: `createdAt`, `action`; default `createdAt,desc` |

**Response:** `200 OK` — `PagedResponse<AuditLogResponse>`
```json
{
  "content": [
    {
      "id": 15,
      "adminUserId": 99,
      "action": "REVIEW_MODERATED",
      "targetType": "REVIEW",
      "targetId": 42,
      "payload": "{\"adminUserId\":99,\"reviewId\":42,\"newStatus\":\"HIDDEN\"}",
      "createdAt": "2026-05-18T20:10:00+03:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

`payload` — это JSON-строка (Jackson сериализовал событие в `jsonb`-колонку), фронт распарсит через `JSON.parse(payload)`.

---

#### `GET /admin/notifications/outbox` — Outbox писем

Просмотр состояния очереди исходящих email-уведомлений.

**Query params:** `status` (`PENDING` / `SENT` / `FAILED`), `page` (default `0`), `size` (default `50`).

**Response:** `200 OK` — `PagedResponse<OutboxEntryResponse>`
```json
{
  "content": [
    {
      "id": 1001,
      "recipientEmail": "student@example.com",
      "subject": "Заявка принята",
      "eventType": "request.accepted",
      "status": "SENT",
      "attempts": 1,
      "lastError": null,
      "nextAttemptAt": null,
      "sentAt": "2026-05-18T20:05:00+03:00",
      "createdAt": "2026-05-18T20:04:55+03:00"
    }
  ]
}
```

`FAILED` появляется после исчерпания `app.notifications.outbox.max-attempts` (default 5). `nextAttemptAt` — когда шедулер попробует переотправить `PENDING`-запись.

---

### 4.19 Admin: User Status (Stage 11)

#### `PUT /admin/users/{userId}/status` — Сменить статус пользователя

**Auth:** Требуется JWT (ADMIN)

**Логика:**
- Допустимые целевые статусы: `ACTIVE`, `BLOCKED`, `DELETED` (выход на `EMAIL_NOT_CONFIRMED` админ не делает).
- При переходе в `BLOCKED` или `DELETED` `tokenVersion` инкрементится — все ранее выписанные JWT этого пользователя моментально становятся невалидными.
- Кеш `userDetails` инвалидируется (`@CacheEvict`), чтобы новые запросы тут же увидели новый статус.
- Публикуется `UserStatusChangedAuditEvent` → запись в `admin_audit_log` с `action = USER_STATUS_CHANGED`.
- Возврат в `ACTIVE` (unblock) `tokenVersion` не уменьшает — старые токены остаются мертвы по дизайну.

**Request:**
```json
{ "status": "BLOCKED" }
```

**Response:** `200 OK` (пустое тело)

**Ошибки:**
- `404` — пользователь не найден
- `400` — недопустимый статус (например, `EMAIL_NOT_CONFIRMED`)
- `422` — попытка заблокировать самого себя

**TypeScript:**
```typescript
type UserStatus = 'ACTIVE' | 'BLOCKED' | 'DELETED' | 'EMAIL_NOT_CONFIRMED';

async function changeUserStatus(userId: number, status: 'ACTIVE' | 'BLOCKED' | 'DELETED') {
  return fetch(`${BASE_URL}/admin/users/${userId}/status`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
}
```

---

### 4.20 Admin: Dictionary CRUD (Stage 11)

> Полный CRUD для четырёх справочников. Все эндпоинты требуют роль `ADMIN`. Возвращают записи **включая `active=false`** (в отличие от публичных `/dictionaries/**`, которые отдают только `active=true`). Каждая мутация инвалидирует кеш `dictionaries` и публикует `DictionaryChangedAuditEvent` (`AuditAction.DICTIONARY_CHANGED`).

`{type}` ∈ {`cities`, `skills`, `languages`, `interaction-types`}

#### Общая схема эндпоинтов

| Метод | Путь | Назначение | Ответ |
|---|---|---|---|
| `GET` | `/admin/dictionaries/{type}` | Список всех записей справочника | `200 OK` — массив `{Type}Response` |
| `POST` | `/admin/dictionaries/{type}` | Создать запись | `201 Created` — `{Type}Response` |
| `PUT` | `/admin/dictionaries/{type}/{id}` | Обновить запись (включая `active`) | `200 OK` — `{Type}Response` |
| `DELETE` | `/admin/dictionaries/{type}/{id}` | Soft delete (`active=false`) | `204 No Content` |
| `PUT` | `/admin/dictionaries/{type}/{id}/restore` | Восстановить (`active=true`) | `200 OK` — `{Type}Response` |

**Правила уникальности:**
- Имя уникально среди `active=true` записей (partial unique index `WHERE active = TRUE`).
- После soft delete можно создать запись с тем же именем — старая просто остаётся в БД с `active=false`.
- Для языков уникальность дополнительно по `code` среди активных.

**Ошибки:**
- `404` — запись с `id` не найдена.
- `409` — `name` (или `code` у языков) занят активной записью.
- `422` — нарушение бизнес-правила (например, попытка `DELETE` уже неактивной записи).

#### Cities — `dict_city`

**Поля:** `id`, `name` (≤150), `region` (≤150, optional), `country` (≤100), `active`.

**Create / Update:**
```json
{ "name": "Минск", "region": "Минская область", "country": "Беларусь" }
```
*(в `UpdateCityRequest` дополнительно `"active": true|false`)*

**Response (`CityResponse`):**
```json
{
  "id": 17,
  "name": "Минск",
  "region": "Минская область",
  "country": "Беларусь",
  "active": true
}
```

#### Skills — `dict_skill`

**Поля:** `id`, `name` (≤150), `category` (≤100, optional), `active`.

**Create / Update:**
```json
{ "name": "Spring Boot", "category": "Backend" }
```

**Response (`SkillResponse`):**
```json
{ "id": 42, "name": "Spring Boot", "category": "Backend", "active": true }
```

#### Languages — `dict_language`

**Поля:** `id`, `name` (≤100), `code` (≤10, обязательно — ISO-639 рекомендован), `active`.

**Create / Update:**
```json
{ "name": "Английский", "code": "en" }
```

**Response (`LanguageResponse`):**
```json
{ "id": 3, "name": "Английский", "code": "en", "active": true }
```

> Дополнительно `code` уникален среди активных записей.

#### Interaction Types — `dict_interaction_type`

**Поля:** `id`, `name` (≤150), `description` (text, optional), `active`.

**Create / Update:**
```json
{ "name": "Видео-звонок", "description": "Сессии через Google Meet / Zoom" }
```

**Response (`InteractionTypeResponse`):**
```json
{
  "id": 5,
  "name": "Видео-звонок",
  "description": "Сессии через Google Meet / Zoom",
  "active": true
}
```

#### Пример: типичная админ-таблица справочника (React)

```typescript
type SkillResponse = {
  id: number; name: string; category: string | null; active: boolean;
};

async function loadSkills(): Promise<SkillResponse[]> {
  const res = await fetch(`${BASE_URL}/admin/dictionaries/skills`, {
    headers: { 'Authorization': `Bearer ${adminToken}` },
  });
  return res.json();
}

async function softDeleteSkill(id: number): Promise<void> {
  await fetch(`${BASE_URL}/admin/dictionaries/skills/${id}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${adminToken}` },
  });
}

async function restoreSkill(id: number): Promise<SkillResponse> {
  const res = await fetch(`${BASE_URL}/admin/dictionaries/skills/${id}/restore`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${adminToken}` },
  });
  return res.json();
}
```

> **Важно:** публичный `GET /dictionaries/{type}` (раздел 4.2) кеширован на 300 секунд (Caffeine, кеш `dictionaries`). После создания/обновления/удаления через админ-API кеш сбрасывается автоматически (`@CacheEvict`), но если фронт хранит словари локально, обновите свой кеш после изменений.

---

## 5. TypeScript-типы (полная карта DTO)

```typescript
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Auth
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

interface RegisterResponse {
  id: number;
  email: string;
  roles: string[];
}

interface LoginRequest {
  email: string;
  password: string;
}

interface LoginResponse {
  accessToken: string;
  tokenType: string;              // всегда "Bearer"
  user: UserInfoResponse;
}

interface UserInfoResponse {
  id: number;
  email: string;
  roles: string[];
  status: UserStatus;
}

interface ForgotPasswordRequest {
  email: string;
}

interface ResetPasswordRequest {
  email: string;
  code: string;                   // ровно 6 цифр
  newPassword: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Error
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Profile Summary
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface ProfileSummaryResponse {
  role: 'STUDENT' | 'MENTOR' | 'ADMIN';
  profileId: number | null;
  profileExists: boolean;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Dictionaries
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface CityResponse {
  id: number;
  name: string;
  region: string | null;
  country: string;
  active: boolean;          // публичный API возвращает только active=true; админский (/admin/dictionaries/cities) — все
}

interface SkillResponse {
  id: number;
  name: string;
  category: string | null;
  active: boolean;
}

interface LanguageResponse {
  id: number;
  name: string;
  code: string;             // ISO 639-1: "en", "ru", "de"
  active: boolean;
}

interface InteractionTypeResponse {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
}

// Admin Dictionary CRUD (Stage 11)

interface CreateCityRequest        { name: string; region?: string | null; country: string; }
interface UpdateCityRequest        { name: string; region?: string | null; country: string; active: boolean; }
interface CreateSkillRequest       { name: string; category?: string | null; }
interface UpdateSkillRequest       { name: string; category?: string | null; active: boolean; }
interface CreateLanguageRequest    { name: string; code: string; }
interface UpdateLanguageRequest    { name: string; code: string; active: boolean; }
interface CreateInteractionTypeRequest { name: string; description?: string | null; }
interface UpdateInteractionTypeRequest { name: string; description?: string | null; active: boolean; }

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Student Profile
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface StudentProfileRequest {
  firstName: string;
  lastName: string;
  middleName?: string | null;
  phone?: string | null;
  cityId?: number | null;
  desiredPosition?: string | null;
  hoursPerWeek?: number | null;
  availableFrom?: string | null;      // "YYYY-MM-DD"
  about?: string | null;
  max?: string | null;
  employmentTypes?: EmploymentType[] | null;
  workFormats?: WorkFormat[] | null;
  educations?: StudentEducationRequest[] | null;
  languages?: StudentLanguageRequest[] | null;
  skills?: StudentSkillRequest[] | null;
}

interface PatchStudentProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  middleName?: string | null;
  phone?: string | null;
  desiredPosition?: string | null;
  hoursPerWeek?: number | null;
  availableFrom?: string | null;
  about?: string | null;
  maxContact?: string | null;
  cityId?: number | null;
  employmentTypes?: EmploymentType[] | null;
  workFormats?: WorkFormat[] | null;
}

interface StudentProfileResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  phone: string | null;
  city: CityResponse | null;
  desiredPosition: string | null;
  hoursPerWeek: number | null;
  availableFrom: string | null;
  about: string | null;
  max: string | null;
  employmentTypes: string[];
  workFormats: string[];
  educations: StudentEducationResponse[];
  languages: StudentLanguageResponse[];
  skills: StudentSkillResponse[];
  resumeFileId: number | null;
}

interface StudentEducationRequest {
  institution: string;
  specialty?: string | null;
  degree?: EducationDegree | null;
  educationForm?: EducationForm | null;
  startYear?: number | null;
  graduationYear?: number | null;
}

interface StudentEducationResponse {
  id: number;
  institution: string;
  specialty: string | null;
  degree: string | null;
  educationForm: string | null;
  startYear: number | null;
  graduationYear: number | null;
}

interface StudentLanguageRequest {
  languageId: number;
  level: LanguageLevel;
}

interface StudentLanguageResponse {
  id: number;
  language: LanguageResponse;
  level: string;
  position: number;
}

interface StudentSkillRequest {
  skillId: number;
  level: SkillLevel;
}

interface StudentSkillResponse {
  id: number;
  skill: SkillResponse;
  level: string;
  position: number;
}

interface PutStudentSkillsRequest {
  skills: Array<{
    skillId: number;
    level: SkillLevel;
    position: number;
  }>;
}

interface PutStudentLanguagesRequest {
  languages: Array<{
    languageId: number;
    level: LanguageLevel;
    position: number;
  }>;
}

interface StudentCompletionResponse {
  percent: number;
  mainDone: boolean;
  aboutDone: boolean;
  skillsDone: boolean;
  resumeDone: boolean;
}

interface StudentFilesResponse {
  resume: FileResponse | null;
  portfolioCount: number;
  avatarFileId: number | null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentor Profile
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface MentorProfileRequest {
  firstName: string;
  lastName: string;
  middleName?: string | null;
  position?: string | null;
  department?: string | null;
  cityId?: number | null;
  phone?: string | null;
  max?: string | null;
  description?: string | null;
  expectations?: string | null;
  canHelpWith?: string | null;
  mentoringType?: MentoringType | null;
  mentoringChannel?: MentoringChannel | null;
  mentoringFrequency?: string | null;
  mentoringDuration?: MentoringDuration | null;
  menteeLimit?: number | null;
  recruitmentStatus?: RecruitmentStatus | null;  // null → "OPEN"
  skills?: MentorSkillRequest[] | null;
}

interface MentorProfileResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  position: string | null;
  department: string | null;
  city: CityResponse | null;
  phone: string | null;
  max: string | null;
  description: string | null;
  expectations: string | null;
  canHelpWith: string | null;
  mentoringType: string | null;
  mentoringChannel: string | null;
  mentoringFrequency: string | null;
  mentoringDuration: string | null;
  menteeLimit: number | null;
  recruitmentStatus: string;         // NOT NULL, default "OPEN"
  skills: MentorSkillResponse[];
}

interface MentorSkillRequest {
  skillId: number;
  level: SkillLevel;
}

interface MentorSkillResponse {
  id: number;
  skill: SkillResponse;
  level: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Pagination
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentor Search
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface MentorSearchParams {
  q?: string;
  skillIds?: number[];
  cityId?: number;
  recruitmentStatus?: RecruitmentStatus;
  mentoringType?: MentoringType;
  mentoringChannel?: MentoringChannel;
  page?: number;
  size?: number;
  sort?: string;
}

// Облегчённая карточка для списка (без description/expectations/canHelpWith/phone/max)
interface MentorCardResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  position: string | null;
  department: string | null;
  city: CityResponse | null;
  mentoringType: string | null;
  mentoringChannel: string | null;
  mentoringDuration: string | null;
  menteeLimit: number | null;
  recruitmentStatus: string;
  skills: MentorSkillResponse[];
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentoring Requests
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface MentoringRequestCreateRequest {
  targetProfileId: number;
  goalType: MentoringType;
  message: string;              // max 2000
}

interface MentoringRequestClarifyRequest {
  clarificationNote: string;    // max 2000
}

interface MentoringRequestRejectRequest {
  reason?: string | null;       // max 2000
}

interface StudentProfileShortResponse {
  id: number;
  firstName: string;
  lastName: string;
}

interface MentorProfileShortResponse {
  id: number;
  firstName: string;
  lastName: string;
  position: string | null;
}

interface MentoringRequestResponse {
  id: number;
  studentProfileId: number;
  mentorProfileId: number;
  studentProfile: StudentProfileShortResponse;
  mentorProfile: MentorProfileShortResponse;
  direction: MentoringRequestDirection;
  status: MentoringRequestStatus;
  goalType: string;
  message: string;
  clarificationNote: string | null;
  reason: string | null;
  createdAt: string;
  respondedAt: string | null;
  completedAt: string | null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Files
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface FileResponse {
  id: number;
  originalFilename: string;
  contentType: string;          // MIME type
  size: number;                 // bytes
  fileType: FileType;
  status: FileStatus;           // "ACTIVE" | "DELETED"
  previewUrl: string | null;
  uploadedAt: string;           // ISO 8601 OffsetDateTime
}

interface StudentFilesResponse {
  resume: FileResponse | null;
  portfolioCount: number;
  avatarFileId: number | null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Chat
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface ChatResponse {
  id: number;
  mentoringRequestId: number;
  studentUserId: number;
  mentorUserId: number;
  createdAt: string;
  lastMessage: string | null;
  lastMessageAt: string | null;
  lastSenderUserId: number | null;
  unreadCount: number;
  studentName: string;
  mentorName: string;
  mentoringRequestStatus: string;
}

interface ChatMessageResponse {
  id: number;
  chatId: number;
  senderUserId: number;
  body: string | null;
  attachment: AttachmentInfo | null;
  createdAt: string;
}

interface AttachmentInfo {
  fileId: number;
  originalFilename: string;
  contentType: string;
  size: number;
}

interface SendMessageRequest {
  body?: string | null;
  attachmentFileId?: number | null;
}

interface TypingRequest {
  typing: boolean;              // @NotNull
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Reviews
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface ReviewCreateRequest {
  mentoringRequestId: number;
  rating: number;               // 1..5
  comment?: string | null;      // max 2000
}

interface ReviewResponse {
  id: number;
  mentoringRequestId: number;
  reviewerUserId: number;
  mentorUserId: number;
  rating: number;               // 1..5
  comment: string | null;
  moderationStatus: 'VISIBLE' | 'HIDDEN' | 'UNDER_REVIEW';
  createdAt: string;
}

interface ModerateReviewRequest {
  moderationStatus: 'VISIBLE' | 'HIDDEN' | 'UNDER_REVIEW';
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentoring Sessions (4.15)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface CreateSessionRequest {
  mentoringRequestId: number;
  scheduledAt: string;          // OffsetDateTime, @Future
  durationMinutes: number;      // 15..480
}

interface RescheduleSessionRequest {
  newScheduledAt: string;       // OffsetDateTime, @Future
  durationMinutes: number;      // 15..480
  reason?: string | null;       // max 500
}

interface CancelSessionRequest {
  reason?: string | null;       // max 500
}

interface SessionResponse {
  id: number;
  mentoringRequestId: number;
  studentUserId: number;
  mentorUserId: number;
  studentName: string;
  mentorName: string;
  scheduledAt: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'RESCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
  cancelReason: string | null;
  rescheduleReason: string | null;
  createdAt: string;
  updatedAt: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Notification Preferences (4.16)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface NotificationPreferencesResponse {
  emailRequestEvents: boolean;
  emailSessionEvents: boolean;
  emailReviewEvents: boolean;
}

interface UpdateNotificationPreferencesRequest {
  emailRequestEvents?: boolean | null;
  emailSessionEvents?: boolean | null;
  emailReviewEvents?: boolean | null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Complaints (4.17, 4.18)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface CreateComplaintRequest {
  targetType: 'REVIEW' | 'USER';
  targetId: number;
  reason: string;               // @NotBlank, max 2000
}

interface ResolveComplaintRequest {
  status: 'RESOLVED' | 'REJECTED';
  resolution?: string | null;   // max 2000
}

interface ComplaintResponse {
  id: number;
  targetType: 'REVIEW' | 'USER';
  targetId: number;
  reporterUserId: number;
  reason: string;
  status: 'OPEN' | 'RESOLVED' | 'REJECTED';
  resolution: string | null;
  resolvedBy: number | null;
  resolvedAt: string | null;
  createdAt: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Admin Audit Log (4.18)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface AuditLogResponse {
  id: number;
  adminUserId: number;
  action: 'ROLE_CHANGED' | 'REVIEW_MODERATED' | 'COMPLAINT_RESOLVED';
  targetType: string;           // 'USER' | 'REVIEW' | 'COMPLAINT'
  targetId: number | null;
  payload: string | null;       // JSON-строка; парсить через JSON.parse
  createdAt: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Notification Outbox (4.18)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface OutboxEntryResponse {
  id: number;
  recipientEmail: string;
  subject: string;
  eventType: string;            // 'request.accepted' | 'session.created' | ...
  status: 'PENDING' | 'SENT' | 'FAILED';
  attempts: number;
  lastError: string | null;
  nextAttemptAt: string | null;
  sentAt: string | null;
  createdAt: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Dashboard
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface DashboardSummaryResponse {
  role: string;
  sentRequests: number;
  pendingRequests: number;
  acceptedRequests: number;
  totalChats: number;
  unreadChats: number;
  profileCompletion: number;
  nextSession: null;            // зарезервировано
}

interface ActivityItemResponse {
  type: string;
  refId: number;
  description: string;
  occurredAt: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentor Stats
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface MentorStatsResponse {
  averageRating: number | null; // null если нет отзывов
  reviewCount: number;
  completedRequests: number;
  responseRate: number;         // 0.0..1.0
  level: 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';
  progress: number;             // 0.0..1.0, прогресс до следующего уровня
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Admin
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface AdminRoleRequest {
  role: 'STUDENT' | 'MENTOR';
}

// Stage 11
interface AdminUserStatusRequest {
  status: 'ACTIVE' | 'BLOCKED' | 'DELETED';
}

interface AdminUserResponse {
  id: number;
  email: string;
  status: string;                    // UserStatus как строка
  roles: string[];
  firstName: string | null;
  lastName: string | null;
  createdAt: string;
}

interface AdminUsersStatsResponse {
  totalUsers: number;
  byRole: Record<string, number>;    // { "STUDENT": 120, "MENTOR": 28, "ADMIN": 2 }
  byStatus: Record<string, number>;  // { "ACTIVE": 145, "BLOCKED": 3 }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Presence
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface PresenceResponse {
  userId: number;
  status: 'online' | 'offline';
  lastSeenAt: string | null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Enums (string unions)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

type UserStatus = 'ACTIVE' | 'EMAIL_NOT_CONFIRMED' | 'BLOCKED' | 'DELETED';
type RoleCode = 'STUDENT' | 'MENTOR' | 'ADMIN';

type EducationDegree = 'BACHELOR' | 'SPECIALIST' | 'MASTER' | 'COURSE' | 'OTHER';
type EducationForm = 'FULL_TIME' | 'PART_TIME' | 'DISTANCE';
type EmploymentType = 'PRACTICE' | 'INTERNSHIP' | 'PART_TIME' | 'FULL_TIME' | 'PROJECT' | 'OTHER';
type WorkFormat = 'REMOTE' | 'OFFICE' | 'HYBRID';
type LanguageLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2' | 'NATIVE';
type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'CONFIDENT';

type MentoringType = 'PRACTICE' | 'INTERNSHIP' | 'PROJECT';
type MentoringChannel = 'CHAT' | 'CALLS' | 'MIXED';
type MentoringDuration = 'ONE_MONTH' | 'THREE_MONTHS' | 'FLEXIBLE';
type RecruitmentStatus = 'OPEN' | 'PAUSED' | 'CLOSED';

type MentoringRequestDirection = 'STUDENT_TO_MENTOR' | 'MENTOR_TO_STUDENT';
type MentoringRequestStatus = 'SENT' | 'REVIEWING' | 'NEEDS_CLARIFICATION' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

type FileType = 'RESUME' | 'PORTFOLIO' | 'AVATAR' | 'CHAT_ATTACHMENT';
type FileStatus = 'ACTIVE' | 'DELETED';
```

---

## 6. Enum-справочник

### EducationDegree

| Значение | Лейбл для UI |
|---|---|
| `BACHELOR` | Бакалавр |
| `SPECIALIST` | Специалист |
| `MASTER` | Магистр |
| `COURSE` | Курсы |
| `OTHER` | Другое |

### EducationForm

| Значение | Лейбл для UI |
|---|---|
| `FULL_TIME` | Очно |
| `PART_TIME` | Очно-заочно |
| `DISTANCE` | Заочно |

### EmploymentType

| Значение | Лейбл для UI |
|---|---|
| `PRACTICE` | Практика |
| `INTERNSHIP` | Стажировка |
| `PART_TIME` | Part-time |
| `FULL_TIME` | Full-time |
| `PROJECT` | Проектная |
| `OTHER` | Другое |

### WorkFormat

| Значение | Лейбл для UI |
|---|---|
| `REMOTE` | Удалённо |
| `OFFICE` | Офис |
| `HYBRID` | Гибрид |

### LanguageLevel

| Значение | Лейбл для UI |
|---|---|
| `A1` | A1 — Начальный |
| `A2` | A2 — Элементарный |
| `B1` | B1 — Средний |
| `B2` | B2 — Выше среднего |
| `C1` | C1 — Продвинутый |
| `C2` | C2 — Мастерство |
| `NATIVE` | Родной |

### SkillLevel

| Значение | Лейбл для UI |
|---|---|
| `BEGINNER` | Начальный |
| `INTERMEDIATE` | Средний |
| `CONFIDENT` | Уверенный |

### MentoringType

| Значение | Лейбл для UI |
|---|---|
| `PRACTICE` | Практика |
| `INTERNSHIP` | Стажировка |
| `PROJECT` | Проектное сопровождение |

### MentoringChannel

| Значение | Лейбл для UI |
|---|---|
| `CHAT` | Чат |
| `CALLS` | Созвоны |
| `MIXED` | Смешанный |

### MentoringDuration

| Значение | Лейбл для UI |
|---|---|
| `ONE_MONTH` | 1 месяц |
| `THREE_MONTHS` | 3 месяца |
| `FLEXIBLE` | Гибко |

### RecruitmentStatus

| Значение | Лейбл для UI |
|---|---|
| `OPEN` | Набираю |
| `PAUSED` | Пауза |
| `CLOSED` | Нет мест |

### FileType

| Значение | Допустимые MIME | Max размер | Лейбл |
|---|---|---|---|
| `RESUME` | `application/pdf` | 5 MB | Резюме |
| `PORTFOLIO` | `application/pdf`, `image/jpeg`, `image/png` | 10 MB | Портфолио |
| `AVATAR` | `image/jpeg`, `image/png`, `image/webp` | 2 MB | Аватар |
| `CHAT_ATTACHMENT` | любой (safe MIME list) | 20 MB | Вложение в чат |

### FileStatus

| Значение | Описание |
|---|---|
| `ACTIVE` | Файл активен, доступен для скачивания |
| `DELETED` | Файл удалён (soft delete), не возвращается в списках |

### MentoringRequestDirection

| Значение | Описание |
|---|---|
| `STUDENT_TO_MENTOR` | Студент отправил заявку ментору |
| `MENTOR_TO_STUDENT` | Ментор отправил приглашение студенту |

### MentoringRequestStatus

| Значение | Лейбл для UI | Описание |
|---|---|---|
| `SENT` | Отправлена | Создана, адресат ещё не открыл |
| `REVIEWING` | На рассмотрении | Адресат просматривает |
| `NEEDS_CLARIFICATION` | Требует уточнения | Адресат запросил информацию |
| `ACCEPTED` | Принята | Менторство активно |
| `REJECTED` | Отклонена | Терминальный статус |
| `CANCELLED` | Отменена | Инициатор отменил; терминальный статус |
| `COMPLETED` | Завершена | Успешно завершено; терминальный статус |

### UserStatus

| Значение | Описание |
|---|---|
| `ACTIVE` | Активный аккаунт |
| `EMAIL_NOT_CONFIRMED` | Email не подтверждён |
| `BLOCKED` | Заблокирован |
| `DELETED` | Удалён (soft delete) |

### MentoringSessionStatus

| Значение | Описание |
|---|---|
| `SCHEDULED` | Сессия создана и запланирована |
| `RESCHEDULED` | Перенесена (заполнен `rescheduleReason`) |
| `COMPLETED` | Проведена |
| `CANCELLED` | Отменена участником (заполнен `cancelReason`) |
| `NO_SHOW` | Время прошло, никто не пометил `COMPLETED`/`CANCELLED` |

### ReviewModerationStatus

| Значение | Описание |
|---|---|
| `VISIBLE` | Виден публично, учитывается в среднем рейтинге |
| `HIDDEN` | Скрыт администратором, исключён из публичных списков и из рейтинга |
| `UNDER_REVIEW` | На модерации, временно скрыт |

### ComplaintStatus

| Значение | Описание |
|---|---|
| `OPEN` | Жалоба создана, ждёт админа |
| `RESOLVED` | Принята: контент скрыт / пользователь предупреждён |
| `REJECTED` | Отклонена как необоснованная |

### ComplaintTargetType

| Значение | Описание |
|---|---|
| `REVIEW` | Жалоба на отзыв (`targetId` = `reviewId`) |
| `USER` | Жалоба на пользователя (`targetId` = `userId`) |

### AuditAction

| Значение | Когда фиксируется |
|---|---|
| `ROLE_CHANGED` | `PUT /admin/users/{userId}/role` сменил роль |
| `REVIEW_MODERATED` | `PUT /admin/reviews/{id}/moderate` поменял `moderationStatus` |
| `COMPLAINT_RESOLVED` | `PUT /admin/complaints/{id}/resolve` закрыл жалобу |
| `USER_STATUS_CHANGED` | `PUT /admin/users/{userId}/status` сменил статус (Stage 11) |
| `DICTIONARY_CHANGED` | Любая мутация `/admin/dictionaries/**` — create/update/deactivate/restore (Stage 11) |

> `AuditLogResponse.payload` — это JSON-строка (Jackson сериализовал событие в `jsonb`-колонку). Распарсите `JSON.parse(payload)` чтобы получить `{adminUserId, ..., operation, dictionaryType, entityId, ...}`.

### NotificationOutboxStatus

| Значение | Описание |
|---|---|
| `PENDING` | В очереди, ждёт следующего тика шедулера (`nextAttemptAt`) |
| `SENT` | Письмо успешно отправлено (`sentAt` заполнен) |
| `FAILED` | Исчерпан лимит попыток (`app.notifications.outbox.max-attempts`, default 5); `lastError` содержит причину |

### DictionaryType (Stage 11)

| Значение | Соответствующий путь |
|---|---|
| `CITY` | `/admin/dictionaries/cities` |
| `SKILL` | `/admin/dictionaries/skills` |
| `LANGUAGE` | `/admin/dictionaries/languages` |
| `INTERACTION_TYPE` | `/admin/dictionaries/interaction-types` |

Появляется в payload `DictionaryChangedAuditEvent`.

### DictionaryOperation (Stage 11)

| Значение | Описание |
|---|---|
| `CREATE` | Создание записи (`POST`) |
| `UPDATE` | Обновление записи (`PUT /{id}`) |
| `DEACTIVATE` | Soft delete (`DELETE /{id}` → `active=false`) |
| `RESTORE` | Восстановление (`PUT /{id}/restore` → `active=true`) |

---

## 7. Загрузка файлов

### Общие правила

1. HTTP метод: `POST` для загрузки, `PUT` для замены
2. Content-Type: `multipart/form-data` (browser ставит автоматически при использовании `FormData`)
3. Имя поля формы: **`file`** (именно `file`, не `document`, не `upload`)
4. **Не устанавливать `Content-Type` вручную** — браузер сам поставит с правильным `boundary`
5. Ответ загрузки: `201 Created` с `FileResponse`

### Сводная таблица эндпоинтов файлов

| Эндпоинт | Метод | Типы | Max размер | Автоматика |
|---|---|---|---|---|
| `/files/resume` | POST | PDF | 5 MB | Soft-delete старого резюме |
| `/files/portfolio` | POST | PDF, JPEG, PNG | 10 MB | — |
| `/files/avatar` | POST | JPEG, PNG, WebP | 2 MB | Soft-delete старого аватара |
| `/files/chat-attachment` | POST | любой | 20 MB | — |
| `/files` | GET | — | — | Список ACTIVE файлов |
| `/files/{id}/download` | GET | — | — | Скачать из MinIO |
| `/files/{id}` | DELETE | — | — | Soft-delete |
| `/files/{id}/replace` | PUT | тот же тип | тот же лимит | — |

### Пример: загрузка аватара (React)

```tsx
async function uploadAvatar(file: File, token: string): Promise<FileResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE_URL}/files/avatar`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) throw await response.json();
  return response.json();
}
```

### Пример: загрузка резюме с клиентской валидацией

```tsx
function validateResume(file: File): string | null {
  if (file.type !== 'application/pdf') return 'Резюме должно быть в формате PDF';
  if (file.size > 5 * 1024 * 1024) return 'Размер не должен превышать 5 МБ';
  return null;
}

async function uploadResume(file: File, token: string): Promise<FileResponse> {
  const error = validateResume(file);
  if (error) throw new Error(error);

  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE_URL}/files/resume`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) throw await response.json();
  return response.json();
}
```

### Клиентская валидация (рекомендация)

```typescript
const FILE_CONSTRAINTS: Record<string, { accept: string; maxSize: number }> = {
  resume:          { accept: '.pdf',                     maxSize: 5 * 1024 * 1024  },
  portfolio:       { accept: '.pdf,.jpg,.jpeg,.png',     maxSize: 10 * 1024 * 1024 },
  avatar:          { accept: '.jpg,.jpeg,.png,.webp',    maxSize: 2 * 1024 * 1024  },
  chatAttachment:  { accept: '*',                        maxSize: 20 * 1024 * 1024 },
};
```

### Пример: менеджер файлов (список + скачивание)

```typescript
// Загрузить список активных файлов
async function listFiles(token: string, type?: FileType): Promise<PagedResponse<FileResponse>> {
  const params = new URLSearchParams({ sort: 'uploadedAt,desc', size: '50' });
  if (type) params.set('type', type);

  return apiCall<PagedResponse<FileResponse>>(`${BASE_URL}/files?${params}`);
}

// Скачать файл
async function downloadFile(fileId: number, token: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/files/${fileId}/download`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!response.ok) throw await response.json();

  const blob = await response.blob();
  const cd = response.headers.get('Content-Disposition') ?? '';
  const match = cd.match(/filename="(.+)"/);
  const filename = match ? match[1] : `file-${fileId}`;

  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// Удалить файл
async function deleteFile(fileId: number, token: string): Promise<void> {
  await apiCall<void>(`${BASE_URL}/files/${fileId}`, { method: 'DELETE' });
}
```

---

## 8. Типичные сценарии (User Flows)

### Сценарий 1: Регистрация и создание профиля студента

```
1. POST /auth/register
   → { id, email, roles: ["STUDENT"] }

2. POST /auth/login
   → { accessToken, tokenType: "Bearer", user: {...} }
   Сохранить accessToken

3. GET /profile/me
   → { role: "STUDENT", profileId: null, profileExists: false }
   → Показать форму создания профиля

4. Параллельно:
   GET /dictionaries/cities
   GET /dictionaries/skills
   GET /dictionaries/languages

5. PUT /profile/student
   → { id: 42, firstName: "Иван", ..., resumeFileId: null }

6. POST /files/avatar (опционально)
   → { id: 100, fileType: "AVATAR", status: "ACTIVE", ... }

7. POST /files/resume (опционально)
   → { id: 101, fileType: "RESUME", status: "ACTIVE", ... }
   → В профиле появится resumeFileId: 101
```

---

### Сценарий 2: Вход существующего ментора

```
1. POST /auth/login
   → { accessToken, user: { roles: ["MENTOR"], ... } }

2. GET /profile/me
   → { role: "MENTOR", profileId: 10, profileExists: true }

3. GET /profile/mentor/me
   → Полные данные профиля

4. GET /dashboard/summary
   → { role: "MENTOR", sentRequests, pendingRequests, ... }

5. GET /mentor-stats/me
   → { averageRating, reviewCount, level: "INTERMEDIATE", ... }
```

---

### Сценарий 3: Дерево решений после логина

```
POST /auth/login → сохранить token
       │
GET /profile/me
       │
       ├── role: "ADMIN"
       │   └── → Панель администратора
       │
       ├── role: "MENTOR"
       │   ├── profileExists: false → Форма создания профиля ментора
       │   └── profileExists: true  → GET /profile/mentor/me → Дашборд
       │
       └── role: "STUDENT"
           ├── profileExists: false → Форма создания профиля студента
           └── profileExists: true  → GET /profile/student/me → Дашборд
```

---

### Сценарий 4: Поиск ментора и отправка заявки

```
1. GET /profiles/mentors?recruitmentStatus=OPEN&skillIds=1&page=0&size=20
   → PagedResponse<MentorCardResponse>

2. GET /profiles/mentors/10
   → Полный профиль ментора (description, expectations, canHelpWith)

3. POST /mentoring/requests
   { targetProfileId: 10, goalType: "PRACTICE", message: "..." }
   → MentoringRequestResponse { status: "SENT" }
```

---

### Сценарий 5: Обработка заявки ментором

```
1. GET /mentoring/requests?status=SENT
   → Новые заявки

2. PUT /mentoring/requests/1/view
   → status: "REVIEWING"

3. PUT /mentoring/requests/1/accept
   → status: "ACCEPTED"
   (автоматически создаётся чат)

4. GET /chats/by-request/1
   → ChatResponse { id: 5, ... }

5. Позже: PUT /mentoring/requests/1/complete
   → status: "COMPLETED"
```

---

### Сценарий 6: SSE-чат в реальном времени

```
1. GET /chats
   → Список чатов с unreadCount

2. GET /chats/5
   → ChatResponse (текущий чат)

3. GET /chats/5/messages/cursor?limit=20
   → Последние 20 сообщений (для первой загрузки)

4. Подключиться к SSE:
   GET /chats/events
   Authorization: Bearer <token>

5. При событии chat.message.created:
   → Добавить сообщение в список, обновить lastMessage в чате

6. POST /chats/5/messages { body: "Привет!" }
   → ChatMessageResponse
   (другому участнику придёт SSE chat.message.created)

7. POST /chats/5/read
   → 204 (сбросить unreadCount)
   (другому участнику придёт SSE chat.read)

8. При прокрутке вверх:
   GET /chats/5/messages/cursor?beforeMessageId=<oldest>&limit=20
   → Более старые сообщения
```

---

### Сценарий 7: Менеджер файлов

```
1. GET /files?sort=uploadedAt,desc
   → PagedResponse<FileResponse> — все активные файлы

2. GET /files?type=PORTFOLIO
   → Только файлы портфолио

3. GET /files/100/download
   → Бинарный поток → скачать

4. DELETE /files/100
   → 204 (soft delete)

5. PUT /files/101/replace
   (FormData с новым файлом)
   → FileResponse (обновлённые метаданные)
```

---

### Сценарий 8: Каталог студентов (для ментора)

```
1. GET /profiles/students?sort=createdAt,desc&page=0&size=20
   → PagedResponse<StudentProfileResponse>

2. Фильтрация:
   GET /profiles/students?q=иван&skillIds=1&workFormat=REMOTE
   → Отфильтрованный список

3. GET /profiles/students/42
   → Полный профиль студента

4. POST /mentoring/requests
   { targetProfileId: 42, goalType: "INTERNSHIP", message: "..." }
   → Отправить приглашение студенту
```

---

### Сценарий 9: Оставить отзыв после завершения менторства

```
1. GET /mentoring/requests?status=COMPLETED
   → Завершённые заявки

2. GET /reviews/by-request/5
   → 404 если отзыва нет

3. POST /reviews
   { mentoringRequestId: 5, rating: 5, comment: "Отличный ментор!" }
   → ReviewResponse { id: 1, rating: 5, ... }

4. GET /profiles/mentors/10/reviews
   → Публичный список отзывов ментора (без авторизации)
```

---

### Сценарий 10: Работа администратора

```
1. GET /admin/users/stats
   → { totalUsers: 150, byRole: {...}, byStatus: {...} }

2. GET /admin/users?q=иван&role=STUDENT&page=0
   → PagedResponse<AdminUserResponse>

3. PUT /admin/users/42/role
   { "role": "MENTOR" }
   → 200 (пользователь стал ментором)
   (пользователь должен перелогиниться для получения нового JWT)
```

---

## 9. Частые вопросы и подводные камни

### Q: Как понять роль пользователя?

**A:** Два источника:
1. `POST /auth/login` → `user.roles[]` — массив ролей после логина
2. `GET /profile/me` → `role` — основная роль (определяет тип дашборда)

Приоритет при определении `role`: `ADMIN` > `MENTOR` > `STUDENT`.

---

### Q: Что передавать в `cityId`, `languageId`, `skillId`?

**A:** ID из справочников. Загрузите при старте приложения:
```typescript
const [cities, skills, languages] = await Promise.all([
  apiCall<CityResponse[]>('/dictionaries/cities'),
  apiCall<SkillResponse[]>('/dictionaries/skills'),
  apiCall<LanguageResponse[]>('/dictionaries/languages'),
]);
```

---

### Q: Как работает upsert для профиля?

**A:** `PUT` — один эндпоинт для создания и обновления:
- Профиля нет → создаётся.
- Профиль есть → **все поля** перезаписываются.

**Коллекции полностью заменяются:** передайте `[]` чтобы удалить все записи.

`PATCH /profile/student` — обновляет **только переданные поля** скалярного типа. Коллекции (educations, skills, languages) не затрагиваются.

---

### Q: В чём разница PUT и PATCH для профиля студента?

**A:**
- `PUT /profile/student` — полный upsert. Все поля, включая все коллекции, перезаписываются. Подходит для формы создания и полного редактирования.
- `PATCH /profile/student` — частичное обновление только скалярных полей. Коллекции меняются через отдельные эндпоинты: `PUT /profile/student/skills` и `PUT /profile/student/languages`.

---

### Q: Как отличить "профиль не создан" от "профиль пустой"?

**A:** `GET /profile/me` → `profileExists: false` — профиля нет вообще, `GET /profile/student/me` вернёт 404. После любого `PUT /profile/student` → `profileExists: true`.

---

### Q: Почему в ответе `employmentTypes` — массив строк?

**A:** Сервер конвертирует enum → `Enum.name()` → строка `"FULL_TIME"`. При отправке на сервер используйте те же строки.

---

### Q: Как можно загрузить резюме без профиля студента?

**A:** Нельзя. `POST /files/resume` вернёт `404 Not Found`. Сначала создайте профиль через `PUT /profile/student`.

---

### Q: Что происходит при повторной загрузке аватара или резюме?

**A:** Старый файл автоматически помечается как `DELETED` (soft delete). В `GET /files` он больше не появляется. Новый файл получает статус `ACTIVE` и становится актуальным.

---

### Q: Как работает курсорная пагинация в чате?

**A:** `GET /chats/{chatId}/messages/cursor` возвращает сообщения строго **старше** `beforeMessageId`. Алгоритм для бесконечной прокрутки:
1. Первая загрузка: запрос без `beforeMessageId` → получить последние N сообщений.
2. При прокрутке вверх: передать `beforeMessageId = id` самого старого из уже загруженных сообщений.
3. Если пришло меньше `limit` сообщений — достигнуто начало истории.

---

### Q: Как подключиться к SSE с JWT?

**A:** Браузерный `EventSource` не поддерживает заголовки. Варианты:
1. Библиотека `@microsoft/fetch-event-source` (рекомендуется).
2. Нативный `fetch` с `ReadableStream` и ручным парсингом.
3. Передача токена через query param (менее безопасно).

```typescript
// @microsoft/fetch-event-source
import { fetchEventSource } from '@microsoft/fetch-event-source';

fetchEventSource(`${BASE_URL}/chats/events`, {
  headers: { 'Authorization': `Bearer ${token}` },
  onmessage(event) { /* ... */ },
  signal: abortController.signal, // для отключения
});
```

---

### Q: Как определить, является ли текущий пользователь инициатором заявки?

**A:**
```typescript
function isInitiator(
  request: MentoringRequestResponse,
  myProfileId: number,
  myRole: 'STUDENT' | 'MENTOR'
): boolean {
  if (request.direction === 'STUDENT_TO_MENTOR') {
    return myRole === 'STUDENT' && request.studentProfileId === myProfileId;
  } else {
    return myRole === 'MENTOR' && request.mentorProfileId === myProfileId;
  }
}

function isRecipient(
  request: MentoringRequestResponse,
  myProfileId: number,
  myRole: 'STUDENT' | 'MENTOR'
): boolean {
  return !isInitiator(request, myProfileId, myRole);
}
```

---

### Q: Что значит `unreadCount` в ChatResponse и как его обнулить?

**A:** `unreadCount` — количество сообщений, которые текущий пользователь ещё не прочитал в этом чате. Обнуляется вызовом `POST /chats/{chatId}/read`. Вызывайте его при открытии чата пользователем.

---

### Q: Как парсить details в ошибке валидации?

**A:** Формат: `"fieldName: error message"`. Разделитель — первый `: `.

```typescript
function parseValidationDetails(details: string[]): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const detail of details) {
    const idx = detail.indexOf(': ');
    if (idx !== -1) {
      errors[detail.substring(0, idx)] = detail.substring(idx + 2);
    }
  }
  return errors;
}
// → { email: "Некорректный формат email", password: "..." }
```

---

### Q: Какой формат даты использовать?

**A:**
- `availableFrom` в профиле студента — ISO date: `"2025-04-01"` (`LocalDate`)
- `uploadedAt`, `createdAt`, `occurredAt` в ответах — ISO datetime с timezone: `"2025-03-19T14:30:00+03:00"` (`OffsetDateTime`)
- `timestamp` в ошибках — ISO instant в UTC: `"2025-03-19T11:30:00.123Z"` (`Instant`)

---

### Q: После смены роли через `/admin/users/{id}/role` старый токен перестаёт работать?

**A:** Да. При смене роли backend инкрементит `User.tokenVersion` — так же, как при блокировке. На ближайшем запросе со старым токеном пользователь получит `401`. Это намеренное поведение: пользователь должен перелогиниться, чтобы получить JWT с новыми authorities. Кеш `userDetails` также сбрасывается.

---

### Q: А после блокировки через `/admin/users/{id}/status` (Stage 11)?

**A:** Да, токен моментально становится невалидным. При переходе в `BLOCKED` или `DELETED` backend инкрементит `User.tokenVersion`, который зашит в claim `tv` каждого JWT. На ближайшем запросе фронт получит `401`. Это поведение отличается от `ROLE_CHANGED` — там токен остаётся технически валидным со старыми ролями.

Возврат в `ACTIVE` (unblock) **не возвращает** `tokenVersion` назад — пользователь обязан перелогиниться. На фронте: на `401` всегда чистите хранилище и редиректьте на `/login`.

---

### Q: Сервер возвращает 429 на `/auth/login` — что это?

**A:** Backend применяет rate-limiting: не более **10 запросов с одного IP в минуту** на `POST /auth/login`, `POST /auth/password/forgot`, `POST /auth/password/reset`. При превышении — `429 Too Many Requests`. Рекомендации для фронтенда:
- Добавьте обработку `429` в глобальный перехватчик ошибок.
- Показывайте пользователю сообщение «Слишком много попыток. Повторите через минуту».
- Не делайте автоматических повторных запросов при 429 — это только продлит блокировку.

```typescript
if (res.status === 429) {
  throw new Error('Слишком много запросов. Попробуйте позже.');
}
```

---

### Q: Чем отличается публичный `GET /dictionaries/{type}` от админского `GET /admin/dictionaries/{type}`?

**A:**
| | Публичный `/dictionaries/{type}` | Админский `/admin/dictionaries/{type}` |
|---|---|---|
| Доступ | Без токена | JWT с ролью `ADMIN` |
| Видны записи | Только `active = true` | **Все**, включая `active = false` |
| Кеш | Caffeine 300 сек | Без кеша |
| Поле `active` в ответе | Всегда `true` | Реальное значение |

После CRUD-мутации админом кеш `dictionaries` сбрасывается, поэтому на ближайшем публичном `GET` фронт увидит обновлённые данные.
