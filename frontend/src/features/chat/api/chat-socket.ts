import type { ChatMessageResponse } from "@/shared/api/contracts";

export const CHAT_SOCKET_ENDPOINT = "/ws/chats";
export const USE_MOCK_CHAT_SOCKET = true;

export interface ChatSocketPayload {
  chatId: number;
  senderUserId: number;
  body: string | null;
  attachmentFileId?: number | null;
  tempId: string;
}

export interface ChatSocketAdapter {
  connect(): Promise<void>;
  disconnect(): void;
  subscribe(chatId: number): void;
  sendMessage(payload: ChatSocketPayload): Promise<ChatMessageResponse>;
  onMessage(handler: (message: ChatMessageResponse) => void): void;
  onDisconnect(handler: () => void): void;
}

export const createMockChatSocket = (): ChatSocketAdapter => {
  let activeChatId: number | null = null;
  let messageHandler: ((message: ChatMessageResponse) => void) | null = null;
  let disconnectHandler: (() => void) | null = null;

  return {
    async connect() {
      await new Promise((resolve) => setTimeout(resolve, 240));
    },
    disconnect() {
      disconnectHandler?.();
    },
    subscribe(chatId: number) {
      activeChatId = chatId;
    },
    async sendMessage(payload: ChatSocketPayload) {
      await new Promise((resolve) => setTimeout(resolve, 520));

      return {
        id: Date.now(),
        chatId: payload.chatId,
        senderUserId: payload.senderUserId,
        body: payload.body,
        attachment: null,
        createdAt: new Date().toISOString()
      };
    },
    onMessage(handler) {
      messageHandler = handler;
    },
    onDisconnect(handler) {
      disconnectHandler = handler;
    }
  };
};
