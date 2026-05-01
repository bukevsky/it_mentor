# Backend Notes For Frontend

## Dashboard

На странице Dashboard фронт уже использует всё, что можно собрать из текущего backend API без изменения контракта:

- `GET /auth/me` - email, роли, статус аккаунта.
- `GET /profile/me` - роль, наличие профиля, id профиля.
- `GET /profile/student/me` - данные профиля студента для расчёта заполненности.
- `GET /profile/mentor/me` - данные профиля ментора для расчёта заполненности.
- `GET /mentoring/requests` - последние заявки и общее количество.
- `GET /mentoring/requests?status=...` - счётчики заявок по статусам.
- `GET /chats` - количество и список последних чатов.
- `GET /chats/{chatId}/messages` - последнее сообщение по чату.
- `GET /profiles/mentors` - количество доступных менторов для студента.

## TODO

- Добавить агрегированный endpoint `GET /dashboard/summary`.
  Он должен возвращать готовые счётчики для текущей роли: заявки всего, активные заявки, ожидающие ответа, завершённые заявки, чаты, непрочитанные сообщения, заполненность профиля, ближайшая сессия.

- Добавить `unread`-модель для чатов.
  Сейчас можно получить количество чатов, но нельзя корректно показать “новые сообщения”, потому что нет признака прочитанности и общего счётчика непрочитанных сообщений.

- Расширить `GET /chats` данными для списка диалогов.
  Сейчас endpoint возвращает только `id`, `mentoringRequestId`, `studentUserId`, `mentorUserId`, `createdAt`. Фронт страницы “Чаты” вынужден для каждого диалога дополнительно вызывать `GET /chats/{chatId}/messages?size=1`, чтобы показать preview и время последней активности. Нужны поля в `ChatResponse`: `lastMessage`, `lastMessageAt`, `lastSenderUserId`, `unreadCount`, а также краткие данные участников (`studentName`, `mentorName`) и, желательно, статус связанной заявки.

- Добавить WebSocket или SSE для realtime-чата.
  Сейчас frontend использует mock WebSocket: REST остаётся только для загрузки списка и истории. Нужен backend-канал с подключением по JWT, подпиской на конкретный чат, событием нового сообщения, событием обновления списка диалогов, reconnect-friendly контрактом и проверкой, что пользователь является участником чата.

- Добавить server-side ack для отправки сообщений.
  Для optimistic UI фронт сразу показывает сообщение со статусом `sending`, но backend должен возвращать/пушить финальное событие с настоящим `messageId`, `createdAt`, `sent` или ошибкой доставки. Это нужно, чтобы не держать mock-логику подтверждения на клиенте.

- Добавить отметку чата прочитанным.
  Нужен endpoint вроде `POST /chats/{chatId}/read` или `PUT /chats/{chatId}/read-state`, который сохраняет `lastReadMessageId` или `lastReadAt` для пары пользователь-чат. Без этого `unreadCount` нельзя считать корректно между сессиями и устройствами.

- Сортировать `GET /chats` по последней активности.
  Для мессенджера список диалогов должен идти по `lastMessageAt DESC`, а не только по `createdAt DESC`, иначе старый чат с новым сообщением не поднимется наверх.

- Улучшить пагинацию истории сообщений.
  Для длинных чатов лучше добавить cursor/keyset-параметры `beforeMessageId` или `beforeCreatedAt` + `limit`. Обычная page-пагинация нестабильна, когда новые сообщения приходят в realtime.

- Доработать вложения в чатах.
  Сейчас есть загрузка `/files/chat-attachment` и поле `attachmentFileId`, но для полноценного UI нужны ограничения размера/типов, проверка доступа к файлу, download/open endpoint или временный URL, а также удобный контракт отправки вложения без ручного ввода ID на фронте.

- Добавить typing/presence события, если нужен Telegram-like UX.
  Минимальный набор: `typing_started`, `typing_stopped`, `online/offline` или `lastSeen`. Это необязательно для MVP, но нужно для ощущения настоящего мессенджера.

- Добавить единый endpoint активности `GET /dashboard/activity`.
  Сейчас Dashboard собирает активность из заявок и чатов несколькими запросами. Лучше отдавать нормализованную ленту: тип события, заголовок, описание, дата, ссылка на сущность.

- Добавить backend-расчёт “Уровень менторства”.
  Сейчас нет доменной модели уровня, рейтинга, прогресса до следующего уровня и правил расчёта. Нужен endpoint вроде `GET /mentor-stats/me`.

- Добавить API отзывов и рейтинга.
  Для Dashboard нужны: средний рейтинг, количество отзывов, последние отзывы, динамика по периоду.

- Добавить SLA/response-rate агрегаты.
  Показатель “Отклики” сейчас демо. Нужен расчёт скорости ответа по заявкам и чатам: процент ответов в целевое время, среднее время ответа.

