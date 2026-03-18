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
| Max file upload | 10 MB (global limit), индивидуальные лимиты по типу файла |
| Даты | ISO 8601: `"2025-03-19"` (LocalDate), `"2025-03-19T14:30:00+03:00"` (OffsetDateTime), `"2025-03-19T11:30:00Z"` (Instant) |
| Null-поля | Отсутствуют в JSON (используется `@JsonInclude(NON_NULL)` в ErrorResponse; в остальных DTO null-поля **присутствуют** как `null`) |

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
| 404 | `NOT_FOUND` | Ресурс не найден (профиль, город, навык и т.д.) | — |
| 405 | `METHOD_NOT_ALLOWED` | Неправильный HTTP-метод | — |
| 409 | `CONFLICT` | Конфликт (например, email уже зарегистрирован) | — |
| 422 | `UNPROCESSABLE_ENTITY` | Бизнес-правило нарушено (неверный формат файла, превышен размер) | — |
| 500 | `INTERNAL_SERVER_ERROR` | Ошибка сервера (включая проблемы с хранилищем файлов) | — |

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

### Пример: ресурс не найден

```json
{
  "timestamp": "2025-03-19T11:30:00.456Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Профиль студента не найден",
  "path": "/profile/student/me"
}
```

### Пример: бизнес-правило (файл)

```json
{
  "timestamp": "2025-03-19T11:30:00.789Z",
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Резюме должно быть в формате PDF",
  "path": "/files/resume"
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
      // Токен истёк → на логин
      redirectToLogin();
      throw error;
    }

    if (res.status === 400 && error.error === 'VALIDATION_ERROR') {
      // Показать ошибки по полям
      // error.details = ["firstName: must not be blank", ...]
      // Парсить как: field = часть до ':', message = часть после ':'
      throw error;
    }

    // Остальные ошибки — показать error.message
    throw error;
  }

  // 204 No Content (forgotPassword, resetPassword)
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

**Важно:** Email нормализуется к нижнему регистру на сервере. `"User@Example.COM"` сохранится как `"user@example.com"`.

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

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `email` | string | да | `@NotBlank`, `@Email` |
| `password` | string | да | `@NotBlank` |

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHVkZW50QGV4YW1wbGUuY29tIi...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "roles": ["STUDENT"],
    "status": "ACTIVE"
  }
}
```

**Важно:**
- Поле `tokenType` всегда `"Bearer"`
- Токен действует 24 часа
- `user.roles` — массив строк: `"STUDENT"`, `"MENTOR"`, `"ADMIN"`
- `user.status` — одно из: `"ACTIVE"`, `"EMAIL_NOT_CONFIRMED"`, `"BLOCKED"`, `"DELETED"`

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

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет/невалидный токен |

---

#### `POST /auth/password/forgot` — Запрос сброса пароля

**Auth:** Не требуется

**Request:**
```json
{
  "email": "student@example.com"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `email` | string | да | `@NotBlank`, `@Email` |

**Response:** `200 OK` (пустое тело)

**Поведение:** Всегда возвращает 200, даже если email не найден (защита от enumeration).

---

#### `POST /auth/password/reset` — Сброс пароля

**Auth:** Не требуется

**Request:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newSecurePass123"
}
```

| Поле | Тип | Обязательное | Валидация |
|---|---|---|---|
| `token` | string | да | `@NotBlank` |
| `newPassword` | string | да | `@NotBlank`, min 8 символов |

**Response:** `200 OK` (пустое тело)

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 404 | Токен не найден / истёк |

---

### 4.2 Dictionaries

Справочники — публичные, кешируются клиентом. Используются как look-up для выпадающих списков.

#### `GET /dictionaries/cities` — Список городов

**Auth:** Не требуется

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Москва",
    "region": "Московская область",
    "country": "Россия"
  },
  {
    "id": 2,
    "name": "Санкт-Петербург",
    "region": "Ленинградская область",
    "country": "Россия"
  }
]
```

---

#### `GET /dictionaries/skills` — Список навыков

**Auth:** Не требуется

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Java",
    "category": "Backend"
  },
  {
    "id": 2,
    "name": "React",
    "category": "Frontend"
  }
]
```

