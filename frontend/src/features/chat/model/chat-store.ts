import { reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  AttachmentInfo,
  ChatMessageDeliveryStatus,
  ChatMessageResponse,
  ChatResponse,
  ChatTypingPayload,
  ErrorResponse,
  MessageStatusChangedPayload,
  MentoringRequestResponse,
  PagedResponse,
  PresenceChangedPayload,
  SendMessageCommand
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { filesApi } from "@/features/files/api/files-api";
import { imageCache } from "@/features/files/model/image-cache";
import { chatApi } from "../api/chat-api";

export type ChatLocalDeliveryStatus = "sending" | "error";
export type ChatViewMessage = Omit<ChatMessageResponse, "deliveryStatus"> & {
  deliveryStatus: ChatMessageDeliveryStatus | ChatLocalDeliveryStatus;
};

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
    body: "",
    attachmentFile: null as File | null,
    attachmentFileId: null as number | null,
    uploadedAttachment: null as AttachmentInfo | null
  });

  const chats = ref<PagedResponse<ChatResponse> | null>(null);
  const activeChat = ref<ChatResponse | null>(null);
  const messages = ref<PagedResponse<ChatMessageResponse> | null>(null);
  const lastMessages = ref<Record<number, ChatViewMessage | null>>({});
  const requestDetails = ref<Record<number, MentoringRequestResponse | null>>({});
  const optimisticMessages = ref<ChatViewMessage[]>([]);
  const typingByChatId = ref<Record<number, ChatTypingPayload>>({});
  const presenceByUserId = ref<Record<number, PresenceChangedPayload>>({});
  const readChatIds = ref<Set<number>>(new Set());
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isLoadingChats = ref(false);
  const isLoadingMessages = ref(false);
  const isOpeningByRequestId = ref(false);
  const isSending = ref(false);
  const isUploadingAttachment = ref(false);
  let successTimer: ReturnType<typeof setTimeout> | null = null;

  const sortChatsByActivity = (items: ChatResponse[]) => {
    return [...items].sort((left, right) => {
      const leftDate = left.lastMessageAt ?? left.lastMessage?.createdAt ?? left.createdAt;
      const rightDate = right.lastMessageAt ?? right.lastMessage?.createdAt ?? right.createdAt;

      return new Date(rightDate).getTime() - new Date(leftDate).getTime();
    });
  };

  const upsertChat = (nextChat: ChatResponse) => {
    const currentChats = chats.value?.content ?? [];
    const exists = currentChats.some((chat) => chat.id === nextChat.id);
    const content = exists
      ? currentChats.map((chat) => (chat.id === nextChat.id ? nextChat : chat))
      : [nextChat, ...currentChats];

    chats.value = {
      content: sortChatsByActivity(content),
      page: chats.value?.page ?? 0,
      size: chats.value?.size ?? Math.max(content.length, 1),
      totalElements: exists ? chats.value?.totalElements ?? content.length : (chats.value?.totalElements ?? 0) + 1,
      totalPages: chats.value?.totalPages ?? 1,
      last: chats.value?.last ?? true
    };
  };

  const markChatRead = (chatId: number) => {
    readChatIds.value = new Set([...readChatIds.value, chatId]);
    if (chats.value) {
      chats.value = {
        ...chats.value,
        content: chats.value.content.map((chat) =>
          chat.id === chatId ? { ...chat, unreadCount: 0 } : chat
        )
      };
    }
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

  const reset = () => {
    form.requestId = "";
    form.body = "";
    chats.value = null;
    activeChat.value = null;
    messages.value = null;
    lastMessages.value = {};
    requestDetails.value = {};
    optimisticMessages.value = [];
    typingByChatId.value = {};
    presenceByUserId.value = {};
    readChatIds.value = new Set();
    error.value = null;
    successMessage.value = "";
    isLoadingChats.value = false;
    isLoadingMessages.value = false;
    isOpeningByRequestId.value = false;
    isSending.value = false;

    if (successTimer) {
      clearTimeout(successTimer);
      successTimer = null;
    }
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
      chats.value = {
        ...chats.value,
        content: sortChatsByActivity(chats.value.content)
      };
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
        upsertChat(chat);
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

  const uploadAttachment = async (file: File) => {
    form.attachmentFile = file;
    form.attachmentFileId = null;
    form.uploadedAttachment = null;
    isUploadingAttachment.value = true;
    error.value = null;

    try {
      const uploaded = await filesApi.uploadChatAttachment(file);
      form.attachmentFileId = uploaded.id;
      form.uploadedAttachment = {
        fileId: uploaded.id,
        originalFilename: uploaded.originalFilename,
        contentType: uploaded.contentType,
        size: uploaded.size
      };

      imageCache.prime(uploaded.id, file, uploaded.originalFilename);
    } catch (rawError) {
      form.attachmentFile = null;
      form.uploadedAttachment = null;
      error.value = normalizeErrorResponse(rawError, "/files/chat-attachment");
    } finally {
      isUploadingAttachment.value = false;
    }
  };

  const clearAttachment = () => {
    form.attachmentFile = null;
    form.attachmentFileId = null;
    form.uploadedAttachment = null;
    isUploadingAttachment.value = false;
  };

  const createPendingMessage = (requestId: string) => {
    if (!activeChat.value) {
      return null;
    }

    const body = form.body.trim();
    if (!body && !form.attachmentFileId) {
      return null;
    }

    const command: SendMessageCommand = {
      requestId,
      body: body || null,
      attachmentFileId: form.attachmentFileId
    };
    const pendingMessage: ChatViewMessage = {
      id: -Date.now(),
      chatId: activeChat.value.id,
      senderUserId: authStore.user?.id ?? 0,
      clientMessageId: requestId,
      body: body || null,
      attachment: form.uploadedAttachment,
      deliveredAt: null,
      readAt: null,
      createdAt: new Date().toISOString(),
      deliveryStatus: "sending"
    };

    optimisticMessages.value = [...optimisticMessages.value, pendingMessage];
    lastMessages.value = {
      ...lastMessages.value,
      [activeChat.value.id]: pendingMessage
    };
    form.body = "";
    form.attachmentFile = null;
    form.attachmentFileId = null;
    form.uploadedAttachment = null;
    isSending.value = true;
    error.value = null;
    successMessage.value = "";

    return { message: pendingMessage, command };
  };

  const confirmPendingMessage = (requestId: string, sentMessage: ChatMessageResponse) => {
    optimisticMessages.value = optimisticMessages.value.map((message) =>
      message.clientMessageId === requestId ? sentMessage : message
    );
    lastMessages.value = {
      ...lastMessages.value,
      [sentMessage.chatId]: sentMessage
    };
    isSending.value = false;
  };

  const failPendingMessage = (requestId: string, messageText = "Не удалось отправить сообщение.") => {
    optimisticMessages.value = optimisticMessages.value.map((message) =>
      message.clientMessageId === requestId ? { ...message, deliveryStatus: "error" } : message
    );
    isSending.value = false;
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "CHAT_COMMAND_ERROR",
      message: messageText,
      path: "/ws"
    };
  };

  const applyMessageStatus = (payload: MessageStatusChangedPayload) => {
    const updateMessage = <T extends ChatMessageResponse | ChatViewMessage>(message: T): T => {
      if (message.id > payload.upToMessageId || message.senderUserId === payload.actorUserId) {
        return message;
      }

      return {
        ...message,
        deliveryStatus: payload.status,
        deliveredAt: message.deliveredAt ?? payload.changedAt,
        readAt: payload.status === "READ" ? payload.changedAt : message.readAt
      } as T;
    };

    if (messages.value) {
      messages.value = {
        ...messages.value,
        content: messages.value.content.map(updateMessage)
      };
    }
    optimisticMessages.value = optimisticMessages.value.map(updateMessage);
    lastMessages.value = Object.fromEntries(
      Object.entries(lastMessages.value).map(([chatId, message]) => [
        chatId,
        message ? updateMessage(message) : null
      ])
    );
  };

  const applyTyping = (payload: ChatTypingPayload) => {
    typingByChatId.value = {
      ...typingByChatId.value,
      [payload.chatId]: payload
    };
  };

  const applyPresence = (payload: PresenceChangedPayload) => {
    presenceByUserId.value = {
      ...presenceByUserId.value,
      [payload.userId]: payload
    };
  };

  const mergeSyncedMessages = (syncedMessages: ChatMessageResponse[]) => {
    const current = messages.value?.content ?? [];
    const byId = new Map(current.map((message) => [message.id, message]));
    syncedMessages.forEach((message) => byId.set(message.id, message));
    messages.value = toPagedResponse([...byId.values()]);
    syncedMessages.forEach((message) => {
      lastMessages.value = {
        ...lastMessages.value,
        [message.chatId]: message
      };
    });
  };

  const getLastKnownMessageId = (chatId: number) => {
    const ids = [
      ...(messages.value?.content ?? []),
      ...optimisticMessages.value
    ]
      .filter((message) => message.chatId === chatId && message.id > 0)
      .map((message) => message.id);
    return ids.length ? Math.max(...ids) : 0;
  };

  const getHighestIncomingMessageId = (chatId: number) => {
    const currentUserId = authStore.user?.id;
    const ids = (messages.value?.content ?? [])
      .filter(
        (message) =>
          message.chatId === chatId && message.senderUserId !== currentUserId && message.id > 0
      )
      .map((message) => message.id);
    return ids.length ? Math.max(...ids) : null;
  };

  const getPeerUserId = (chat: ChatResponse | null = activeChat.value) => {
    if (!chat) {
      return null;
    }
    return authStore.user?.id === chat.studentUserId ? chat.mentorUserId : chat.studentUserId;
  };

  const receiveSocketMessage = (message: ChatMessageResponse) => {
    const isIncoming = message.senderUserId !== authStore.user?.id;
    const isActive = activeChat.value?.id === message.chatId;
    lastMessages.value = {
      ...lastMessages.value,
      [message.chatId]: message
    };

    const isKnownChat = chats.value?.content.some((chat) => chat.id === message.chatId) ?? false;

    if (!isKnownChat) {
      void chatApi.getById(message.chatId)
        .then((chat) => {
          upsertChat({
            ...chat,
            lastMessage: message,
            lastMessageAt: message.createdAt,
            lastSenderUserId: message.senderUserId,
            unreadCount: isIncoming && !isActive ? Math.max(chat.unreadCount, 1) : chat.unreadCount
          });
          void loadRequestDetails([chat]);
        })
        .catch(() => undefined);
    }

    if (chats.value) {
      chats.value = {
        ...chats.value,
        content: sortChatsByActivity(
          chats.value.content.map((chat) => {
            if (chat.id !== message.chatId) {
              return chat;
            }

            return {
              ...chat,
              lastMessage: message,
              lastMessageAt: message.createdAt,
              lastSenderUserId: message.senderUserId,
              unreadCount: isIncoming && !isActive ? chat.unreadCount + 1 : chat.unreadCount
            };
          })
        )
      };
    }

    if (activeChat.value?.id === message.chatId) {
      const existingMessages = messages.value?.content ?? [];
      const alreadyExists = existingMessages.some((item) => item.id === message.id);
      const optimisticMatchIndex = optimisticMessages.value.findIndex(
        (item) =>
          item.id === message.id ||
          (item.chatId === message.chatId && item.clientMessageId === message.clientMessageId)
      );

      if (optimisticMatchIndex >= 0) {
        optimisticMessages.value = optimisticMessages.value.map((item, index) =>
          index === optimisticMatchIndex ? message : item
        );
        isSending.value = false;
        setSuccess("Сообщение отправлено.");
      }

      if (!alreadyExists && optimisticMatchIndex < 0) {
        messages.value = toPagedResponse([...existingMessages, message]);
      }

      markChatRead(message.chatId);
      return { delivered: isIncoming, read: isIncoming };
    }

    markChatUnread(message.chatId);
    return { delivered: isIncoming, read: false };
  };

  return {
    activeChat,
    applyMessageStatus,
    applyPresence,
    applyTyping,
    chats,
    clearAttachment,
    confirmPendingMessage,
    createPendingMessage,
    error,
    failPendingMessage,
    form,
    getHighestIncomingMessageId,
    getLastKnownMessageId,
    getPeerUserId,
    isLoadingChats,
    isLoadingMessages,
    isOpeningByRequestId,
    isSending,
    isUploadingAttachment,
    lastMessages,
    loadChats,
    loadMessages,
    markChatRead,
    messages,
    mergeSyncedMessages,
    openByRequestId,
    optimisticMessages,
    presenceByUserId,
    readChatIds,
    receiveSocketMessage,
    requestDetails,
    reset,
    successMessage,
    typingByChatId,
    uploadAttachment
  };
});
