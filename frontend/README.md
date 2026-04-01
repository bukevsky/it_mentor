# IT Mentor Frontend

Frontend-часть проекта IT Mentor на `Vue 3`, `TypeScript`, `Pinia` и `Vite`.

Сейчас в проекте собран базовый клиентский каркас для интеграции с backend:
- общий HTTP-клиент;
- auth API-слой;
- auth store на Pinia;
- стартовый экран для проверки связки с backend;
- proxy для локальной разработки через Vite.

## Стек

- `Node.js 20+`
- `TypeScript 5.3`
- `Vue 3`
- `Pinia`
- `Vite`
- `SCSS`

## Структура

```text
frontend/
├── src/
│   ├── entities/
│   │   └── user/
│   ├── features/
│   │   └── auth/
│   ├── shared/
│   │   ├── api/
│   │   ├── config/
│   │   └── lib/
│   ├── App.vue
│   ├── main.ts
│   └── style.scss
├── index.html
├── package.json
├── tsconfig*.json
└── vite.config.ts
```

## Архитектура

Проект разделён по зонам ответственности:

- `entities` — базовые доменные типы;
- `features` — прикладные пользовательские сценарии;
- `shared/api` — HTTP-клиент, контракты и обработка ошибок;
- `shared/config` — конфигурация окружения;
- `shared/lib` — инфраструктурные утилиты, например хранение токена.

Такой расклад нужен, чтобы UI не зависел от деталей backend-реализации, а интеграция шла через типизированный API-слой.

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

Сейчас реализованы и проверены:

- `POST /auth/login`
- `GET /auth/me`
- `GET /actuator/health`

JWT сохраняется на фронте в `sessionStorage`.

## Что важно знать

- Backend в этой связке не меняется и рассматривается как внешний API.
- Если `actuator/health` возвращает `503`, это не всегда означает, что backend полностью недоступен.
  Например, API может работать, а общий health быть `DOWN` из-за почты или другой необязательной зависимости.
- Загрузка файлов требует работающего `MinIO`.

## Ближайшее развитие

- добавить `vue-router` и защищённые маршруты;
- вынести auth flow в отдельные страницы;
- расширить API-слой профилями, справочниками и поиском менторов;
- подключить UI-kit после получения доступа к библиотеке.