---

#### `GET /dictionaries/languages` — Список языков

**Auth:** Не требуется

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Английский",
    "code": "en"
  },
  {
    "id": 2,
    "name": "Русский",
    "code": "ru"
  }
]
```

---

#### `GET /dictionaries/interaction-types` — Типы взаимодействия

**Auth:** Не требуется

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Менторинг",
    "description": "Индивидуальное сопровождение ментором"
  }
]
```

---

### 4.3 Profile Summary

#### `GET /profile/me` — Сводка профиля текущего пользователя

**Auth:** Требуется JWT

Первый запрос после логина — определяет роль и наличие заполненного профиля. На основе ответа фронтенд решает, показывать форму создания профиля или дашборд.

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

**Логика определения роли:**
1. Если у пользователя роль `ADMIN` → `"ADMIN"`, профиль не проверяется
2. Если роль `MENTOR` → `"MENTOR"`, проверяется `mentor_profiles`
3. Если роль `STUDENT` (или нет ролей) → `"STUDENT"`, проверяется `student_profiles`

**Пример: пользователь без профиля**
```json
{
  "role": "STUDENT",
  "profileId": null,
  "profileExists": false
}
```

---

### 4.4 Student Profile

#### `PUT /profile/student` — Создать или обновить профиль студента

**Auth:** Требуется JWT

