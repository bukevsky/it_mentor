import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  CHAT_SOCKET_ENDPOINT,
  createChatSseClient,
  type ChatSocketAdapter
} from "@/features/chat/api/chat-socket";
import { useChatStore } from "./chat-store";

type ChatSocketStatus = "idle" | "connecting" | "connected" | "disconnected";

const status = ref<ChatSocketStatus>("idle");
const reconnectAttempt = ref(0);
const adapter = ref<ChatSocketAdapter | null>(null);
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let shouldReconnect = true;

export const useChatSocket = () => {
  const chatStore = useChatStore();
  const { activeChat } = storeToRefs(chatStore);

  const statusLabel = computed(() => {
    if (status.value === "connected") {
      return "В сети";
    }

    if (status.value === "connecting") {
      return "Подключение...";
    }

    return "Соединение потеряно";
  });

  const scheduleReconnect = () => {
    if (!shouldReconnect) {
      return;
    }

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
    shouldReconnect = true;

    const nextAdapter = createChatSseClient();
    nextAdapter.onMessage((message) => {
      chatStore.receiveSocketMessage(message);
    });
    nextAdapter.onDisconnect(() => {
      adapter.value = null;
      scheduleReconnect();
    });

    try {
      adapter.value = nextAdapter;
      await nextAdapter.connect();
      status.value = "connected";
      reconnectAttempt.value = 0;

      if (activeChat.value) {
        nextAdapter.subscribe(activeChat.value.id);
      }
    } catch {
      if (adapter.value === nextAdapter) {
        adapter.value = null;
      }
      scheduleReconnect();
    }
  };

  const subscribe = (chatId: number) => {
    adapter.value?.subscribe(chatId);
  };

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    shouldReconnect = false;
    reconnectAttempt.value = 0;
    status.value = "idle";
    adapter.value?.disconnect();
    adapter.value = null;
  };

  const sendActiveMessage = async () => {
    await chatStore.sendMessage();
  };

  return {
    connect,
    disconnect,
    endpoint: CHAT_SOCKET_ENDPOINT,
    sendActiveMessage,
    status,
    statusLabel,
    subscribe
  };
};
