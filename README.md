# IT Mentor Monorepo

Монорепозиторий проекта IT Mentor с раздельными зонами backend и frontend.

## Структура

```text
.
├── backend/           Spring Boot backend
├── frontend/          frontend-зона проекта
├── docker-compose.yml локальная инфраструктура
└── README.md
```

## Папки

- `backend/` содержит Java/Spring Boot приложение, исходный код, Maven-конфигурацию и backend-документацию.
- `frontend/` выделена под клиентское приложение. Сейчас в ней лежит интеграционная документация API.
- `docker-compose.yml` в корне поднимает локальную инфраструктуру для разработки.

## Быстрый старт backend

```bash
docker compose up -d
cd backend
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Документация

- Project context for future work: `docs/PROJECT_CONTEXT.md`
- Backend TODO for frontend gaps: `README-BACKEND.md`
- Backend: `backend/README.md`
- Frontend API guide: `frontend/README.md`