**Семантика:** Upsert. Один и тот же эндпоинт для создания и обновления. Если профиль не существует — создаётся. Если существует — полностью перезаписывается (все поля заменяются, включая коллекции).

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
    {
      "languageId": 1,
      "level": "B2"
    }
  ],
  "skills": [
    {
      "skillId": 1,
      "level": "INTERMEDIATE"
    },
    {
      "skillId": 2,
      "level": "BEGINNER"
    }
  ]
}
```

**Полная таблица полей запроса:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `firstName` | string | **да** | `@NotBlank` | Имя |
| `lastName` | string | **да** | `@NotBlank` | Фамилия |
| `middleName` | string | нет | — | Отчество |
| `phone` | string | нет | — | Телефон |
| `cityId` | number | нет | Должен существовать в `/dictionaries/cities` | ID города |
| `desiredPosition` | string | нет | — | Желаемая должность |
| `hoursPerWeek` | number | нет | min: 0, max: 168 | Часов в неделю |
| `availableFrom` | string | нет | ISO 8601 date (`"YYYY-MM-DD"`) | Дата готовности |
| `about` | string | нет | — | О себе |
| `max` | string | нет | — | Доп. контактная информация |
| `employmentTypes` | string[] | нет | Enum: см. [EmploymentType](#employmenttype) | Типы занятости |
| `workFormats` | string[] | нет | Enum: см. [WorkFormat](#workformat) | Форматы работы |
| `educations` | object[] | нет | `@Valid` каждый элемент | Образование |
| `languages` | object[] | нет | `@Valid` каждый элемент | Языки |
| `skills` | object[] | нет | `@Valid` каждый элемент | Навыки |

**Вложенный объект `educations[*]`:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `institution` | string | **да** | `@NotBlank` | Учебное заведение |
| `specialty` | string | нет | — | Специальность |
| `degree` | string | нет | Enum: см. [EducationDegree](#educationdegree) | Степень |
| `educationForm` | string | нет | Enum: см. [EducationForm](#educationform) | Форма обучения |
| `startYear` | number | нет | min: 1900, max: 2100 | Год начала |
| `graduationYear` | number | нет | min: 1900, max: 2100 | Год окончания |

**Вложенный объект `languages[*]`:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `languageId` | number | **да** | `@NotNull`, должен существовать в `/dictionaries/languages` | ID языка |
| `level` | string | **да** | `@NotNull`, Enum: см. [LanguageLevel](#languagelevel) | Уровень |

**Вложенный объект `skills[*]`:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `skillId` | number | **да** | `@NotNull`, должен существовать в `/dictionaries/skills` | ID навыка |
| `level` | string | **да** | `@NotNull`, Enum: см. [SkillLevel](#skilllevel) | Уровень |

**Response:** `200 OK`
```json
{
  "id": 42,
  "userId": 1,
  "firstName": "Иван",
  "lastName": "Петров",
  "middleName": "Сергеевич",
  "phone": "+7 999 123-45-67",
  "city": {
    "id": 1,
    "name": "Москва",
    "region": "Московская область",
    "country": "Россия"
  },
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
      "language": {
        "id": 1,
        "name": "Английский",
        "code": "en"
      },
      "level": "B2"
    }
  ],
  "skills": [
    {
      "id": 7,
      "skill": {
        "id": 1,
        "name": "Java",
        "category": "Backend"
      },
      "level": "INTERMEDIATE"
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
| 404 | `cityId` не найден / `languageId` не найден / `skillId` не найден |

**Важные нюансы:**
- `employmentTypes` и `workFormats` в request передаются как **enum-имена** (`"FULL_TIME"`, `"REMOTE"`), в response возвращаются как **строки** (те же enum-имена)
- `educations`, `languages`, `skills` — при каждом PUT **полностью заменяются**. Если передать `null` или не включить поле — существующие записи удаляются
- `cityId` в request → развёрнутый `city` объект в response
- `languageId` в request → развёрнутый `language` объект в response
- `skillId` в request → развёрнутый `skill` объект в response
- `resumeFileId` заполняется через отдельный эндпоинт `POST /files/resume`

---

#### `GET /profile/student/me` — Мой профиль студента

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична ответу `PUT /profile/student`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 404 | Профиль студента не создан |

---

#### `GET /profiles/students/{id}` — Профиль студента по ID

**Auth:** Требуется JWT

**Path:** `id` — числовой ID профиля (поле `id` из `StudentProfileResponse`)

**Response:** `200 OK` — структура идентична ответу `PUT /profile/student`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 404 | Профиль с таким ID не найден |

---

### 4.5 Mentor Profile

#### `PUT /profile/mentor` — Создать или обновить профиль ментора

**Auth:** Требуется JWT

**Семантика:** Upsert (аналогично студенту).

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
  "description": "10+ лет опыта в Java/Spring. Помогаю войти в профессию.",
  "expectations": "Минимум 10 часов в неделю на практику. Базовые знания Java.",
  "canHelpWith": "Backend-разработка, System Design, подготовка к собеседованиям",
  "mentoringType": "INTERNSHIP",
  "mentoringChannel": "MIXED",
  "mentoringFrequency": "2 раза в неделю",
  "mentoringDuration": "THREE_MONTHS",
  "menteeLimit": 5,
  "recruitmentStatus": "OPEN",
  "skills": [
    {
      "skillId": 1,
      "level": "CONFIDENT"
    }
  ]
}
```

**Полная таблица полей запроса:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `firstName` | string | **да** | `@NotBlank` | Имя |
| `lastName` | string | **да** | `@NotBlank` | Фамилия |
| `middleName` | string | нет | — | Отчество |
| `position` | string | нет | — | Должность |
| `department` | string | нет | — | Отдел / компания |
| `cityId` | number | нет | Должен существовать в `/dictionaries/cities` | ID города |
| `phone` | string | нет | — | Телефон |
| `max` | string | нет | — | Доп. контактная информация |
| `description` | string | нет | — | О себе |
| `expectations` | string | нет | — | Ожидания от менти |
| `canHelpWith` | string | нет | — | Чем могу помочь |
| `mentoringType` | string | нет | Enum: см. [MentoringType](#mentoringtype) | Тип менторинга |
| `mentoringChannel` | string | нет | Enum: см. [MentoringChannel](#mentoringchannel) | Канал связи |
| `mentoringFrequency` | string | нет | — | Частота встреч (свободный текст) |
| `mentoringDuration` | string | нет | Enum: см. [MentoringDuration](#mentoringduration) | Длительность менторинга |
| `menteeLimit` | number | нет | min: 0, max: 100 | Макс. кол-во менти |
| `recruitmentStatus` | string | нет | Enum: см. [RecruitmentStatus](#recruitmentstatus) | Статус набора (при `null` сбрасывается к `"OPEN"`) |
| `skills` | object[] | нет | `@Valid` каждый элемент | Навыки |

**Вложенный объект `skills[*]`:**

| Поле | Тип | Обязательное | Валидация | Описание |
|---|---|---|---|---|
| `skillId` | number | **да** | `@NotNull`, должен существовать в `/dictionaries/skills` | ID навыка |
| `level` | string | **да** | `@NotNull`, Enum: см. [SkillLevel](#skilllevel) | Уровень |

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
  "city": {
    "id": 1,
    "name": "Москва",
    "region": "Московская область",
    "country": "Россия"
  },
  "phone": "+7 999 987-65-43",
  "max": "t.me/alexey_smirnov",
  "description": "10+ лет опыта в Java/Spring. Помогаю войти в профессию.",
  "expectations": "Минимум 10 часов в неделю на практику. Базовые знания Java.",
  "canHelpWith": "Backend-разработка, System Design, подготовка к собеседованиям",
  "mentoringType": "INTERNSHIP",
  "mentoringChannel": "MIXED",
  "mentoringFrequency": "2 раза в неделю",
  "mentoringDuration": "THREE_MONTHS",
  "menteeLimit": 5,
  "recruitmentStatus": "OPEN",
  "skills": [
    {
      "id": 15,
      "skill": {
        "id": 1,
        "name": "Java",
        "category": "Backend"
      },
      "level": "CONFIDENT"
    }
  ]
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 400 | Невалидные поля |
| 401 | Нет токена |
| 404 | `cityId` не найден / `skillId` не найден |

**Важные нюансы:**
- `recruitmentStatus`: если передать `null` или не передать поле — сбрасывается к `"OPEN"` (дефолт). Это NOT NULL поле в БД
- `skills` — при каждом PUT **полностью заменяются** (как и у студента)
- Все enum-поля в response приходят как строки (`"INTERNSHIP"`, `"MIXED"`, `"THREE_MONTHS"`)

---

#### `GET /profile/mentor/me` — Мой профиль ментора

**Auth:** Требуется JWT

**Response:** `200 OK` — структура идентична ответу `PUT /profile/mentor`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 404 | Профиль ментора не создан |

---

#### `GET /profiles/mentors/{id}` — Профиль ментора по ID

**Auth:** Требуется JWT

**Path:** `id` — числовой ID профиля (поле `id` из `MentorProfileResponse`)

**Response:** `200 OK` — структура идентична ответу `PUT /profile/mentor`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 404 | Профиль с таким ID не найден |

---

### 4.6 Files

Файлы загружаются как `multipart/form-data`. Имя поля формы: **`file`**.

#### `POST /files/resume` — Загрузить резюме

**Auth:** Требуется JWT

**Ограничения:**
- **Формат:** только PDF (`application/pdf`)
- **Размер:** max 5 MB
- **Предусловие:** у пользователя должен существовать профиль студента (сначала `PUT /profile/student`)
- **Поведение:** после загрузки файл привязывается к профилю студента (`resumeFileId`)

**Request:** `Content-Type: multipart/form-data`
```
file: <binary PDF>
```

```typescript
const formData = new FormData();
formData.append('file', pdfFile); // File object

const response = await fetch(`${BASE_URL}/files/resume`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    // НЕ ставить Content-Type — browser сам поставит с boundary
  },
  body: formData,
});
```

**Response:** `201 Created`
```json
{
  "id": 100,
  "originalFilename": "Resume_Ivan_Petrov.pdf",
  "contentType": "application/pdf",
  "size": 245760,
  "fileType": "RESUME",
  "uploadedAt": "2025-03-19T14:30:00+03:00"
}
```

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 404 | Профиль студента не создан |
| 422 | Не PDF / превышен размер 5 MB |
| 500 | Ошибка хранилища (MinIO недоступен) |

---

#### `POST /files/portfolio` — Загрузить файл портфолио

**Auth:** Требуется JWT

**Ограничения:**
- **Формат:** PDF, JPEG, PNG (`application/pdf`, `image/jpeg`, `image/png`)
- **Размер:** max 10 MB
- **Предусловие:** нет (не привязывается к профилю автоматически)

**Request:** `Content-Type: multipart/form-data`
```
file: <binary>
```

**Response:** `201 Created` — структура идентична ответу `/files/resume`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 422 | Неподдерживаемый формат / превышен размер 10 MB |
| 500 | Ошибка хранилища |

---

#### `POST /files/avatar` — Загрузить аватар

**Auth:** Требуется JWT

**Ограничения:**
- **Формат:** JPEG, PNG, WebP (`image/jpeg`, `image/png`, `image/webp`)
- **Размер:** max 2 MB
- **Поведение:** привязывается к пользователю (user), не к профилю

**Request:** `Content-Type: multipart/form-data`
```
file: <binary image>
```

**Response:** `201 Created` — структура идентична ответу `/files/resume`

**Ошибки:**
| Код | Когда |
|---|---|
| 401 | Нет токена |
| 422 | Неподдерживаемый формат / превышен размер 2 MB |
| 500 | Ошибка хранилища |

---

## 5. TypeScript-типы (полная карта DTO)

Копируйте в проект как есть. Типы 1:1 соответствуют серверным Java records.

```typescript
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Auth
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface RegisterRequest {
  email: string;           // @NotBlank, @Email
  password: string;        // @NotBlank, min 8
  firstName: string;       // @NotBlank, max 100
  lastName: string;        // @NotBlank, max 100
}

