import { describe, expect, it } from "vitest";
import {
  buildChatSocketUrl,
  chatDestination,
  parseChatEvent
} from "./chat-protocol";

describe("chat protocol", () => {
  it("builds websocket URLs for proxied and absolute API bases", () => {
    expect(buildChatSocketUrl("/api", "https://app.example")).toBe("wss://app.example/api/ws");
    expect(buildChatSocketUrl("https://api.example", "https://app.example")).toBe(
      "wss://api.example/ws"
    );
  });

  it("builds STOMP command destinations", () => {
    expect(chatDestination(7, "send")).toBe("/app/chats/7/messages/send");
    expect(chatDestination(7, "delivered")).toBe("/app/chats/7/messages/delivered");
    expect(chatDestination(7, "read")).toBe("/app/chats/7/messages/read");
    expect(chatDestination(7, "typing")).toBe("/app/chats/7/typing");
  });

  it("parses known envelopes and ignores unknown event types", () => {
    const known = parseChatEvent(
      JSON.stringify({
        type: "chat.typing",
        requestId: "00000000-0000-4000-8000-000000000001",
        chatId: 7,
        occurredAt: "2026-06-23T10:00:00Z",
        payload: { chatId: 7, userId: 2, typing: true }
      })
    );

    expect(known?.type).toBe("chat.typing");
    expect(parseChatEvent('{"type":"future.event"}')).toBeNull();
  });

  it("rejects malformed known events", () => {
    expect(() => parseChatEvent('{"type":"chat.message.created"}')).toThrow(
      "Malformed chat event"
    );
  });
});
