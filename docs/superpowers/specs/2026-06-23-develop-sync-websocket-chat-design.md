# Develop Sync And WebSocket Chat Design

## Goal

Bring the monorepo backend up to `origin/develop` and adapt the existing Vue frontend to the current STOMP WebSocket chat contract without changing unrelated product flows or UI.

## Repository Strategy

The local monorepo layout remains authoritative:

- `backend/` contains the backend currently stored at the root of `origin/develop`.
- `frontend/` contains the standalone frontend currently stored at the root of `origin/frontend`.

Backend synchronization is content-based rather than a Git merge because the branches use incompatible root layouts. The tracked backend tree is replaced with the exact application content from `origin/develop`, translated under `backend/`. Monorepo-owned root files and all frontend files remain untouched.

## Backend Scope

Import the current `origin/develop` backend, including:

- Spring STOMP endpoint at `/ws`.
- JWT authentication during STOMP `CONNECT`.
- User event subscription at `/user/queue/chat-events`.
- Chat commands under `/app/chats/{chatId}/...`.
- Message delivery state persistence and Liquibase migration `032`.
- The associated backend tests, documentation, and Maven dependencies.

No local backend-only behavior is preserved because the current `backend/` tree exactly matches the older `develop` commit `869fdcb`; the new remote tree is its direct replacement.

## Frontend Architecture

Replace the SSE adapter with a focused STOMP adapter built on `@stomp/stompjs`. The adapter owns connection setup, JWT `CONNECT` headers, heartbeat/reconnect behavior, the user event subscription, and command publication. It exposes typed events and commands to the existing chat composable rather than leaking STOMP frames into stores or Vue components.

The HTTP chat API remains responsible only for chat lists, chat details, paginated history, cursor history, and reconnect synchronization. Message send, delivered/read acknowledgements, and typing move entirely to STOMP.

## Data Flow

1. `ChatPage` starts the chat connection after authentication.
2. The adapter connects to `/ws` with `Authorization: Bearer <token>` and subscribes to `/user/queue/chat-events`.
3. Sending creates a UUID `requestId` and optimistic message keyed by that ID.
4. The frontend publishes to `/app/chats/{chatId}/messages/send`.
5. `chat.message.created` replaces the matching optimistic message using `requestId` and `clientMessageId`.
6. Incoming messages are rendered and acknowledged with `/messages/delivered`.
7. Opening or viewing a chat publishes `/messages/read` with the highest visible incoming message ID.
8. Typing commands publish to `/typing`; incoming typing and presence events update transient UI state.
9. After reconnect, `/messages/sync?afterMessageId=...` fills any event gap before normal event processing continues.

## Contracts

Frontend contracts add the server fields `clientMessageId`, `deliveryStatus`, `deliveredAt`, and `readAt`. Server delivery states (`SENT`, `DELIVERED`, `READ`) remain distinct from local optimistic states (`sending`, `error`) to avoid case conversion and accidental state loss.

The event envelope is a discriminated union for:

- `chat.message.created`
- `chat.message.status.changed`
- `chat.typing`
- `presence.changed`
- `chat.command.ack`
- `chat.command.error`

Unknown event types are ignored without disconnecting the client. Malformed known events are reported through the adapter error callback and do not crash the page.

## Error Handling

- Connection failures use bounded exponential reconnect through the STOMP client.
- Missing JWT prevents connection and exposes the disconnected state.
- Command errors mark only the optimistic message associated with the returned `requestId` as failed.
- Duplicate message events are de-duplicated by server message ID and client message UUID.
- REST sync failure keeps existing messages visible and schedules the next reconnect attempt.

## Testing

Frontend behavior is developed test-first. Unit tests cover event parsing, destinations and payloads, optimistic reconciliation, status updates, read acknowledgements, and reconnect synchronization. Existing type checking, Vite build, and Playwright tests remain part of final verification.

Backend verification runs the imported Maven test suite. The import itself is not behaviorally rewritten locally; its source of truth is the tested `origin/develop` tree.

## Non-Goals

- Redesigning chat screens or unrelated pages.
- Changing authentication or REST APIs outside chat.
- Rewriting the backend WebSocket implementation.
- Merging the unrelated Git histories of `frontend`, `develop`, and the monorepo branch.
