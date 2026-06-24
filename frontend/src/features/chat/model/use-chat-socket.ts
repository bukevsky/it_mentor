import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { chatApi } from "@/features/chat/api/chat-api";
import {
  CHAT_SOCKET_ENDPOINT,
  createChatStompClient,
  type ChatSocketAdapter
} from "@/features/chat/api/chat-socket";
import { presenceApi } from "@/features/presence/api/presence-api";
import type { ChatEventEnvelope } from "@/shared/api/contracts";
import { useChatStore } from "./chat-store";

type ChatSocketStatus = "idle" | "connecting" | "connected" | "disconnected";

const status = ref<ChatSocketStatus>("idle");
const adapter = ref<ChatSocketAdapter | null>(null);
let typingTimer: ReturnType<typeof setTimeout> | null = null;

export const useChatSocket = () => {
  const chatStore = useChatStore();
  const { activeChat, presenceByUserId, typingByChatId } = storeToRefs(chatStore);

  const activePeerUserId = computed(() => chatStore.getPeerUserId());
  const activePeerPresence = computed(() => {
    const peerUserId = activePeerUserId.value;
    return peerUserId ? presenceByUserId.value[peerUserId] ?? null : null;
  });
  const isPeerTyping = computed(() => {
    const typing = activeChat.value ? typingByChatId.value[activeChat.value.id] : null;
    return Boolean(typing?.typing && typing.userId === activePeerUserId.value);
  });
  const isPeerOnline = computed(() => activePeerPresence.value?.status === "online");
  const statusLabel = computed(() => {
    if (status.value === "connecting") return "Подключение...";
    if (status.value !== "connected") return "Соединение потеряно";
    if (isPeerTyping.value) return "печатает...";
    return isPeerOnline.value ? "В сети" : "Не в сети";
  });

  const createRequestId = () => crypto.randomUUID();

  const synchronizeActiveChat = async () => {
    const chat = activeChat.value;
    if (!chat) {
      return;
    }

    const afterMessageId = chatStore.getLastKnownMessageId(chat.id);
    const [missedMessages, peerPresence] = await Promise.all([
      chatApi.syncMessages(chat.id, afterMessageId),
      presenceApi.getByUserId(chatStore.getPeerUserId(chat) ?? 0)
    ]);
    chatStore.mergeSyncedMessages(missedMessages);
    chatStore.applyPresence(peerPresence);
  };

  const acknowledgeMessage = (
    socket: ChatSocketAdapter,
    chatId: number,
    messageId: number,
    result: { delivered: boolean; read: boolean }
  ) => {
    if (result.delivered) {
      socket.markDelivered(chatId, {
        requestId: createRequestId(),
        upToMessageId: messageId
      });
    }
    if (result.read) {
      socket.markRead(chatId, {
        requestId: createRequestId(),
        upToMessageId: messageId
      });
    }
  };

  const handleEvent = (socket: ChatSocketAdapter, event: ChatEventEnvelope) => {
    switch (event.type) {
      case "chat.command.ack":
        if (event.payload.command === "send" && event.payload.duplicate) {
          void synchronizeActiveChat();
        }
        return;
      case "chat.command.error":
        if (event.requestId) {
          chatStore.failPendingMessage(event.requestId, event.payload.message);
        }
        return;
      case "chat.message.created": {
        const result = chatStore.receiveSocketMessage(event.payload);
        acknowledgeMessage(socket, event.chatId, event.payload.id, result);
        return;
      }
      case "chat.message.status.changed":
        chatStore.applyMessageStatus(event.payload);
        return;
      case "chat.typing":
        chatStore.applyTyping(event.payload);
        return;
      case "presence.changed":
        chatStore.applyPresence(event.payload);
    }
  };

  const connect = async () => {
    if (adapter.value || status.value === "connecting") {
      return;
    }

    status.value = "connecting";
    const socket = createChatStompClient();
    let initialConnectionComplete = false;
    socket.onConnect(() => {
      status.value = "connected";
      if (initialConnectionComplete) {
        void synchronizeActiveChat();
      }
    });
    socket.onDisconnect(() => {
      status.value = "disconnected";
    });
    socket.onEvent((event) => handleEvent(socket, event));
    socket.onError(() => {
      status.value = "disconnected";
    });
    adapter.value = socket;

    try {
      await socket.connect();
      status.value = "connected";
      await synchronizeActiveChat();
      initialConnectionComplete = true;
    } catch {
      adapter.value = null;
      status.value = "disconnected";
    }
  };

  const disconnect = async () => {
    if (typingTimer) {
      clearTimeout(typingTimer);
      typingTimer = null;
    }
    const socket = adapter.value;
    adapter.value = null;
    status.value = "idle";
    await socket?.disconnect();
  };

  const sendActiveMessage = () => {
    const chat = activeChat.value;
    const socket = adapter.value;
    if (!chat || !socket) {
      return;
    }

    const requestId = createRequestId();
    const pending = chatStore.createPendingMessage(requestId);
    if (pending) {
      socket.sendMessage(chat.id, pending.command);
    }
  };

  const sendTyping = (typing: boolean) => {
    const chatId = activeChat.value?.id;
    const socket = adapter.value;
    if (!chatId || !socket) {
      return;
    }

    if (typingTimer) {
      clearTimeout(typingTimer);
      typingTimer = null;
    }

    socket.sendTyping(chatId, { requestId: createRequestId(), typing });
    if (typing) {
      typingTimer = setTimeout(() => {
        socket.sendTyping(chatId, { requestId: createRequestId(), typing: false });
        typingTimer = null;
      }, 3000);
    }
  };

  const markActiveChatRead = () => {
    const chatId = activeChat.value?.id;
    const socket = adapter.value;
    if (!chatId || !socket) {
      return;
    }
    const upToMessageId = chatStore.getHighestIncomingMessageId(chatId);
    if (upToMessageId) {
      socket.markRead(chatId, { requestId: createRequestId(), upToMessageId });
    }
  };

  return {
    connect,
    disconnect,
    endpoint: CHAT_SOCKET_ENDPOINT,
    isPeerOnline,
    isPeerTyping,
    markActiveChatRead,
    sendActiveMessage,
    sendTyping,
    status,
    statusLabel
  };
};
