# Develop Sync And WebSocket Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Synchronize `backend/` with current `origin/develop` and make the Vue frontend compatible with its STOMP WebSocket chat protocol.

**Architecture:** Keep the monorepo layout and import the remote backend root beneath `backend/`. The frontend keeps REST for chat queries and uses a typed `@stomp/stompjs` adapter for realtime commands/events; Pinia owns durable view state while the composable coordinates transport lifecycle.

**Tech Stack:** Java 25, Spring Boot 4, Maven, Liquibase, Vue 3, Pinia, TypeScript, Vite 5, `@stomp/stompjs` 7.3.0, Vitest 2.1.9, Playwright.

---

### Task 1: Import The Current Develop Backend

**Files:**
- Replace: `backend/**`
- Source: `origin/develop` repository root

- [ ] **Step 1: Capture a clean remote snapshot outside the repository**

Run:

```bash
snapshot=$(mktemp -d /tmp/it-mentor-develop.XXXXXX)
git archive --format=tar origin/develop -o "$snapshot/develop.tar"
mkdir "$snapshot/tree"
tar -xf "$snapshot/develop.tar" -C "$snapshot/tree"
```

Expected: `$snapshot/tree/pom.xml` and `$snapshot/tree/src/main/...` exist.

- [ ] **Step 2: Replace only the backend subtree**

Run:

```bash
rsync -a --delete --exclude target --exclude .idea "$snapshot/tree/" backend/
```

Expected: `backend/src/main/java/com/example/it/mentor/config/WebSocketConfig.java` and migration `032_chat_message_delivery_status.sql` exist; frontend and monorepo root files are unchanged.

- [ ] **Step 3: Verify the translated tree is exact**

Run:

```bash
diff -qr -x target -x .idea -x .DS_Store backend "$snapshot/tree"
```

Expected: no output and exit code 0.

- [ ] **Step 4: Run focused backend WebSocket tests**

Run:

```bash
cd backend
./mvnw -Dtest='ChatWebSocketIT,ChatRealtimeFacadeTest,ChatMessageStatusServiceTest' test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit the backend import**

```bash
git add backend
git commit -m "feat: sync backend with develop websocket chat"
```

### Task 2: Add Frontend Unit-Test And STOMP Dependencies

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/tsconfig.node.json`
- Create: `frontend/vitest.config.ts`

- [ ] **Step 1: Install compatible packages**

Run:

```bash
cd frontend
npm install @stomp/stompjs@7.3.0
npm install --save-dev vitest@2.1.9
```

Expected: package lock records both exact package families without changing Vue or Vite major versions.

- [ ] **Step 2: Add the unit test script**

Add to `frontend/package.json` scripts:

```json
"test:unit": "vitest run"
```

- [ ] **Step 3: Add the Vitest configuration**

Create `frontend/vitest.config.ts`:

```ts
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  test: {
    environment: "node",
    clearMocks: true,
    restoreMocks: true
  }
});
```

Add `vitest.config.ts` to `frontend/tsconfig.node.json` `include`.

- [ ] **Step 4: Verify the empty test suite starts**

Run:

```bash
npm run test:unit -- --passWithNoTests
```

Expected: Vitest exits successfully with no tests.

- [ ] **Step 5: Commit test infrastructure**

```bash
git add frontend/package.json frontend/package-lock.json frontend/tsconfig.node.json frontend/vitest.config.ts
git commit -m "test: add frontend unit test runner"
```

### Task 3: Define And Test The Realtime Protocol

**Files:**
- Modify: `frontend/src/shared/api/contracts.ts:611-618`
- Modify: `frontend/src/features/chat/api/chat-api.ts:20-27`
- Create: `frontend/src/features/chat/api/chat-protocol.ts`
- Create: `frontend/src/features/chat/api/chat-protocol.test.ts`
- Modify: `frontend/vite.config.ts:18-23`

- [ ] **Step 1: Write failing protocol tests**

Create `chat-protocol.test.ts` with tests that require these APIs:

