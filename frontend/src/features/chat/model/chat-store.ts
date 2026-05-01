import { reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ChatMessageResponse,
  ChatResponse,
  ErrorResponse,
  MentoringRequestResponse,
  PagedResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { chatApi } from "../api/chat-api";

export type ChatDeliveryStatus = "sending" | "sent" | "error";

export interface ChatViewMessage extends ChatMessageResponse {
  deliveryStatus?: ChatDeliveryStatus;
  tempId?: string;
}

const toPagedResponse = <T>(content: T[]): PagedResponse<T> => ({
  content,
  page: 0,
  size: content.length || 1,
  totalElements: content.length,
  totalPages: 1,
  last: true
});

export const useChatStore = defineStore("chat", () => {
  const authStore = useAuthStore();

  const form = reactive({
    requestId: "",
    body: ""
  });

  const chats = ref<PagedResponse<ChatResponse> | null>(null);
  const activeChat = ref<ChatResponse | null>(null);
  const messages = ref<PagedResponse<ChatMessageResponse> | null>(null);
  const lastMessages = ref<Record<number, ChatMessageResponse | null>>({});
  const requestDetails = ref<Record<number, MentoringRequestResponse | null>>({});
  const optimisticMessages = ref<ChatViewMessage[]>([]);
  const readChatIds = ref<Set<number>>(new Set());
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isLoadingChats = ref(false);
  const isLoadingMessages = ref(false);
  const isOpeningByRequestId = ref(false);
  const isSending = ref(false);
  let successTimer: ReturnType<typeof setTimeout> | null = null;

  const markChatRead = (chatId: number) => {
    readChatIds.value = new Set([...readChatIds.value, chatId]);
  };

  const markChatUnread = (chatId: number) => {
    const nextReadChatIds = new Set(readChatIds.value);
    nextReadChatIds.delete(chatId);
    readChatIds.value = nextReadChatIds;
  };

  const clearSuccessSoon = () => {
    if (successTimer) {
      clearTimeout(successTimer);
    }

    successTimer = setTimeout(() => {
      successMessage.value = "";
      successTimer = null;
    }, 3200);
  };

  const setSuccess = (message: string) => {
    successMessage.value = message;
    clearSuccessSoon();
  };

  const loadLastMessages = async (items: ChatResponse[]) => {
    await Promise.all(
      items.map(async (chat) => {
        try {
          const response = await chatApi.getMessages(chat.id, { page: 0, size: 1 });
          lastMessages.value = {
            ...lastMessages.value,
            [chat.id]: response.content[0] ?? null
          };
        } catch {
          lastMessages.value = {
            ...lastMessages.value,
            [chat.id]: null
          };
        }
      })
    );
  };

  const loadRequestDetails = async (items: ChatResponse[]) => {
    await Promise.all(
      items.map(async (chat) => {
        if (chat.mentoringRequestId in requestDetails.value) {
          return;
        }

        try {
          const request = await mentoringApi.getById(chat.mentoringRequestId);
          requestDetails.value = {
            ...requestDetails.value,
            [chat.mentoringRequestId]: request
          };
        } catch {
          requestDetails.value = {
            ...requestDetails.value,
            [chat.mentoringRequestId]: null
          };
        }
      })
    );
  };

  const loadChats = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    isLoadingChats.value = true;
    error.value = null;

    try {
      chats.value = await chatApi.getChats({ page: 0, size: 100 });
      await loadLastMessages(chats.value.content);
      await loadRequestDetails(chats.value.content);

      const activeChatStillExists = chats.value.content.some((chat) => chat.id === activeChat.value?.id);

      if (activeChat.value && !activeChatStillExists) {
        activeChat.value = null;
        messages.value = null;
        optimisticMessages.value = [];
      }
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/chats");
    } finally {
      isLoadingChats.value = false;
    }
  };

  const loadMessages = async (chatId: number) => {
    isLoadingMessages.value = true;
    error.value = null;
    optimisticMessages.value = [];

    try {
      activeChat.value = await chatApi.getById(chatId);
      markChatRead(chatId);
      await loadRequestDetails([activeChat.value]);
      messages.value = await chatApi.getMessages(chatId, { page: 0, size: 80 });
      lastMessages.value = {
        ...lastMessages.value,
        [chatId]: messages.value.content[0] ?? lastMessages.value[chatId] ?? null
      };
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/chats/${chatId}`);
    } finally {
      isLoadingMessages.value = false;
    }
  };

  const openByRequestId = async () => {
    if (!form.requestId.trim()) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "FORM_ERROR",
        message: "Введите ID заявки.",
        path: "/chats/by-request/{requestId}"
      };
      return;
    }

    isOpeningByRequestId.value = true;
    error.value = null;
    optimisticMessages.value = [];

    try {
      const chat = await chatApi.getByRequestId(Number(form.requestId));
      activeChat.value = chat;
      markChatRead(chat.id);
      await loadRequestDetails([chat]);
      messages.value = await chatApi.getMessages(chat.id, { page: 0, size: 80 });

      if (!chats.value?.content.some((item) => item.id === chat.id)) {
        chats.value = {
          content: [chat, ...(chats.value?.content ?? [])],
          page: chats.value?.page ?? 0,
          size: chats.value?.size ?? 100,
          totalElements: (chats.value?.totalElements ?? 0) + 1,
          totalPages: chats.value?.totalPages ?? 1,
          last: chats.value?.last ?? true
        };
      }

      lastMessages.value = {
        ...lastMessages.value,
        [chat.id]: messages.value.content[0] ?? null
      };
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/chats/by-request/{requestId}");
    } finally {
      isOpeningByRequestId.value = false;
    }
  };

  const sendMessage = async () => {
    if (!activeChat.value) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "FORM_ERROR",
        message: "Сначала откройте чат.",
        path: "/chats/{chatId}/messages"
      };
      return;
    }

    const body = form.body.trim();
    if (!body) {
      return;
    }

    const chat = activeChat.value;
    const tempId = `temp-${Date.now()}`;
    const pendingMessage: ChatViewMessage = {
      id: -Date.now(),
      tempId,
      chatId: chat.id,
      senderUserId: authStore.user?.id ?? 0,
      body: body || null,
      attachment: null,
      createdAt: new Date().toISOString(),
      deliveryStatus: "sending"
    };

    optimisticMessages.value = [...optimisticMessages.value, pendingMessage];
    isSending.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      const sentMessage = await chatApi.sendMessage(chat.id, {
        body: body || null,
        attachmentFileId: null
      });

      optimisticMessages.value = optimisticMessages.value.map((message) =>
        message.tempId === tempId ? { ...sentMessage, deliveryStatus: "sent" } : message
      );
      lastMessages.value = {
        ...lastMessages.value,
        [chat.id]: sentMessage
      };
      form.body = "";
      setSuccess("Сообщение отправлено.");
      void loadChats();
    } catch (rawError) {
      optimisticMessages.value = optimisticMessages.value.map((message) =>
        message.tempId === tempId ? { ...message, deliveryStatus: "error" } : message
      );
      error.value = normalizeErrorResponse(rawError, `/chats/${chat.id}/messages`);
    } finally {
      isSending.value = false;
    }
  };

  const createPendingMessage = () => {
    if (!activeChat.value) {
      return null;
    }

    const body = form.body.trim();
    if (!body) {
      return null;
    }

    const pendingMessage: ChatViewMessage = {
      id: -Date.now(),
      tempId: `temp-${Date.now()}`,
      chatId: activeChat.value.id,
      senderUserId: authStore.user?.id ?? 0,
      body: body || null,
      attachment: null,
      createdAt: new Date().toISOString(),
      deliveryStatus: "sending"
    };

    optimisticMessages.value = [...optimisticMessages.value, pendingMessage];
    lastMessages.value = {
      ...lastMessages.value,
      [activeChat.value.id]: pendingMessage
    };
    form.body = "";
    isSending.value = true;
    error.value = null;
    successMessage.value = "";

    return pendingMessage;
  };

  const confirmPendingMessage = (tempId: string, sentMessage: ChatMessageResponse) => {
    optimisticMessages.value = optimisticMessages.value.map((message) =>
      message.tempId === tempId ? { ...sentMessage, tempId, deliveryStatus: "sent" } : message
    );
    lastMessages.value = {
      ...lastMessages.value,
      [sentMessage.chatId]: sentMessage
    };
    isSending.value = false;
  };

  const failPendingMessage = (tempId: string) => {
    optimisticMessages.value = optimisticMessages.value.map((message) =>
      message.tempId === tempId ? { ...message, deliveryStatus: "error" } : message
    );
    isSending.value = false;
  };

  const receiveSocketMessage = (message: ChatMessageResponse) => {
    lastMessages.value = {
      ...lastMessages.value,
      [message.chatId]: message
    };

    if (activeChat.value?.id === message.chatId) {
      const existingMessages = messages.value?.content ?? [];
      const alreadyExists = existingMessages.some((item) => item.id === message.id);

      if (!alreadyExists) {
        messages.value = toPagedResponse([...existingMessages, message]);
      }

      markChatRead(message.chatId);
      return;
    }

    markChatUnread(message.chatId);
  };

  return {
    activeChat,
    chats,
    confirmPendingMessage,
    createPendingMessage,
    error,
    failPendingMessage,
    form,
    isLoadingChats,
    isLoadingMessages,
    isOpeningByRequestId,
    isSending,
    lastMessages,
    loadChats,
    loadMessages,
    messages,
    openByRequestId,
    optimisticMessages,
    readChatIds,
    receiveSocketMessage,
    requestDetails,
    sendMessage,
    successMessage
  };
});