interface RegisterResponse {
  id: number;
  email: string;
  roles: string[];         // ["STUDENT"] / ["MENTOR"] / ["ADMIN"]
}

interface LoginRequest {
  email: string;           // @NotBlank, @Email
  password: string;        // @NotBlank
}

interface LoginResponse {
  accessToken: string;     // JWT строка
  tokenType: string;       // всегда "Bearer"
  user: UserInfoResponse;
}

interface UserInfoResponse {
  id: number;
  email: string;
  roles: string[];         // ["STUDENT", "MENTOR", ...]
  status: UserStatus;
}

interface ForgotPasswordRequest {
  email: string;           // @NotBlank, @Email
}

interface ResetPasswordRequest {
  token: string;           // @NotBlank, UUID из письма
  newPassword: string;     // @NotBlank, min 8
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Error
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface ErrorResponse {
  timestamp: string;       // ISO 8601 Instant
  status: number;          // HTTP status code
  error: string;           // "VALIDATION_ERROR" | "NOT_FOUND" | ...
  message: string;         // Человекочитаемое (на русском)
  path: string;            // URI запроса
  details?: string[];      // Только для VALIDATION_ERROR
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
  region: string;
  country: string;
}

interface SkillResponse {
  id: number;
  name: string;
  category: string;
}

interface LanguageResponse {
  id: number;
  name: string;
  code: string;            // ISO 639-1: "en", "ru", "de"
}

interface InteractionTypeResponse {
  id: number;
  name: string;
  description: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Student Profile
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface StudentProfileRequest {
  firstName: string;                           // @NotBlank
  lastName: string;                            // @NotBlank
  middleName?: string | null;
  phone?: string | null;
  cityId?: number | null;                      // FK → /dictionaries/cities
  desiredPosition?: string | null;
  hoursPerWeek?: number | null;                // 0..168
  availableFrom?: string | null;               // "YYYY-MM-DD"
  about?: string | null;
  max?: string | null;
  employmentTypes?: EmploymentType[] | null;
  workFormats?: WorkFormat[] | null;
  educations?: StudentEducationRequest[] | null;
  languages?: StudentLanguageRequest[] | null;
  skills?: StudentSkillRequest[] | null;
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
  availableFrom: string | null;                // "YYYY-MM-DD"
  about: string | null;
  max: string | null;
  employmentTypes: string[];                   // Set → массив enum-имён
  workFormats: string[];                       // Set → массив enum-имён
  educations: StudentEducationResponse[];
  languages: StudentLanguageResponse[];
  skills: StudentSkillResponse[];
  resumeFileId: number | null;                 // ID загруженного резюме
}

interface StudentEducationRequest {
  institution: string;                         // @NotBlank
  specialty?: string | null;
  degree?: EducationDegree | null;
  educationForm?: EducationForm | null;
  startYear?: number | null;                   // 1900..2100
  graduationYear?: number | null;              // 1900..2100
}

interface StudentEducationResponse {
  id: number;
  institution: string;
  specialty: string | null;
  degree: string | null;                       // enum name as string
  educationForm: string | null;                // enum name as string
  startYear: number | null;
  graduationYear: number | null;
}

interface StudentLanguageRequest {
  languageId: number;                          // @NotNull, FK → /dictionaries/languages
  level: LanguageLevel;                        // @NotNull
}

interface StudentLanguageResponse {
  id: number;
  language: LanguageResponse;                  // развёрнутый объект
  level: string;                               // enum name as string
}

interface StudentSkillRequest {
  skillId: number;                             // @NotNull, FK → /dictionaries/skills
  level: SkillLevel;                           // @NotNull
}

interface StudentSkillResponse {
  id: number;
  skill: SkillResponse;                        // развёрнутый объект
  level: string;                               // enum name as string
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Mentor Profile
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface MentorProfileRequest {
  firstName: string;                           // @NotBlank
  lastName: string;                            // @NotBlank
  middleName?: string | null;
  position?: string | null;
  department?: string | null;
  cityId?: number | null;                      // FK → /dictionaries/cities
  phone?: string | null;
  max?: string | null;
  description?: string | null;
  expectations?: string | null;
  canHelpWith?: string | null;
  mentoringType?: MentoringType | null;
  mentoringChannel?: MentoringChannel | null;
  mentoringFrequency?: string | null;          // свободный текст
  mentoringDuration?: MentoringDuration | null;
  menteeLimit?: number | null;                 // 0..100
  recruitmentStatus?: RecruitmentStatus | null; // null → "OPEN"
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
  mentoringType: string | null;                // enum name as string
  mentoringChannel: string | null;             // enum name as string
  mentoringFrequency: string | null;
  mentoringDuration: string | null;            // enum name as string
  menteeLimit: number | null;
  recruitmentStatus: string;                   // NOT NULL, default "OPEN"
  skills: MentorSkillResponse[];
}

interface MentorSkillRequest {
  skillId: number;                             // @NotNull, FK → /dictionaries/skills
  level: SkillLevel;                           // @NotNull
}

interface MentorSkillResponse {
  id: number;
  skill: SkillResponse;                        // развёрнутый объект
  level: string;                               // enum name as string
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Files
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface FileUploadResponse {
  id: number;
  originalFilename: string;
  contentType: string;                         // MIME type
  size: number;                                // bytes
  fileType: FileType;                          // "RESUME" | "PORTFOLIO" | "AVATAR"
  uploadedAt: string;                          // ISO 8601 OffsetDateTime
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

type FileType = 'RESUME' | 'PORTFOLIO' | 'ATTACHMENT' | 'AVATAR';
```

---

## 6. Enum-справочник

Все enum-значения, которые фронтенд должен знать. Передаются в request body как строки (UPPER_SNAKE_CASE). В response приходят в том же формате.

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
| `A1` | A1 |
| `A2` | A2 |
| `B1` | B1 |
| `B2` | B2 |
| `C1` | C1 |
| `C2` | C2 |
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
| `ATTACHMENT` | любой | без ограничений | Вложение |

### UserStatus

| Значение | Описание |
|---|---|
| `ACTIVE` | Активный аккаунт |
| `EMAIL_NOT_CONFIRMED` | Email не подтверждён |
| `BLOCKED` | Заблокирован |
| `DELETED` | Удалён (soft delete) |

---

## 7. Загрузка файлов

### Общие правила

1. HTTP метод: `POST`
2. Content-Type: `multipart/form-data` (browser ставит автоматически при `FormData`)
3. Имя поля: **`file`** (именно это имя, не `document`, не `upload`)
4. Ответ: `201 Created` с `FileUploadResponse`
5. **Не ставить `Content-Type: application/json`** — browser сам поставит `multipart/form-data` с boundary

### Сводная таблица

| Эндпоинт | Типы файлов | Max размер | Требует профиль | Привязка |
|---|---|---|---|---|
| `POST /files/resume` | PDF | 5 MB | Студент | → `studentProfile.resumeFileId` |
| `POST /files/portfolio` | PDF, JPEG, PNG | 10 MB | Нет | Хранится отдельно |
| `POST /files/avatar` | JPEG, PNG, WebP | 2 MB | Нет | → `user` |

### Пример: загрузка аватара (React)

```tsx
async function uploadAvatar(file: File, token: string): Promise<FileUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE_URL}/files/avatar`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      // НЕ добавлять Content-Type!
    },
    body: formData,
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw error;
  }

  return response.json();
}
```

### Пример: загрузка резюме с валидацией на клиенте

```tsx
function validateResume(file: File): string | null {
  if (file.type !== 'application/pdf') {
    return 'Резюме должно быть в формате PDF';
  }
  if (file.size > 5 * 1024 * 1024) {
    return 'Размер резюме не должен превышать 5 МБ';
  }
  return null;
}

async function uploadResume(file: File, token: string): Promise<FileUploadResponse> {
  const validationError = validateResume(file);
  if (validationError) {
    throw new Error(validationError);
  }

  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE_URL}/files/resume`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw error;
  }

