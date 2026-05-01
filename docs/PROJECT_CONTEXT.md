# IT Mentor Project Context

Этот документ нужен как быстрый вход в проект для следующих сессий работы. Перед изменениями в приложении сначала читать его, затем профильные README и дизайн-доки.

## Кратко

`IT Mentor` - платформа для менторинга между студентами и IT-менторами. Пользовательские сценарии: регистрация и вход, заполнение профиля, каталог менторов и студентов, заявки на менторство, чат, файлы, отзывы и администрирование.

Рабочая директория проекта:

```text
/Users/yar_shpep/Documents/УЧЕБА/Практика/it_mentor
```

Монорепозиторий состоит из:

```text
it_mentor/
├── backend/   Spring Boot API
├── frontend/  Vue 3 + Conductor UI
├── docs/      рабочий контекст проекта
└── docker-compose.yml
```

Рядом с проектом в папке `Практика` лежат организационные и дизайн-материалы: ТЗ, пользовательский путь, модель данных, API-описание, блок-схемы, экспорт Google Stitch и промпт для Stitch.

## Локальные сервисы

Обычный локальный запуск:

```bash
cd /Users/yar_shpep/Documents/УЧЕБА/Практика/it_mentor
docker compose up -d postgres minio mailhog

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd ../frontend
npm run dev
```

Адреса:

- Frontend: `http://127.0.0.1:5173/`
- Backend API: `http://127.0.0.1:8080`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- MailHog: `http://127.0.0.1:8025`
- MinIO UI: `http://127.0.0.1:9001`

Тестовый пользователь, созданный для проверки экранов за логином:

```text
email: student.demo@example.com
password: DemoPass123
role: STUDENT
```

Если сервисы уже подняты, не останавливать их без явной просьбы пользователя.

## Backend

Путь: `backend/`

Стек:

- Java 25
- Spring Boot 4
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 15
- Liquibase
- MinIO
- MailHog для локальной почты
- Swagger через springdoc-openapi

Основные зоны кода:

- `controller/` - HTTP API
- `service/` - бизнес-логика
- `repository/` - JPA доступ к данным
- `mapper/` - MapStruct DTO mapping
- `security/` - JWT, filters, handlers
- `exception/` - API errors
- `resources/db/changelog/` - миграции Liquibase

Backend README содержит подробный API-контракт. Фронт ходит к backend через Vite proxy: браузер обращается к `/api/*`, Vite проксирует на `http://localhost:8080/*`.

## Frontend

Путь: `frontend/`

Стек:

- Vue 3
- TypeScript
- Pinia
- Vue Router
- Vite
- SCSS
- `conductor@2.2.0` как UI-kit

Команды:

```bash
npm run dev
npm run typecheck
npm run build
```

Структура:

- `src/app/` - router и layout
- `src/pages/` - страницы приложения
- `src/features/*/api` - API-клиенты по предметным зонам
- `src/features/*/model` - Pinia stores и состояние
- `src/shared/api` - HTTP-клиент, query helpers, contracts
- `src/shared/lib` - presenters, options, token storage, parsing
- `src/shared/ui` - локальные shared-компоненты
- `src/styles/conductor-lite.css` - восстановленный utility/reference слой Conductor
- `src/style.scss` - основной app-level стиль поверх Conductor

Маршруты находятся в `frontend/src/app/router/index.ts`:

- `/` - главная / dashboard
- `/auth` - вход, регистрация, восстановление
- `/profile` - профиль
- `/mentors` - каталог менторов
- `/students` - каталог студентов
- `/requests` - заявки
- `/chat` - чаты
- `/files` - файлы
- `/reviews` - отзывы
- `/admin` - администрирование

`App.vue` оборачивает приложение в `ThemeProvider` из Conductor и при mount запускает `authStore.initialize()` и `dictionariesStore.loadAll()`.

## Дизайн-Система

Главный приоритет при UI-правках:

1. Использовать компоненты Conductor UI-kit.
2. Соблюдать документы о дизайне и экспорт Stitch.
3. Только потом добавлять локальные app-level стили.

Локальные дизайн-доки:

- `frontend/docs/design-system/README.md`
- `frontend/docs/design-system/colors.md`
- `frontend/docs/design-system/foundations.md`
- `../stitch_mentor_student_workspace_dashboard/architectural_logic/DESIGN.md`
- `../google-stitch-it-mentor-prompt.md`
- `../Приложение 7 - Проект интерфейса (frontend).md`

Подтвержденная база Conductor:

- шрифт: `Open Sans`
- основной spacing module: `8px`
- radius scale: `4px`, `8px`, `16px`, `24px`, round
- control heights: `24px`, `32px`, `40px`, `48px`, `56px`
- темы: `default-light`, `default-dark`, также есть light-варианты `ocrv-light`, `grv-light`
- основные палитры: Primary, Gray, Green, Yellow, Red, Blue, Purple, Attention

