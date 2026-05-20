import type { ChatMessageResponse } from "@/shared/api/contracts";
import { env } from "@/shared/config/env";
import { tokenStorage } from "@/shared/lib/token-storage";

export const CHAT_SOCKET_ENDPOINT = "/chats/events";

type ChatEventName = "chat.message.created" | "chat.read" | "chat.typing" | "presence.changed";

export interface ChatSocketAdapter {
  connect(): Promise<void>;
  disconnect(): void;
  subscribe(chatId: number): void;
  onMessage(handler: (message: ChatMessageResponse) => void): void;
  onDisconnect(handler: () => void): void;
}

type ParsedSseEvent = {
  event: ChatEventName | "";
  data: string;
};

const parseEventBlock = (block: string): ParsedSseEvent => {
  let event: ParsedSseEvent["event"] = "";
  const data: string[] = [];

  block.split(/\r?\n/).forEach((line) => {
    if (!line || line.startsWith(":")) {
      return;
    }

    if (line.startsWith("event:")) {
      event = line.slice("event:".length).trim() as ParsedSseEvent["event"];
      return;
    }

    if (line.startsWith("data:")) {
      data.push(line.slice("data:".length).trimStart());
    }
  });

  return {
    event,
    data: data.join("\n")
  };
};

export const createChatSseClient = (): ChatSocketAdapter => {
  let abortController: AbortController | null = null;
  let messageHandler: ((message: ChatMessageResponse) => void) | null = null;
  let disconnectHandler: (() => void) | null = null;
  let isManualDisconnect = false;

  const handleEvent = (block: string) => {
    const parsed = parseEventBlock(block);

    if (parsed.event !== "chat.message.created" || !parsed.data) {
      return;
    }

    try {
      messageHandler?.(JSON.parse(parsed.data) as ChatMessageResponse);
    } catch {
      // Некорректное событие не должно обрывать весь SSE-поток.
    }
  };

  const readStream = async (response: Response, signal: AbortSignal) => {
    const reader = response.body?.getReader();

    if (!reader) {
      throw new Error("SSE stream is unavailable");
    }

    const decoder = new TextDecoder();
    let buffer = "";

    while (!signal.aborted) {
      const { done, value } = await reader.read();

      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });

      let separatorIndex = buffer.search(/\r?\n\r?\n/);
      while (separatorIndex >= 0) {
        const block = buffer.slice(0, separatorIndex);
        const separatorLength = buffer[separatorIndex] === "\r" ? 4 : 2;
        buffer = buffer.slice(separatorIndex + separatorLength);
        handleEvent(block);
        separatorIndex = buffer.search(/\r?\n\r?\n/);
      }
    }
  };

  return {
    async connect() {
      const token = tokenStorage.get();

      if (!token) {
        throw new Error("Missing access token");
      }

      isManualDisconnect = false;
      abortController = new AbortController();

      const response = await fetch(`${env.apiBaseUrl}${CHAT_SOCKET_ENDPOINT}`, {
        headers: {
          Accept: "text/event-stream",
          Authorization: `Bearer ${token}`
        },
        signal: abortController.signal
      });

      if (!response.ok) {
        throw new Error(`SSE connection failed with status ${response.status}`);
      }

      void readStream(response, abortController.signal)
        .catch(() => undefined)
        .finally(() => {
          if (!isManualDisconnect && !abortController?.signal.aborted) {
            disconnectHandler?.();
          }
        });
    },
    disconnect() {
      isManualDisconnect = true;
      abortController?.abort();
      abortController = null;
    },
    subscribe(_chatId: number) {
      // SSE-подписка идёт сразу на все чаты пользователя; отдельная подписка не нужна.
    },
    onMessage(handler) {
      messageHandler = handler;
    },
    onDisconnect(handler) {
      disconnectHandler = handler;
    }
  };
};