  return response.json();
}
```

### Валидация на клиенте (рекомендация)

Сервер всё равно провалидирует, но клиентская валидация улучшает UX:

```typescript
const FILE_CONSTRAINTS: Record<string, { accept: string; maxSize: number }> = {
  resume:    { accept: '.pdf',                    maxSize: 5 * 1024 * 1024  },
  portfolio: { accept: '.pdf,.jpg,.jpeg,.png',    maxSize: 10 * 1024 * 1024 },
  avatar:    { accept: '.jpg,.jpeg,.png,.webp',   maxSize: 2 * 1024 * 1024  },
};
```

```html
<!-- HTML input с ограничениями -->
<input type="file" accept=".pdf" />               <!-- resume -->
<input type="file" accept=".pdf,.jpg,.jpeg,.png" /> <!-- portfolio -->
<input type="file" accept=".jpg,.jpeg,.png,.webp" /> <!-- avatar -->
```

---

## 8. Типичные сценарии (User Flows)

### Сценарий 1: Регистрация и создание профиля студента

```
1. POST /auth/register
   → { id, email, roles: ["STUDENT"] }

2. POST /auth/login
   → { accessToken: "eyJ...", tokenType: "Bearer", user: {...} }
   Сохранить accessToken

3. GET /profile/me
   → { role: "STUDENT", profileId: null, profileExists: false }
   → Показать форму создания профиля

4. GET /dictionaries/cities        (параллельно)
   GET /dictionaries/skills        (параллельно)
   GET /dictionaries/languages     (параллельно)
   → Заполнить select-ы в форме

5. PUT /profile/student
   → { id: 42, firstName: "Иван", ..., resumeFileId: null }
   → Профиль создан, перенаправить на дашборд

6. POST /files/avatar   (опционально)
   → { id: 100, fileType: "AVATAR", ... }

7. POST /files/resume   (опционально)
   → { id: 101, fileType: "RESUME", ... }
   → В профиле появится resumeFileId: 101
```

### Сценарий 2: Вход существующего ментора

```
1. POST /auth/login
   → { accessToken, user: { roles: ["MENTOR"], ... } }

2. GET /profile/me
   → { role: "MENTOR", profileId: 10, profileExists: true }
   → Перенаправить на дашборд ментора

3. GET /profile/mentor/me
   → Полные данные профиля для отображения
```

### Сценарий 3: Редактирование профиля студента

```
1. GET /profile/student/me
   → Текущие данные → заполнить форму

2. GET /dictionaries/cities        (параллельно)
   GET /dictionaries/skills        (параллельно)
   GET /dictionaries/languages     (параллельно)
   → Для select-ов

3. Пользователь редактирует форму

4. PUT /profile/student
   → Полный объект (все поля, даже неизменённые)
   → ВАЖНО: коллекции (educations, languages, skills) полностью заменяются
```

### Сценарий 4: Просмотр чужого профиля

```
1. GET /profiles/mentors/10
   → Публичные данные ментора

2. GET /profiles/students/42
   → Публичные данные студента
```

### Дерево решений после логина

```
POST /auth/login → сохранить token
        │
GET /profile/me
        │
        ├── role: "ADMIN"
        │   └── → Админ-панель (профиль не нужен)
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

## 9. Частые вопросы и подводные камни

### Q: Как понять роль пользователя?

**A:** Два источника:
1. `POST /auth/login` → `user.roles[]` — массив ролей сразу после логина
2. `GET /profile/me` → `role` — основная роль (одна строка), определяет тип профиля

Приоритет ролей (сервер возвращает наивысшую): `ADMIN` > `MENTOR` > `STUDENT`.

---

### Q: Что передавать в `cityId`, `languageId`, `skillId`?

**A:** ID из справочников. Загрузите справочники при старте приложения:
```typescript
const [cities, skills, languages] = await Promise.all([
  apiCall<CityResponse[]>('/dictionaries/cities'),
  apiCall<SkillResponse[]>('/dictionaries/skills'),
  apiCall<LanguageResponse[]>('/dictionaries/languages'),
]);
```
Используйте `id` из полученных объектов при отправке профиля.

---

### Q: Как работает upsert для профиля?

**A:** Один эндпоинт `PUT` для создания и обновления:
- Если профиль **не существует** → создаётся новый
- Если профиль **существует** → все поля перезаписываются

**Коллекции полностью заменяются:** если в `educations` передать 2 записи — старые удаляются, новые создаются. Если передать `null` или `[]` — все удаляются.

---

### Q: Что если передать null в необязательное поле?

**A:** Поле сохранится как `null` в базе (очистится). Исключение: `recruitmentStatus` у ментора — при `null` сбрасывается к `"OPEN"`.

---

### Q: Как отличить "профиль не создан" от "профиль пустой"?

**A:** `GET /profile/me` → `profileExists: false` — профиля нет вообще. `GET /profile/student/me` вернёт 404. После `PUT /profile/student` → `profileExists: true`, профиль существует (даже с минимальными данными — только firstName + lastName).

---

### Q: Почему в ответе `employmentTypes` — массив строк, а не массив enum?

**A:** Сервер конвертирует enum → `Enum.name()`. В JSON это строки: `["FULL_TIME", "REMOTE"]`.
При отправке на сервер используйте те же строки: `"FULL_TIME"`, не `"Full-time"`.

---

### Q: Можно ли загрузить резюме без профиля студента?

**A:** Нет. `POST /files/resume` вернёт `404 Not Found` с сообщением "Профиль студента не найден". Сначала создайте профиль через `PUT /profile/student`.

---

### Q: Как привязать аватар к профилю?

**A:** `POST /files/avatar` автоматически привязывает аватар к пользователю (user). Повторный вызов перезаписывает привязку.

---

### Q: Какой формат даты использовать?

**A:**
- `availableFrom` — ISO date: `"2025-04-01"` (LocalDate)
- `uploadedAt` в ответах — ISO datetime с timezone: `"2025-03-19T14:30:00+03:00"` (OffsetDateTime)
- `timestamp` в ошибках — ISO instant: `"2025-03-19T11:30:00.123Z"` (Instant / UTC)

---

### Q: Как парсить details в ошибке валидации?

**A:** Формат: `"fieldName: error message"`. Разделитель — первый `: `.

```typescript
function parseValidationDetails(details: string[]): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const detail of details) {
    const colonIndex = detail.indexOf(': ');
    if (colonIndex !== -1) {
      const field = detail.substring(0, colonIndex);
      const message = detail.substring(colonIndex + 2);
      errors[field] = message;
    }
  }
  return errors;
}

// Пример:
// parseValidationDetails(["email: Некорректный формат email", "password: Пароль должен содержать не менее 8 символов"])
// → { email: "Некорректный формат email", password: "Пароль должен содержать не менее 8 символов" }
```

---

### Q: Максимальный размер JSON body?

**A:** Spring Boot default — нет жёсткого лимита на JSON body. Для файлов — global `max-request-size: 11MB` (включает multipart overhead).

---

### Q: Как выглядит 401 от Spring Security?

**A:** Кастомный JSON (не HTML):
```json
{
  "timestamp": "2025-03-19T11:30:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Необходима авторизация",
  "path": "/profile/me"
}
```

---

### Q: Есть ли пагинация в списках?

**A:** Нет. Справочники и профили возвращаются целиком. Пагинация не реализована в текущей версии.