Практические правила UI:

- Карточки не делать чрезмерно рыхлыми; рабочие экраны должны быть плотными и сканируемыми.
- Сохранять единую сетку ширин внутри форм и фильтров.
- Основной текст контролов держать около `13px`; заголовки секций не должны конкурировать с кнопками.
- Кнопки вторичных действий делать визуально легче основных действий.
- Для light и dark theme проверять контраст отдельно.
- Не использовать декоративные градиентные пятна и маркетинговые hero-паттерны для рабочих экранов.
- Для моковых данных ставить небольшой бейдж `Демо`, не ломая сетку и визуальную иерархию.

## Текущие UI-Договоренности

Приложение было перестроено из инженерного workbench в продуктовый dashboard-shell:

- слева sidebar с навигацией;
- сверху topbar с поиском, переключением меню и темой;
- страницы используют `app-panel`, `workspace-header`, `section-title`, `section-kicker`, карточки и таблицы;
- данные backend используются при наличии ответа API;
- fallback/mock данные отображаются только как демонстрационные и должны быть помечены бейджем `Демо`;
- светлая и темная темы должны выглядеть равноценно, а не как побочный режим.

Важно: если Conductor-компонент показывает техническое состояние вроде `Ошибка в props`, это не должно попадать в UI как нормальное значение. Нужно исправлять входные props или давать безопасный fallback.

## Моковые Данные

В проекте есть демо-данные для пустых или еще не наполненных backend-ответов. Их задача - показать интерфейс, когда API доступен, но реальных сущностей мало.

Правило: вся моковая информация должна быть помечена небольшим бейджем `Демо`.

Бейдж не должен:

- перетягивать внимание с основного контента;
- менять высоту строк таблицы;
- ломать выравнивание карточек;
- выглядеть как статус backend-сущности.

## Последняя Активная Область Правок

Последние пользовательские замечания касались страницы `Каталог` (`frontend/src/pages/MentorsPage.vue`) и стилей фильтра:

- слишком большой внутренний отступ карточки фильтров;
- разная визуальная сетка у полей, кнопок и нижнего блока;
- кнопка `Сбросить` слишком тяжелая и плохо выровнена;
- поиск выше селектов, placeholder слишком крупный;
- чекбоксы крупные, с большим gap и разной логикой active/inactive;
- `Ошибка в props` в селектах не должна выглядеть как выбранное значение;
- labels слишком близко к controls;
- primary-кнопка фильтров слишком массивная;
- `Найдено менторов: 4` должен выглядеть как информационный summary/badge, а не как disabled input;
- `Демо` должен быть связан с summary, но оставаться легким;
- нужно уменьшить количество спорящих размеров текста;
- рамка карточки не должна перегружать интерфейс.

В `MentorsPage.vue` уже начата нормализация:

- импорт оставлен на `BaseButton`, `BaseInput`, `BaseSelect`;
- для пустых `cityOptions` и `skillOptions` добавлен fallback `Список загружается`;
- панели фильтра добавлены классы `catalog-filter-panel*`;
- поиск и селекты переведены на `size="s"`;
- нижний статус заменен на `catalog-filter-panel__summary` с бейджем `Демо`.

Следующий шаг по этой области - проверить и довести CSS в `frontend/src/style.scss`, затем выполнить `npm run build`.

## Как Работать Дальше

Перед любыми UI-правками:

1. Прочитать этот файл.
2. Проверить `frontend/docs/design-system/*`.
3. Открыть конкретную страницу в `frontend/src/pages`.
4. Найти уже существующие классы в `frontend/src/style.scss`, не плодить параллельную систему.
5. Если правка касается компонентов Conductor, сначала проверить их props и размеры.
6. После изменений запускать `npm run build`; для визуальных правок желательно проверять через браузер на light и dark theme.

Перед backend-правками:

1. Проверить `backend/README.md`.
2. Найти controller/service/repository для нужной области.
3. Не менять API-контракт без необходимости, потому что frontend типизирован под `frontend/src/shared/api/contracts.ts`.
4. После изменений запускать релевантные Maven tests или хотя бы сборку.

## Что Не Терять

- Приоритет дизайна: UI-kit Conductor и локальные дизайн-доки важнее случайных решений из сгенерированного Stitch export.
- Приложение должно быть рабочим за логином, а не только набором статичных экранов.
- Темная и светлая темы равноправны.
- Моковые данные допустимы, но только явно помеченные как `Демо`.
- Пользователь часто дает точечный визуальный feedback; нужно исправлять конкретные элементы, не перепридумывая весь экран без причины.
