import { describe, expect, it, vi } from "vitest";
import type { IMessage, IFrame, StompConfig, StompSubscription } from "@stomp/stompjs";
import {
  createChatStompClient,
  type StompClientFactory,
  type StompClientPort
} from "./chat-socket";

const createFakeClient = () => {
  let subscriptionHandler: ((message: IMessage) => void) | null = null;

  const client: StompClientPort = {
    onConnect: () => undefined,
    onStompError: () => undefined,
    onWebSocketClose: () => undefined,
    activate: vi.fn(),
    deactivate: vi.fn().mockResolvedValue(undefined),
    publish: vi.fn(),
    subscribe: vi.fn((_destination, handler) => {
      subscriptionHandler = handler;
      return { id: "subscription-1", unsubscribe: vi.fn() } as StompSubscription;
    })
  };

  return {
    client,
    emitConnect: () => client.onConnect({} as IFrame),
    emitMessage: (body: string) => subscriptionHandler?.({ body } as IMessage)
  };
};

describe("STOMP chat adapter", () => {
  it("connects with JWT and subscribes to the user event queue", async () => {
    const fake = createFakeClient();
    let config: StompConfig | null = null;
    const factory: StompClientFactory = (nextConfig) => {
      config = nextConfig;
      return fake.client;
    };
    const adapter = createChatStompClient({
      token: "jwt-token",
      apiBaseUrl: "/api",
      appOrigin: "https://app.example",
      clientFactory: factory
    });

    const connected = adapter.connect();
    expect(fake.client.activate).toHaveBeenCalledOnce();
    fake.emitConnect();
    await connected;

    expect(config?.brokerURL).toBe("wss://app.example/api/ws");
    expect(config?.connectHeaders).toEqual({ Authorization: "Bearer jwt-token" });
    expect(fake.client.subscribe).toHaveBeenCalledWith(
      "/user/queue/chat-events",
      expect.any(Function)
    );
  });

  it("publishes send commands to the backend destination", () => {
    const fake = createFakeClient();
    const adapter = createChatStompClient({
      token: "jwt-token",
      apiBaseUrl: "/api",
      appOrigin: "http://localhost:5173",
      clientFactory: () => fake.client
    });
    const command = {
      requestId: "00000000-0000-4000-8000-000000000001",
      body: "hello",
      attachmentFileId: null
    };

    adapter.sendMessage(7, command);

    expect(fake.client.publish).toHaveBeenCalledWith({
      destination: "/app/chats/7/messages/send",
      body: JSON.stringify(command)
    });
  });

  it("reports malformed frames without dispatching an event", async () => {
    const fake = createFakeClient();
    const onEvent = vi.fn();
    const onError = vi.fn();
    const adapter = createChatStompClient({
      token: "jwt-token",
      apiBaseUrl: "/api",
      appOrigin: "http://localhost:5173",
      clientFactory: () => fake.client
    });
    adapter.onEvent(onEvent);
    adapter.onError(onError);

    const connected = adapter.connect();
    fake.emitConnect();
    await connected;
    fake.emitMessage('{"type":"chat.message.created"}');

    expect(onEvent).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: "Malformed chat event" }));
  });

  it("deactivates the STOMP client on disconnect", async () => {
    const fake = createFakeClient();
    const adapter = createChatStompClient({
      token: "jwt-token",
      apiBaseUrl: "/api",
      appOrigin: "http://localhost:5173",
      clientFactory: () => fake.client
    });

    await adapter.disconnect();

    expect(fake.client.deactivate).toHaveBeenCalledOnce();
  });
});
