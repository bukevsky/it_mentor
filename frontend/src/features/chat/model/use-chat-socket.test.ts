import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import type { ChatEventEnvelope, ChatMessageResponse, ChatResponse } from "@/shared/api/contracts";

const mocks = vi.hoisted(() => {
  let eventHandler: ((event: ChatEventEnvelope) => void) | null = null;
  let connectHandler: (() => void) | null = null;

  const socket = {
    connect: vi.fn(async () => connectHandler?.()),
    disconnect: vi.fn(async () => undefined),
    onConnect: vi.fn((handler: () => void) => {
      connectHandler = handler;
    }),
    onDisconnect: vi.fn(),
    onEvent: vi.fn((handler: (event: ChatEventEnvelope) => void) => {
      eventHandler = handler;
    }),
    onError: vi.fn(),
    sendMessage: vi.fn(),
    markDelivered: vi.fn(),
    markRead: vi.fn(),
    sendTyping: vi.fn(),
    subscribe: vi.fn(),
    onMessage: vi.fn()
  };

  return {
    socket,
    syncMessages: vi.fn(async () => [] as ChatMessageResponse[]),
    getPresence: vi.fn(async () => ({ userId: 22, status: "online" as const, lastSeenAt: null })),
    emitEvent(event: ChatEventEnvelope) {
      eventHandler?.(event);
    },
    resetHandlers() {
      eventHandler = null;
      connectHandler = null;
    }
  };
});

vi.mock("@/features/chat/api/chat-socket", () => ({
  CHAT_SOCKET_ENDPOINT: "/ws",
  createChatStompClient: () => mocks.socket
}));

vi.mock("@/features/chat/api/chat-api", () => ({
  chatApi: {
    syncMessages: mocks.syncMessages
  }
}));

vi.mock("@/features/presence/api/presence-api", () => ({
  presenceApi: {
    getByUserId: mocks.getPresence
  }
}));

vi.mock("@/features/auth/model/auth-store", () => ({
  useAuthStore: () => ({ user: { id: 17 }, isAuthenticated: true })
}));

import { useChatStore } from "./chat-store";
import { useChatSocket } from "./use-chat-socket";

const chat: ChatResponse = {
  id: 401,
  mentoringRequestId: 301,
  studentUserId: 17,
  mentorUserId: 22,
  createdAt: "2026-06-23T09:00:00Z",
  lastMessage: null,
  lastMessageAt: null,
  lastSenderUserId: null,
  unreadCount: 0,
  studentName: "Student",
  mentorName: "Mentor",
  mentoringRequestStatus: "ACCEPTED"
};

const message: ChatMessageResponse = {
  id: 501,
  chatId: 401,
  senderUserId: 22,
  clientMessageId: "request-1",
  body: "hello",
  attachment: null,
  deliveryStatus: "SENT",
  deliveredAt: null,
  readAt: null,
  createdAt: "2026-06-23T10:00:00Z"
};

describe("chat socket coordination", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    mocks.resetHandlers();
    vi.clearAllMocks();
  });

  afterEach(async () => {
    await useChatSocket().disconnect();
    vi.useRealTimers();
  });

  it("acknowledges an incoming active-chat message as delivered and read", async () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.chats = {
      content: [chat],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
      last: true
    };
    const socket = useChatSocket();
    await socket.connect();

    mocks.emitEvent({
      type: "chat.message.created",
      requestId: "request-1",
      chatId: 401,
      occurredAt: message.createdAt,
      payload: message
    });

    expect(mocks.socket.markDelivered).toHaveBeenCalledWith(
      401,
      expect.objectContaining({ upToMessageId: 501 })
    );
    expect(mocks.socket.markRead).toHaveBeenCalledWith(
      401,
      expect.objectContaining({ upToMessageId: 501 })
    );
  });

  it("routes status, typing, presence, and command errors to the store", async () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.form.body = "pending";
    store.createPendingMessage("request-error");
    const socket = useChatSocket();
    await socket.connect();

    mocks.emitEvent({
      type: "chat.typing",
      requestId: "request-typing",
      chatId: 401,
      occurredAt: message.createdAt,
      payload: { chatId: 401, userId: 22, typing: true }
    });
    mocks.emitEvent({
      type: "presence.changed",
      requestId: null,
      chatId: null,
      occurredAt: message.createdAt,
      payload: { userId: 22, status: "online", lastSeenAt: null }
    });
    mocks.emitEvent({
      type: "chat.command.error",
      requestId: "request-error",
      chatId: 401,
      occurredAt: message.createdAt,
      payload: { code: "CONFLICT", message: "Ошибка", fieldErrors: {} }
    });

    expect(store.typingByChatId[401]?.typing).toBe(true);
    expect(store.presenceByUserId[22]?.status).toBe("online");
    expect(store.optimisticMessages[0].deliveryStatus).toBe("error");
  });

  it("synchronizes missed messages and requests peer presence on connect", async () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.messages = {
      content: [message],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
      last: true
    };

    await useChatSocket().connect();

    expect(mocks.syncMessages).toHaveBeenCalledWith(401, 501);
    expect(mocks.getPresence).toHaveBeenCalledWith(22);
  });

  it("publishes typing false after the idle timeout", async () => {
    vi.useFakeTimers();
    const store = useChatStore();
    store.activeChat = chat;
    const socket = useChatSocket();
    await socket.connect();

    socket.sendTyping(true);
    expect(mocks.socket.sendTyping).toHaveBeenLastCalledWith(
      401,
      expect.objectContaining({ typing: true })
    );

    await vi.advanceTimersByTimeAsync(3000);

    expect(mocks.socket.sendTyping).toHaveBeenLastCalledWith(
      401,
      expect.objectContaining({ typing: false })
    );
  });
});