- Добавить поиск/listing студентов для роли `MENTOR`.
  Сейчас есть `GET /profiles/students/{id}`, но нет `GET /profiles/students` с фильтрами и пагинацией. Из-за этого нельзя показать количество доступных студентов и полноценный каталог студентов с backend.
  Фронт страницы “Студенты” уже ожидает такой контракт:

  ```http
  GET /profiles/students?q=&cityId=&skillIds=&employmentType=&workFormat=&page=0&size=24&sort=createdAt,desc
  Authorization: Bearer <token>
  ```

  Ожидаемый ответ - стандартный `PagedResponse<StudentProfileResponse>`:

  ```json
  {
    "content": [
      {
        "id": 1,
        "userId": 10,
        "firstName": "Анна",
        "lastName": "Смирнова",
        "middleName": null,
        "phone": null,
        "city": { "id": 1, "name": "Москва", "region": "Москва", "country": "Россия" },
        "desiredPosition": "Frontend Developer",
        "hoursPerWeek": 20,
        "availableFrom": "2026-05-01",
        "about": "Хочу развиваться во frontend",
        "max": null,
        "employmentTypes": ["INTERNSHIP"],
        "workFormats": ["REMOTE"],
        "educations": [],
        "languages": [],
        "skills": [
          {
            "id": 1,
            "skill": { "id": 5, "name": "TypeScript", "category": "Frontend" },
            "level": "INTERMEDIATE"
          }
        ],
        "resumeFileId": null
      }
    ],
    "page": 0,
    "size": 24,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
  ```

  Фильтры должны работать так:
  `q` ищет по имени, фамилии, желаемой позиции и навыкам; `cityId` фильтрует по городу; `skillIds` принимает один или несколько id навыков; `employmentType` и `workFormat` фильтруют по массивам профиля студента. Доступ нужен минимум для роли `MENTOR`.

- Доработать backend под продуктовый UX профиля студента.
  Новая страница профиля разбита на секции и работает с preview, чеклистом заполненности и несколькими навыками/языками. Текущий API уже принимает массивы `skills` и `languages`, но для полноценного UX нужны дополнительные контракты и гарантии:

  ```http
  GET /profile/student/me/completion
  PATCH /profile/student
  PUT /profile/student/skills
  PUT /profile/student/languages
  ```

  `GET /profile/student/me/completion` должен возвращать backend-расчёт заполненности и actionable checklist, чтобы правила не дублировались на фронте:

  ```json
  {
    "percent": 67,
    "items": [
      { "code": "MAIN", "label": "Заполнить имя, город и позицию", "done": true },
      { "code": "ABOUT", "label": "Добавить описание и цели", "done": false },
      { "code": "SKILLS", "label": "Добавить минимум 3 навыка", "done": false },
      { "code": "RESUME", "label": "Загрузить резюме", "done": true }
    ]
  }
  ```

  `PATCH /profile/student` нужен для сохранения отдельных секций без отправки всей анкеты. Это позволит сделать autosave/debounce и статус “Сохранено / Есть изменения” честным, а не только frontend-состоянием.

  `PUT /profile/student/skills` и `PUT /profile/student/languages` нужны как явные контракты для управления списками. Желательно поддержать несколько навыков и языков, порядок, уникальность по `skillId`/`languageId` и уровень для каждого элемента.

- Связать профиль студента с файловым менеджером.
  Для секции “Файлы” профиля нужен backend-ответ с актуальным резюме и, желательно, счетчиками портфолио:

  ```json
  {
    "resumeFile": {
      "id": 801,
      "originalFilename": "resume.pdf",
      "uploadedAt": "2026-04-28T17:00:00Z"
    },
    "portfolioCount": 3,
    "avatarFileId": 900
  }
  ```

  Сейчас `resumeFileId` есть в `StudentProfileResponse`, но фронту для нормального preview нужны хотя бы имя файла, дата загрузки и статус.

- Добавить admin dashboard endpoints.
  Для роли `ADMIN` сейчас есть только `PUT /admin/users/{userId}/role`. Нужны `GET /admin/users`, статистика пользователей по ролям/статусам, жалобы, модерация отзывов и аудит действий.

- Добавить API списка файлов текущего пользователя.
  Сейчас есть загрузка файлов, но нет endpoint’а для получения последних загруженных файлов и их количества.

- Добавить полноценный файловый менеджер API.
  Новая страница “Файлы” на фронте работает как единый менеджер, но backend сейчас поддерживает только загрузку. Нужны контракты:

  ```http
  GET /files?type=&page=0&size=50&sort=uploadedAt,desc
  GET /files/{fileId}/download
  DELETE /files/{fileId}
  PUT /files/{fileId}/replace
  ```

  `GET /files` должен возвращать `PagedResponse<FileResponse>` для текущего пользователя:

  ```json
  {
    "content": [
      {
        "id": 801,
        "originalFilename": "resume.pdf",
        "contentType": "application/pdf",
        "size": 245760,
        "fileType": "RESUME",
        "status": "ACTIVE",
        "uploadedAt": "2026-04-28T17:00:00Z",
        "downloadUrl": "/files/801/download"
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
  ```

  Правила для типов:
  `RESUME` и `AVATAR` - по одному актуальному файлу, новая загрузка заменяет старый активный файл; `PORTFOLIO` и `CHAT_ATTACHMENT` - много файлов. Удаление лучше делать soft delete со статусом `DELETED`, чтобы не ломать ссылки из профиля и сообщений.

- Добавить preview/download metadata для файлов.
  Для аватара и изображений портфолио нужен `previewUrl` или отдельный endpoint thumbnail. Для всех файлов нужен безопасный download/open endpoint с проверкой доступа владельца или участника чата.

- Добавить endpoint ближайших сессий.
  В API есть заявки и чаты, но нет отдельной сущности календарной сессии. Для кнопки/виджета “Запланировать сессию” нужен контракт с датой, участниками и статусом.
