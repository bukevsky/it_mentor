import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import type {
  ChatMessageResponse,
  ChatResponse,
  MessageStatusChangedPayload
} from "@/shared/api/contracts";
import { useChatStore } from "./chat-store";

vi.mock("@/features/auth/model/auth-store", () => ({
  useAuthStore: () => ({
    user: { id: 17 },
    isAuthenticated: true
  })
}));

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

const serverMessage = (
  overrides: Partial<ChatMessageResponse> = {}
): ChatMessageResponse =>
  ({
    id: 501,
    chatId: 401,
    senderUserId: 17,
    clientMessageId: "request-1",
    body: "hello",
    attachment: null,
    deliveryStatus: "SENT",
    deliveredAt: null,
    readAt: null,
    createdAt: "2026-06-23T10:00:00Z",
    ...overrides
  }) as ChatMessageResponse;

describe("chat store realtime state", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("creates and reconciles an optimistic message by request ID", () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.form.body = "hello";

    const pending = store.createPendingMessage("request-1");

    expect(pending?.command.requestId).toBe("request-1");
    expect(store.optimisticMessages[0].deliveryStatus).toBe("sending");

    store.receiveSocketMessage(serverMessage());

    expect(store.optimisticMessages[0].id).toBe(501);
    expect(store.optimisticMessages[0].deliveryStatus).toBe("SENT");
  });

  it("applies delivery status changes only to the other actor's messages", () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.messages = {
      content: [serverMessage({ senderUserId: 17 })],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
      last: true
    };
    const payload: MessageStatusChangedPayload = {
      actorUserId: 22,
      upToMessageId: 501,
      status: "READ",
      changedAt: "2026-06-23T10:01:00Z",
      changedCount: 1
    };

    store.applyMessageStatus(payload);

    expect(store.messages.content[0].deliveryStatus).toBe("READ");
    expect(store.messages.content[0].readAt).toBe(payload.changedAt);
  });

  it("does not append duplicate server messages", () => {
    const store = useChatStore();
    store.activeChat = chat;
    store.messages = {
      content: [serverMessage()],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
      last: true
    };

    store.receiveSocketMessage(serverMessage());

    expect(store.messages.content).toHaveLength(1);
  });

  it("returns delivery and read acknowledgements for an active incoming message", () => {
    const store = useChatStore();
    store.activeChat = chat;

    const result = store.receiveSocketMessage(serverMessage({ senderUserId: 22 }));

    expect(result).toEqual({ delivered: true, read: true });
  });

  it("stores typing and presence state by chat and user", () => {
    const store = useChatStore();

    store.applyTyping({ chatId: 401, userId: 22, typing: true });
    store.applyPresence({ userId: 22, status: "online", lastSeenAt: null });

    expect(store.typingByChatId[401]).toEqual({ chatId: 401, userId: 22, typing: true });
    expect(store.presenceByUserId[22]).toEqual({
      userId: 22,
      status: "online",
      lastSeenAt: null
    });
  });
});
