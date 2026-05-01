import { computed, onBeforeUnmount, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  CHAT_SOCKET_ENDPOINT,
  createMockChatSocket,
  USE_MOCK_CHAT_SOCKET,
  type ChatSocketAdapter
} from "@/features/chat/api/chat-socket";
import { useChatStore } from "./chat-store";

type ChatSocketStatus = "idle" | "connecting" | "connected" | "disconnected";

export const useChatSocket = () => {
  const chatStore = useChatStore();
  const { activeChat } = storeToRefs(chatStore);

  const status = ref<ChatSocketStatus>("idle");
  const reconnectAttempt = ref(0);
  const adapter = ref<ChatSocketAdapter | null>(null);
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  const statusLabel = computed(() => {
    if (status.value === "connected") {
      return USE_MOCK_CHAT_SOCKET ? "Demo realtime" : "В сети";
    }

    if (status.value === "connecting") {
      return "Подключение...";
    }

    return "Соединение потеряно";
  });

  const scheduleReconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
    }

    const timeout = Math.min(8000, 1000 * 2 ** reconnectAttempt.value);
    reconnectAttempt.value += 1;
    status.value = "disconnected";

    reconnectTimer = setTimeout(() => {
      void connect();
    }, timeout);
  };

  const connect = async () => {
    if (adapter.value || status.value === "connecting") {
      return;
    }

    status.value = "connecting";

    const nextAdapter = createMockChatSocket();
    nextAdapter.onMessage((message) => {
      chatStore.receiveSocketMessage(message);
    });
    nextAdapter.onDisconnect(() => {
      adapter.value = null;
      scheduleReconnect();
    });

    try {
      await nextAdapter.connect();
      adapter.value = nextAdapter;
      status.value = "connected";
      reconnectAttempt.value = 0;

      if (activeChat.value) {
        nextAdapter.subscribe(activeChat.value.id);
      }
    } catch {
      adapter.value = null;
      scheduleReconnect();
    }
  };

  const subscribe = (chatId: number) => {
    adapter.value?.subscribe(chatId);
  };

  const sendActiveMessage = async () => {
    const pendingMessage = chatStore.createPendingMessage();

    if (!pendingMessage?.tempId) {
      return;
    }

    if (!adapter.value || status.value !== "connected") {
      chatStore.failPendingMessage(pendingMessage.tempId);
      scheduleReconnect();
      return;
    }

    try {
      const sentMessage = await adapter.value.sendMessage({
        chatId: pendingMessage.chatId,
        senderUserId: pendingMessage.senderUserId,
        body: pendingMessage.body,
        attachmentFileId: null,
        tempId: pendingMessage.tempId
      });
      chatStore.confirmPendingMessage(pendingMessage.tempId, sentMessage);
    } catch {
      chatStore.failPendingMessage(pendingMessage.tempId);
    }
  };

  onBeforeUnmount(() => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
    }

    adapter.value?.disconnect();
    adapter.value = null;
  });

  return {
    connect,
    endpoint: CHAT_SOCKET_ENDPOINT,
    sendActiveMessage,
    status,
    statusLabel,
    subscribe
  };
};