```ts
import { describe, expect, it } from "vitest";
import {
  buildChatSocketUrl,
  chatDestination,
  parseChatEvent
} from "./chat-protocol";

describe("chat protocol", () => {
  it("builds websocket URLs for proxied and absolute API bases", () => {
    expect(buildChatSocketUrl("/api", "https://app.example")).toBe("wss://app.example/api/ws");
    expect(buildChatSocketUrl("https://api.example", "https://app.example")).toBe("wss://api.example/ws");
  });

  it("builds STOMP command destinations", () => {
    expect(chatDestination(7, "send")).toBe("/app/chats/7/messages/send");
    expect(chatDestination(7, "read")).toBe("/app/chats/7/messages/read");
    expect(chatDestination(7, "typing")).toBe("/app/chats/7/typing");
  });

  it("parses known envelopes and ignores unknown event types", () => {
    const known = parseChatEvent(JSON.stringify({
      type: "chat.typing",
      requestId: "00000000-0000-4000-8000-000000000001",
      chatId: 7,
      occurredAt: "2026-06-23T10:00:00Z",
      payload: { chatId: 7, userId: 2, typing: true }
    }));
    expect(known?.type).toBe("chat.typing");
    expect(parseChatEvent('{"type":"future.event"}')).toBeNull();
  });

  it("rejects malformed known events", () => {
    expect(() => parseChatEvent('{"type":"chat.message.created"}')).toThrow("Malformed chat event");
  });
});
```

- [ ] **Step 2: Run the test and verify RED**

Run: `cd frontend && npm run test:unit -- src/features/chat/api/chat-protocol.test.ts`

Expected: FAIL because `chat-protocol.ts` does not exist.

- [ ] **Step 3: Add command and event contracts**

Add the server delivery state and command/event union while leaving the existing `ChatMessageResponse` shape unchanged until its store migration in Task 5:

```ts
export type ChatMessageDeliveryStatus = "SENT" | "DELIVERED" | "READ";

export interface SendMessageCommand {
  requestId: string;
  body: string | null;
  attachmentFileId: number | null;
}

export interface MessageStatusCommand {
  requestId: string;
  upToMessageId: number;
}

export interface TypingCommand {
  requestId: string;
  typing: boolean;
}

export interface ChatCommandAckPayload {
  command: "send" | "delivered" | "read" | "typing";
  resourceId: number | null;
  duplicate: boolean;
}

export interface ChatCommandErrorPayload {
  code: "NOT_FOUND" | "FORBIDDEN" | "CONFLICT" | "BUSINESS_RULE_VIOLATION" | "VALIDATION_ERROR" | "INTERNAL_ERROR";
  message: string;
  fieldErrors: Record<string, string>;
}

export interface MessageStatusChangedPayload {
  actorUserId: number;
  upToMessageId: number;
  status: "DELIVERED" | "READ";
  changedAt: string;
  changedCount: number;
}

export interface ChatTypingPayload {
  chatId: number;
  userId: number;
  typing: boolean;
}

export interface PresenceChangedPayload {
  userId: number;
  status: "online" | "offline";
  lastSeenAt: string | null;
}

export type ChatEventEnvelope =
  | { type: "chat.command.ack"; requestId: string; chatId: number; occurredAt: string; payload: ChatCommandAckPayload }
  | { type: "chat.command.error"; requestId: string | null; chatId: number | null; occurredAt: string; payload: ChatCommandErrorPayload }
  | { type: "chat.message.created"; requestId: string; chatId: number; occurredAt: string; payload: ChatMessageResponse }
  | { type: "chat.message.status.changed"; requestId: string; chatId: number; occurredAt: string; payload: MessageStatusChangedPayload }
  | { type: "chat.typing"; requestId: string; chatId: number; occurredAt: string; payload: ChatTypingPayload }
  | { type: "presence.changed"; requestId: null; chatId: null; occurredAt: string; payload: PresenceChangedPayload };
```

Keep these definitions aligned with `origin/develop:README_FRONT.md`.

- [ ] **Step 4: Implement protocol helpers**

Create `chat-protocol.ts` with exported constants `CHAT_SOCKET_PATH = "/ws"`, `CHAT_EVENTS_DESTINATION = "/user/queue/chat-events"`, URL conversion, destination mapping, and runtime envelope validation. `parseChatEvent` must return `null` for unknown `type` values and throw `Error("Malformed chat event")` when a known type lacks `requestId`, `chatId`, `occurredAt`, or `payload`.

- [ ] **Step 5: Add reconnect synchronization to the REST API**

Keep the legacy mutation methods temporarily so this commit typechecks, and add:

```ts
syncMessages(chatId: number, afterMessageId: number, limit = 100) {
  return request<ChatMessageResponse[]>(
    `/chats/${chatId}/messages/sync${buildQuery({ afterMessageId, limit })}`
  );
}
```

Make `beforeMessageId` required in `getMessagesCursor`.

- [ ] **Step 6: Enable WebSocket proxying in development**

Add `ws: true` to the `/api` proxy in `frontend/vite.config.ts`; `/api/ws` will continue to be rewritten to backend `/ws`.

