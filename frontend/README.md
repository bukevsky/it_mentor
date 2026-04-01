# IT Mentor Frontend

Frontend-часть проекта IT Mentor на `Vue 3`, `TypeScript`, `Pinia` и `Vite`.

Сейчас фронт собран как typed workbench под полный контракт из [backend/README_FRONT.md](/Users/yar_shpep/Documents/УЧЕБА/Практика/it_mentor/backend/README_FRONT.md). Backend не меняется и используется только как read-only API.

## Стек

- `Node.js 20+`
- `TypeScript 5.3`
- `Vue 3`
- `Pinia`
- `Vite`
- `SCSS`

## Что покрыто

Реализованы API-модули и обзорные экраны для всех разделов guide:

- `Auth`: register, login, me, forgot/reset password, health
- `Dictionaries`: cities, skills, languages, interaction types
- `Profile Summary`: `GET /profile/me`
- `Student Profile`: upsert, my profile, public by id
- `Mentor Profile`: upsert, my profile, public by id
- `Mentor Search`: `GET /profiles/mentors`
- `Files`: resume, portfolio, avatar, chat attachment
- `Mentoring Requests`: create, list, by id, view, clarify, accept, reject, cancel, complete
- `Chat`: list, by id, by request id, messages, send message
- `Admin`: assign role

## Структура

```text
frontend/
├── src/
│   ├── entities/
│   │   └── user/
│   ├── features/
│   │   ├── admin/
│   │   ├── auth/
│   │   ├── chat/
│   │   ├── dictionaries/
│   │   ├── files/
│   │   ├── mentor-profile/
│   │   ├── mentoring/
│   │   ├── profile-summary/
│   │   └── student-profile/
│   ├── shared/
│   │   ├── api/
│   │   ├── config/
│   │   ├── lib/
│   │   └── ui/
│   ├── widgets/
│   │   └── api-workbench/
│   ├── App.vue
│   ├── main.ts
│   └── style.scss
├── index.html
├── package.json
├── tsconfig*.json
└── vite.config.ts
```

## Архитектура

- `shared/api` хранит DTO, query helpers и HTTP-клиент.
- `shared/lib` содержит инфраструктурные утилиты: парсинг JSON, нормализацию ошибок, token storage.
- `features/*/api` изолируют вызовы backend по предметным зонам.
- `features/*/ui` показывают отдельные workbench-секции для конкретных эндпоинтов.
- `widgets/api-workbench` собирает единый shell приложения.

Такой расклад держит интеграцию типизированной и не смешивает UI с деталями transport layer.

## Переменные окружения

Пример лежит в [`.env.example`](/Users/yar_shpep/Documents/УЧЕБА/Практика/it_mentor/frontend/.env.example).

Поддерживаются:

- `VITE_API_BASE_URL` — базовый путь для запросов из браузера, по умолчанию `/api`
- `VITE_DEV_PROXY_TARGET` — адрес backend для локального proxy, по умолчанию `http://localhost:8080`

Для локальной разработки можно создать `.env.local`:

```env
VITE_API_BASE_URL=/api
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

## Запуск

Установка зависимостей:

```bash
npm install
```

Запуск dev-сервера:

```bash
npm run dev
```

Проверка типов:

```bash
npm run typecheck
```

Production build:

```bash
npm run build
```

## Интеграция с Backend

Во время локальной разработки фронт ходит в backend через Vite proxy:

```text
Frontend browser -> /api/* -> Vite proxy -> http://localhost:8080/*
```

JWT сохраняется на фронте в `sessionStorage` через `tokenStorage`.

## Ограничения

- Это не финальный продуктовый UI, а инженерный workbench для полного покрытия backend-контракта.
- Загрузка файлов зависит от работающего `MinIO`.
- Если `actuator/health` возвращает `503`, часть API всё ещё может быть доступна.
- UI-kit пока не подключён, потому что доступ к внешнему приватному репозиторию не выдан.
