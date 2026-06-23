import {
  Client,
  type IFrame,
  type IMessage,
  type StompConfig,
  type StompSubscription
} from "@stomp/stompjs";
import type {
  ChatEventEnvelope,
  ChatMessageResponse,
  MessageStatusCommand,
  SendMessageCommand,
  TypingCommand
} from "@/shared/api/contracts";
import { env } from "@/shared/config/env";
import { tokenStorage } from "@/shared/lib/token-storage";
import {
  buildChatSocketUrl,
  CHAT_EVENTS_DESTINATION,
  CHAT_SOCKET_PATH,
  chatDestination,
  parseChatEvent
} from "./chat-protocol";

export const CHAT_SOCKET_ENDPOINT = CHAT_SOCKET_PATH;

export interface StompClientPort {
  onConnect: (frame: IFrame) => void;
  onStompError: (frame: IFrame) => void;
  onWebSocketClose: (event: CloseEvent) => void;
  activate(): void;
  deactivate(): Promise<void>;
  publish(params: { destination: string; body: string }): void;
  subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription;
}

export type StompClientFactory = (config: StompConfig) => StompClientPort;

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
  subscribe(chatId: number): void;
  onMessage(handler: (message: ChatMessageResponse) => void): void;
}

interface CreateChatStompClientOptions {
  token?: string;
  apiBaseUrl?: string;
  appOrigin?: string;
  clientFactory?: StompClientFactory;
}

const defaultClientFactory: StompClientFactory = (config) => new Client(config);

export const createChatStompClient = (
  options: CreateChatStompClientOptions = {}
): ChatSocketAdapter => {
  const token = options.token ?? tokenStorage.get();
  if (!token) {
    throw new Error("Missing access token");
  }

  const appOrigin = options.appOrigin ?? window.location.origin;
  const clientFactory = options.clientFactory ?? defaultClientFactory;
  const pendingMessages = new Map<string, { chatId: number; command: SendMessageCommand }>();
  let connectHandler: (() => void) | null = null;
  let disconnectHandler: (() => void) | null = null;
  let eventHandler: ((event: ChatEventEnvelope) => void) | null = null;
  let errorHandler: ((error: Error) => void) | null = null;
  let messageHandler: ((message: ChatMessageResponse) => void) | null = null;
  let resolveConnect: (() => void) | null = null;
  let rejectConnect: ((error: Error) => void) | null = null;

  const client = clientFactory({
    brokerURL: buildChatSocketUrl(options.apiBaseUrl ?? env.apiBaseUrl, appOrigin),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000
  });

  const publish = (destination: string, command: object) => {
    client.publish({ destination, body: JSON.stringify(command) });
  };

  const handleFrame = (frame: IMessage) => {
    try {
      const event = parseChatEvent(frame.body);
      if (!event) {
        return;
      }

      if (event.type === "chat.command.ack" && event.payload.command === "send") {
        pendingMessages.delete(event.requestId);
      }

      if (event.type === "chat.message.created") {
        messageHandler?.(event.payload);
      }
      eventHandler?.(event);
    } catch (error) {
      errorHandler?.(error instanceof Error ? error : new Error("Malformed chat event"));
    }
  };

  client.onConnect = () => {
    client.subscribe(CHAT_EVENTS_DESTINATION, handleFrame);
    pendingMessages.forEach(({ chatId, command }) => {
      publish(chatDestination(chatId, "send"), command);
    });
    resolveConnect?.();
    resolveConnect = null;
    rejectConnect = null;
    connectHandler?.();
  };

  client.onStompError = (frame) => {
    const error = new Error(frame.headers.message ?? "STOMP connection error");
    rejectConnect?.(error);
    resolveConnect = null;
    rejectConnect = null;
    errorHandler?.(error);
  };

  client.onWebSocketClose = () => {
    disconnectHandler?.();
  };

  return {
    connect() {
      return new Promise<void>((resolve, reject) => {
        resolveConnect = resolve;
        rejectConnect = reject;
        client.activate();
      });
    },
    disconnect() {
      return client.deactivate();
    },
    onConnect(handler) {
      connectHandler = handler;
    },
    onDisconnect(handler) {
      disconnectHandler = handler;
    },
    onEvent(handler) {
      eventHandler = handler;
    },
    onError(handler) {
      errorHandler = handler;
    },
    sendMessage(chatId, command) {
      pendingMessages.set(command.requestId, { chatId, command });
      publish(chatDestination(chatId, "send"), command);
    },
    markDelivered(chatId, command) {
      publish(chatDestination(chatId, "delivered"), command);
    },
    markRead(chatId, command) {
      publish(chatDestination(chatId, "read"), command);
    },
    sendTyping(chatId, command) {
      publish(chatDestination(chatId, "typing"), command);
    },
    subscribe(_chatId) {
      // The backend exposes one user-scoped queue for all chats.
    },
    onMessage(handler) {
      messageHandler = handler;
    }
  };
};

export const createChatSseClient = createChatStompClient;