- [ ] **Step 7: Run protocol tests and typecheck**

Run:

```bash
npm run test:unit -- src/features/chat/api/chat-protocol.test.ts
npm run typecheck
```

Expected: protocol tests and typecheck PASS.

- [ ] **Step 8: Commit protocol contracts**

```bash
git add frontend/src/shared/api/contracts.ts frontend/src/features/chat/api/chat-api.ts frontend/src/features/chat/api/chat-protocol.ts frontend/src/features/chat/api/chat-protocol.test.ts frontend/vite.config.ts
git commit -m "feat: define websocket chat protocol"
```

### Task 4: Build The Tested STOMP Adapter

**Files:**
- Replace: `frontend/src/features/chat/api/chat-socket.ts`
- Create: `frontend/src/features/chat/api/chat-socket.test.ts`

- [ ] **Step 1: Write a failing adapter test with a fake client**

The fake client records constructor configuration, `subscribe`, and `publish`. Assert that `connectHeaders.Authorization` contains the token, connection subscribes once to `/user/queue/chat-events`, and sending chat 7 publishes:

```ts
{
  destination: "/app/chats/7/messages/send",
  body: JSON.stringify({ requestId, body: "hello", attachmentFileId: null })
}
```

Also assert malformed frames call `onError` without calling `onEvent`, and `disconnect()` calls `deactivate()`.

- [ ] **Step 2: Run the adapter test and verify RED**

Run: `npm run test:unit -- src/features/chat/api/chat-socket.test.ts`

Expected: FAIL because the current SSE adapter has no STOMP client factory or command methods.

- [ ] **Step 3: Implement the adapter port**

Export this public interface:

```ts
export interface ChatSocketAdapter {
  connect(): Promise<void>;
  disconnect(): Promise<void>;
  onConnect(handler: () => void): void;
  onDisconnect(handler: () => void): void;
  onEvent(handler: (event: ChatEventEnvelope) => void): void;
  onError(handler: (error: Error) => void): void;
  sendMessage(chatId: number, command: SendMessageCommand): void;
  markDelivered(chatId: number, command: MessageStatusCommand): void;
  markRead(chatId: number, command: MessageStatusCommand): void;
  sendTyping(chatId: number, command: TypingCommand): void;
}
```

Implement `createChatStompClient` with `Client` configuration:

```ts
{
  brokerURL: buildChatSocketUrl(env.apiBaseUrl, window.location.origin),
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 3000,
  heartbeatIncoming: 10_000,
  heartbeatOutgoing: 10_000
}
```

Allow an optional client factory parameter for tests. Subscribe inside every `onConnect`, route frames through `parseChatEvent`, and retain pending sends so they are re-published after reconnect until an ACK is received.

- [ ] **Step 4: Run adapter tests**

Run: `npm run test:unit -- src/features/chat/api/chat-socket.test.ts`

Expected: PASS.

- [ ] **Step 5: Commit the adapter**

```bash
git add frontend/src/features/chat/api/chat-socket.ts frontend/src/features/chat/api/chat-socket.test.ts
git commit -m "feat: add stomp chat adapter"
```

### Task 5: Reconcile Messages And Statuses In Pinia

**Files:**
- Modify: `frontend/src/shared/api/contracts.ts:602-618`
- Modify: `frontend/src/features/chat/api/chat-api.ts:20-44`
- Modify: `frontend/src/features/chat/model/chat-store.ts`
- Modify: `frontend/src/features/chat/model/use-chat.ts`
- Modify: `frontend/src/features/chat/ui/ChatMessageBubble.vue`
- Create: `frontend/src/features/chat/model/chat-store.test.ts`

- [ ] **Step 1: Write failing store tests**

Using `setActivePinia(createPinia())` and a mocked authenticated user ID 17, test these behaviors independently:

```ts
const pending = store.createPendingMessage("request-1");
expect(pending?.command.requestId).toBe("request-1");
expect(store.optimisticMessages[0].deliveryStatus).toBe("sending");

store.receiveSocketMessage(serverMessage({ clientMessageId: "request-1" }));
expect(store.optimisticMessages[0].id).toBe(501);
expect(store.optimisticMessages[0].deliveryStatus).toBe("SENT");

store.applyMessageStatus({ actorUserId: 2, upToMessageId: 501, status: "READ", changedAt, changedCount: 1 });
expect(store.optimisticMessages[0].deliveryStatus).toBe("READ");
```

Add tests proving duplicate server IDs are ignored, incoming active-chat messages return `{ delivered: true, read: true }`, typing state is keyed by chat, and presence state is keyed by user.

