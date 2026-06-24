import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import type { ChatResponse } from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { mentoringTypeOptions } from "@/shared/lib/options";
import { fullName, getOptionLabel } from "@/shared/lib/presenters";
import { useChatStore, type ChatViewMessage } from "./chat-store";

export type Chat = ChatResponse;
export type Message = ChatViewMessage;

export interface ChatDialogItem {
  chat: ChatResponse;
  title: string;
  subtitle: string;
  peerName: string;
  peerRole: string;
  requestGoal: string;
  preview: string;
  lastActivityAt: string;
  unreadCount: number;
  isActive: boolean;
}

const normalize = (value: string) => value.toLowerCase().trim();

export const useChat = () => {
  const authStore = useAuthStore();
  const chatStore = useChatStore();

  const { user } = storeToRefs(authStore);
  const {
    activeChat,
    chats,
    form,
    isLoadingChats,
    isLoadingMessages,
    isOpeningByRequestId,
    isSending,
    isUploadingAttachment,
    lastMessages,
    messages,
    optimisticMessages,
    readChatIds,
    requestDetails
  } = storeToRefs(chatStore);

  const searchQuery = ref("");
  const debouncedSearchQuery = ref("");
  let searchTimer: ReturnType<typeof setTimeout> | null = null;

  watch(
    () => searchQuery.value,
    (nextQuery) => {
      if (searchTimer) {
        clearTimeout(searchTimer);
      }

      searchTimer = setTimeout(() => {
        debouncedSearchQuery.value = nextQuery;
      }, 250);
    },
    { immediate: true }
  );

  const getPeerMeta = (chat: ChatResponse) => {
    const request = requestDetails.value[chat.mentoringRequestId];

    if (user.value?.id === chat.studentUserId) {
      return {
        name: request ? fullName(request.mentorProfile) : `ID ${chat.mentorUserId}`,
        role: "Ментор",
        label: `Ментор · ${request ? fullName(request.mentorProfile) : `ID ${chat.mentorUserId}`}`
      };
    }

    if (user.value?.id === chat.mentorUserId) {
      return {
        name: request ? fullName(request.studentProfile) : `ID ${chat.studentUserId}`,
        role: "Студент",
        label: `Студент · ${request ? fullName(request.studentProfile) : `ID ${chat.studentUserId}`}`
      };
    }

    return {
      name: `Чат #${chat.id}`,
      role: "Диалог",
      label: `Студент ID ${chat.studentUserId} · Ментор ID ${chat.mentorUserId}`
    };
  };

  const getPeerLabel = (chat: ChatResponse) => {
    return getPeerMeta(chat).label;
  };

  const getRequestGoal = (chat: ChatResponse) => {
    const request = requestDetails.value[chat.mentoringRequestId];
    return getOptionLabel(mentoringTypeOptions, request?.goalType, "Цель не указана");
  };

  const getDialogPreview = (chat: ChatResponse) => {
    const lastMessage = lastMessages.value[chat.id];

    if (!lastMessage) {
      return "Сообщений пока нет";
    }

    if (lastMessage.body?.trim()) {
      return lastMessage.body.trim();
    }

    if (lastMessage.attachment) {
      return `Вложение: ${lastMessage.attachment.originalFilename}`;
    }

    return "Сообщение без текста";
  };

  const dialogItems = computed<ChatDialogItem[]>(() => {
    const query = normalize(debouncedSearchQuery.value);

    return [...(chats.value?.content ?? [])]
      .map((chat) => {
        const lastMessage = lastMessages.value[chat.id];
        const peer = getPeerMeta(chat);
        const title = peer.name;
        const subtitle = `${peer.role} · заявка #${chat.mentoringRequestId}`;
        const preview = getDialogPreview(chat);
        const unreadCount = readChatIds.value.has(chat.id) ? 0 : chat.unreadCount;

        return {
          chat,
          title,
          subtitle,
          peerName: peer.name,
          peerRole: peer.role,
          requestGoal: getRequestGoal(chat),
          preview,
          lastActivityAt: lastMessage?.createdAt ?? chat.createdAt,
          unreadCount,
          isActive: activeChat.value?.id === chat.id
        };
      })
      .filter((item) => {
        if (!query) {
          return true;
        }

        return normalize(
          `${item.title} ${item.subtitle} ${item.requestGoal} ${item.preview} ${item.chat.id} ${item.chat.mentoringRequestId}`
        ).includes(query);
      })
      .sort(
        (left, right) =>
          new Date(right.lastActivityAt).getTime() - new Date(left.lastActivityAt).getTime()
      );
  });

  const orderedMessages = computed<ChatViewMessage[]>(() => {
    const serverMessages = [...(messages.value?.content ?? [])];

    return [...serverMessages, ...optimisticMessages.value]
      .filter((message) => message.chatId === activeChat.value?.id)
      .sort(
        (left, right) =>
          new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime()
      );
  });

  const selectedChatTitle = computed(() => {
    return activeChat.value ? getPeerMeta(activeChat.value).name : "Диалог не выбран";
  });

  const selectedChatSubtitle = computed(() => {
    if (!activeChat.value) {
      return "Выберите диалог слева.";
    }

    const peer = getPeerMeta(activeChat.value);
    return `${peer.role} · заявка #${activeChat.value.mentoringRequestId}`;
  });

  const activeRequestGoal = computed(() => {
    return activeChat.value ? getRequestGoal(activeChat.value) : "";
  });

  const canSendMessage = computed(() => {
    return Boolean(
      activeChat.value &&
      !isSending.value &&
      !isUploadingAttachment.value &&
      (form.value.body.trim().length > 0 || form.value.attachmentFileId)
    );
  });

  const hasLoadedChats = computed(() => Boolean(chats.value));
  const hasAnyChats = computed(() => Boolean(chats.value?.content.length));
  const isChatBusy = computed(
    () => isLoadingChats.value || isLoadingMessages.value || isOpeningByRequestId.value || isSending.value
  );

  return {
    activeChat,
    activeRequestGoal,
    canSendMessage,
    dialogItems,
    form,
    getPeerMeta,
    getPeerLabel,
    hasAnyChats,
    hasLoadedChats,
    isChatBusy,
    orderedMessages,
    searchQuery,
    selectedChatSubtitle,
    selectedChatTitle
  };
};
