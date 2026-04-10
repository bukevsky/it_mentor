import { reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ChatMessageResponse,
  ChatResponse,
  ErrorResponse,
  PagedResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { chatApi } from "../api/chat-api";

export const useChatStore = defineStore("chat", () => {
  const authStore = useAuthStore();

  const form = reactive({
    requestId: "",
    attachmentFileId: "",
    body: "Здравствуйте! Предлагаю обсудить следующий шаг по заявке."
  });

  const chats = ref<PagedResponse<ChatResponse> | null>(null);
  const activeChat = ref<ChatResponse | null>(null);
  const messages = ref<PagedResponse<ChatMessageResponse> | null>(null);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isBusy = ref(false);

  const loadChats = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    isBusy.value = true;
    error.value = null;

    try {
      chats.value = await chatApi.getChats({ page: 0, size: 20 });
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/chats");
    } finally {
      isBusy.value = false;
    }
  };

  const loadMessages = async (chatId: number) => {
    isBusy.value = true;
    error.value = null;

    try {
      activeChat.value = await chatApi.getById(chatId);
      messages.value = await chatApi.getMessages(chatId, { page: 0, size: 40 });
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/chats/${chatId}`);
    } finally {
      isBusy.value = false;
    }
  };

  const openByRequestId = async () => {
    if (!form.requestId) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "FORM_ERROR",
        message: "Введите ID заявки.",
        path: "/chats/by-request/{requestId}"
      };
      return;
    }

    isBusy.value = true;
    error.value = null;

    try {
      const chat = await chatApi.getByRequestId(Number(form.requestId));
      activeChat.value = chat;
      messages.value = await chatApi.getMessages(chat.id, { page: 0, size: 40 });
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/chats/by-request/{requestId}");
    } finally {
      isBusy.value = false;
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

    isBusy.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      await chatApi.sendMessage(activeChat.value.id, {
        body: form.body || null,
        attachmentFileId: form.attachmentFileId ? Number(form.attachmentFileId) : null
      });
      successMessage.value = "Сообщение отправлено.";
      messages.value = await chatApi.getMessages(activeChat.value.id, { page: 0, size: 40 });
      form.body = "";
      form.attachmentFileId = "";
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/chats/${activeChat.value.id}/messages`);
    } finally {
      isBusy.value = false;
    }
  };

  return {
    activeChat,
    chats,
    error,
    form,
    isBusy,
    loadChats,
    loadMessages,
    messages,
    openByRequestId,
    sendMessage,
    successMessage
  };
});