- [ ] **Step 2: Run store tests and verify RED**

Run: `npm run test:unit -- src/features/chat/model/chat-store.test.ts`

Expected: FAIL because request-ID reconciliation and status/event reducers do not exist.

- [ ] **Step 3: Update view-message state**

First update `ChatMessageResponse` to the exact server contract:

```ts
export interface ChatMessageResponse {
  id: number;
  chatId: number;
  senderUserId: number;
  clientMessageId: string;
  body: string | null;
  attachment: AttachmentInfo | null;
  deliveryStatus: ChatMessageDeliveryStatus;
  deliveredAt: string | null;
  readAt: string | null;
  createdAt: string;
}
```

Use server states without lowercasing:

```ts
export type ChatLocalDeliveryStatus = "sending" | "error";
export type ChatViewMessage = Omit<ChatMessageResponse, "deliveryStatus"> & {
  deliveryStatus: ChatMessageDeliveryStatus | ChatLocalDeliveryStatus;
};
```

`createPendingMessage(requestId)` must support text or uploaded attachment, set `clientMessageId` to `requestId`, set server timestamps to `null`, preserve the uploaded attachment, clear the composer, and return both the optimistic message and `SendMessageCommand`.

- [ ] **Step 4: Replace REST mutations with event reducers**

Delete `sendMessage`, `markAsRead`, and `sendTyping` from `chat-api.ts`. Remove the store's REST `sendMessage()` and all `chatApi.markAsRead()` calls. Reconcile by `clientMessageId`, de-duplicate by `id`, update status only for messages whose `id <= upToMessageId` and `senderUserId !== actorUserId`, and expose `applyTyping`, `applyPresence`, `failPendingMessage`, and `mergeSyncedMessages`.

- [ ] **Step 5: Preserve server delivery labels in the UI**

Stop rewriting every loaded message to `"sent"` in `use-chat.ts`. In `ChatMessageBubble.vue`, map own-message states as follows:

```ts
const labels = {
  sending: "sending",
  error: "error",
  SENT: "sent",
  DELIVERED: "delivered",
  READ: "read"
} as const;
```

- [ ] **Step 6: Run store tests and typecheck**

Run:

```bash
npm run test:unit -- src/features/chat/model/chat-store.test.ts
npm run typecheck
```

Expected: PASS.

- [ ] **Step 7: Commit state reconciliation**

```bash
git add frontend/src/shared/api/contracts.ts frontend/src/features/chat/api/chat-api.ts frontend/src/features/chat/model/chat-store.ts frontend/src/features/chat/model/chat-store.test.ts frontend/src/features/chat/model/use-chat.ts frontend/src/features/chat/ui/ChatMessageBubble.vue
git commit -m "feat: reconcile realtime chat state"
```

### Task 6: Coordinate Reconnect, Read, Typing, And Presence

**Files:**
- Replace: `frontend/src/features/chat/model/use-chat-socket.ts`
- Create: `frontend/src/features/chat/model/use-chat-socket.test.ts`
- Modify: `frontend/src/pages/ChatPage.vue:55-163`
- Modify: `frontend/src/features/chat/ui/ChatHeader.vue:5-27`

- [ ] **Step 1: Write failing composable tests**

Mock `createChatStompClient`, `chatApi.syncMessages`, `presenceApi.getByUserId`, and Pinia. Verify:

- `chat.message.created` reaches the store and emits `delivered` for an incoming message.
- An incoming message in the active chat also emits `read`.
- `chat.message.status.changed`, `chat.typing`, and `presence.changed` call their matching store reducers.
- reconnect calls `syncMessages(activeChatId, lastKnownMessageId)` and merges the result.
- a command error fails only the pending request with the envelope `requestId`.
- typing publishes `true` immediately and `false` after 3000 ms using fake timers.

- [ ] **Step 2: Run composable tests and verify RED**

Run: `npm run test:unit -- src/features/chat/model/use-chat-socket.test.ts`

Expected: FAIL because the composable still coordinates SSE and REST sending.

- [ ] **Step 3: Implement STOMP event coordination**

Replace manual reconnect timers with adapter callbacks. `sendActiveMessage()` must generate `crypto.randomUUID()`, call `store.createPendingMessage(requestId)`, and publish its command. Handle all six event envelope variants with an exhaustive switch.

On each connect, synchronize the active chat after its highest known message ID and re-request peer presence. Track the highest delivered/read command per chat to avoid repeated acknowledgements.

- [ ] **Step 4: Wire typing and peer status into the existing page**

