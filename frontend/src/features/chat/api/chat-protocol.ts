import type { ChatEventEnvelope } from "@/shared/api/contracts";

export const CHAT_SOCKET_PATH = "/ws";
export const CHAT_EVENTS_DESTINATION = "/user/queue/chat-events";

type ChatCommandName = "send" | "delivered" | "read" | "typing";

const knownEventTypes = new Set<ChatEventEnvelope["type"]>([
  "chat.command.ack",
  "chat.command.error",
  "chat.message.created",
  "chat.message.status.changed",
  "chat.typing",
  "presence.changed"
]);

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null;

export const buildChatSocketUrl = (apiBaseUrl: string, appOrigin: string) => {
  const basePath = apiBaseUrl.replace(/\/+$/, "");
  const url = new URL(`${basePath}${CHAT_SOCKET_PATH}`, appOrigin);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  return url.toString().replace(/\/$/, "");
};

export const chatDestination = (chatId: number, command: ChatCommandName) => {
  const suffix = command === "typing" ? "typing" : `messages/${command}`;
  return `/app/chats/${chatId}/${suffix}`;
};

export const parseChatEvent = (body: string): ChatEventEnvelope | null => {
  const parsed: unknown = JSON.parse(body);

  if (!isRecord(parsed) || typeof parsed.type !== "string") {
    throw new Error("Malformed chat event");
  }

  if (!knownEventTypes.has(parsed.type as ChatEventEnvelope["type"])) {
    return null;
  }

  if (
    !Object.hasOwn(parsed, "requestId") ||
    !Object.hasOwn(parsed, "chatId") ||
    typeof parsed.occurredAt !== "string" ||
    !Object.hasOwn(parsed, "payload") ||
    !isRecord(parsed.payload)
  ) {
    throw new Error("Malformed chat event");
  }

  return parsed as ChatEventEnvelope;
};