In `ChatPage.vue`, replace the inline body assignment with:

```ts
const handleBodyUpdate = (value: string) => {
  form.value.body = value;
  chatSocket.sendTyping(value.trim().length > 0);
};
```

Pass it to `ChatInput`, stop typing after send, and remove `subscribe(chatId)` because the backend uses one user queue. Update `ChatHeader` so the existing status line shows `"печатает…"`, `"В сети"`, or `"Не в сети"`; show the transport-disconnected label only when STOMP is disconnected.

- [ ] **Step 5: Run composable tests and typecheck**

Run:

```bash
npm run test:unit -- src/features/chat/model/use-chat-socket.test.ts
npm run typecheck
```

Expected: PASS.

- [ ] **Step 6: Commit realtime coordination**

```bash
git add frontend/src/features/chat/model/use-chat-socket.ts frontend/src/features/chat/model/use-chat-socket.test.ts frontend/src/pages/ChatPage.vue frontend/src/features/chat/ui/ChatHeader.vue
git commit -m "feat: integrate stomp chat lifecycle"
```

### Task 7: Update Browser Mocks For STOMP

**Files:**
- Create: `frontend/tests/e2e/support/mock-stomp.ts`
- Modify: `frontend/tests/e2e/support/mock-api.ts:409-450`
- Modify: `frontend/tests/e2e/app.spec.ts:60-75`

- [ ] **Step 1: Run the existing chat E2E and verify RED**

Run: `npm run test:e2e -- --grep "chat and files"`

Expected: FAIL because the browser mock serves the removed REST send endpoint and does not implement `/ws`.

- [ ] **Step 2: Implement an in-browser STOMP server double**

`registerStompMock(page)` must install a `WebSocket` class before navigation. It accepts `CONNECT`, responds with `CONNECTED`, records `SUBSCRIBE`, and for a `/messages/send` `SEND` frame emits both `chat.command.ack` and `chat.message.created` `MESSAGE` frames to subscription `/user/queue/chat-events`. Generated messages must include `clientMessageId`, `deliveryStatus: "SENT"`, `deliveredAt: null`, and `readAt: null`.

- [ ] **Step 3: Update API fixtures and the E2E setup**

Add the new message fields to existing mock messages, remove the REST `POST /chats/401/messages` branch, call `registerStompMock(page)` before `page.goto`, and retain the assertion that the sent message appears.

- [ ] **Step 4: Run the focused E2E**

Run: `npm run test:e2e -- --grep "chat and files"`

Expected: PASS with the existing chat screenshot unless the delivery label changes; if it changes, inspect the image and update only `chat-flow-chromium-darwin.png`.

- [ ] **Step 5: Commit browser coverage**

```bash
git add frontend/tests/e2e/support/mock-stomp.ts frontend/tests/e2e/support/mock-api.ts frontend/tests/e2e/app.spec.ts frontend/tests/e2e/app.spec.ts-snapshots/chat-flow-chromium-darwin.png
git commit -m "test: cover stomp chat flow"
```

### Task 8: Full Verification

**Files:**
- Verify: all changed files

- [ ] **Step 1: Run all frontend unit tests**

Run: `cd frontend && npm run test:unit`

Expected: PASS with no warnings or unhandled rejections.

- [ ] **Step 2: Run frontend typecheck and production build**

Run:

```bash
npm run typecheck
npm run build
```

Expected: both commands exit 0.

- [ ] **Step 3: Run the complete browser suite**

Run: `npm run test:e2e`

Expected: all Playwright tests and snapshots pass.

- [ ] **Step 4: Run the complete backend suite**

Run: `cd ../backend && ./mvnw test`

Expected: BUILD SUCCESS.

- [ ] **Step 5: Verify backend parity and repository scope**

Recreate an `origin/develop` snapshot and run:

```bash
snapshot=$(mktemp -d /tmp/it-mentor-develop-verify.XXXXXX)
git archive --format=tar origin/develop -o "$snapshot/develop.tar"
mkdir "$snapshot/tree"
tar -xf "$snapshot/develop.tar" -C "$snapshot/tree"
diff -qr -x target -x .idea -x .DS_Store backend "$snapshot/tree"
git status --short
git diff --check
```

Expected: backend diff is empty; Git changes are limited to the planned frontend integration, plan/spec documents, and expected build artifacts remain ignored.

- [ ] **Step 6: Commit any final test-only corrections**

```bash
git add frontend
git commit -m "test: verify websocket chat integration"
```

Skip this commit when the verification run produces no tracked corrections.
