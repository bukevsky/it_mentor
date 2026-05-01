<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { BaseButton } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useChatStore } from "@/features/chat/model/chat-store";
import { useChat } from "@/features/chat/model/use-chat";
import { useChatSocket } from "@/features/chat/model/use-chat-socket";
import ChatHeader from "@/features/chat/ui/ChatHeader.vue";
import ChatInput from "@/features/chat/ui/ChatInput.vue";
import ChatList from "@/features/chat/ui/ChatList.vue";
import ChatMessages from "@/features/chat/ui/ChatMessages.vue";

defineProps<{
  sidebarToggleLabel?: string;
  sidebarToggleIcon?: string;
  isSidebarVisible?: boolean;
}>();

const emit = defineEmits<{
  (event: "toggle-sidebar"): void;
}>();

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const { isAuthenticated, user } = storeToRefs(authStore);
const {
  error,
  isLoadingChats,
  isLoadingMessages,
  isSending,
  successMessage
} = storeToRefs(chatStore);
const {
  activeChat,
  activeRequestGoal,
  canSendMessage,
  dialogItems,
  form,
  hasAnyChats,
  hasLoadedChats,
  orderedMessages,
  searchQuery,
  selectedChatSubtitle,
  selectedChatTitle
} = useChat();
const chatSocket = useChatSocket();

const chatBodyRef = ref<HTMLElement | null>(null);
const isThreadNearBottom = ref(true);
const hasNewMessagesBelow = ref(false);

const openRequests = () => {
  void router.push({ name: "requests" });
};

const getThreadFeed = () => chatBodyRef.value?.querySelector<HTMLElement>(".chat-thread-feed") ?? null;

const updateThreadPosition = () => {
  const feed = getThreadFeed();

  if (!feed) {
    isThreadNearBottom.value = true;
    return;
  }

  isThreadNearBottom.value = feed.scrollHeight - feed.scrollTop - feed.clientHeight < 80;

  if (isThreadNearBottom.value) {
    hasNewMessagesBelow.value = false;
  }
};

const scrollThreadToBottom = async () => {
  await nextTick();

  const feed = getThreadFeed();
  if (!feed) {
    return;
  }

  feed.scrollTop = feed.scrollHeight;
  updateThreadPosition();
};

const handleSendMessage = () => {
  void chatSocket.sendActiveMessage();
  void scrollThreadToBottom();
};

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void chatStore.loadChats();
    void chatSocket.connect();
  },
  { immediate: true }
);

watch(
  () => [activeChat.value?.id, orderedMessages.value.length],
  ([nextChatId], [previousChatId]) => {
    if (nextChatId !== previousChatId || isThreadNearBottom.value) {
      void scrollThreadToBottom();
      return;
    }

    hasNewMessagesBelow.value = true;
  }
);

watch(
  () => activeChat.value?.id,
  (nextChatId) => {
    if (nextChatId) {
      chatSocket.subscribe(nextChatId);
    }
  }
);
</script>

<template>
  <section class="app-section chat-page">
    <div v-if="!isAuthenticated" class="empty-state">
      Для работы с чатами нужно войти в систему.
    </div>

    <template v-else>
      <div v-if="successMessage" class="chat-toast success-state">
        {{ successMessage }}
      </div>

      <div class="chat-workspace">
        <aside class="chat-left-column">
          <div class="chat-sidebar-toolbar">
            <BaseButton
              variant="secondary"
              size="m"
              :start-icon="sidebarToggleIcon"
              :label="sidebarToggleLabel || 'Скрыть меню'"
              @click="emit('toggle-sidebar')"
            />
          </div>

          <ChatList
            :items="dialogItems"
            :search-query="searchQuery"
            :is-loading="isLoadingChats"
            :has-loaded="hasLoadedChats"
            :has-any-chats="hasAnyChats"
            @update:search-query="searchQuery = $event"
            @open="chatStore.loadMessages"
            @open-requests="openRequests"
          />
        </aside>

        <article class="app-panel chat-window-panel" :class="{ 'chat-window-panel--empty': !activeChat }">
          <ChatHeader
            v-if="activeChat"
            :chat="activeChat"
            :title="selectedChatTitle"
            :subtitle="selectedChatSubtitle"
            :request-goal="activeRequestGoal"
            :socket-status-label="chatSocket.statusLabel.value"
            :is-online="chatSocket.status.value === 'connected'"
            @open-requests="openRequests"
          />

          <div v-if="error" class="error-state mt-4">
            {{ error.message }}
          </div>

          <div v-if="activeChat" ref="chatBodyRef" class="chat-window__body">
            <ChatMessages
              :messages="orderedMessages"
              :current-user-id="user?.id"
              :is-loading="isLoadingMessages"
              :peer-name="selectedChatTitle"
              @scroll-state="isThreadNearBottom = $event"
            />
            <button
              v-if="hasNewMessagesBelow"
              type="button"
              class="chat-new-messages-button"
              @click="scrollThreadToBottom"
            >
              Новые сообщения
            </button>

            <ChatInput
              :body="form.body"
              :disabled="!canSendMessage"
              :is-sending="isSending"
              @update:body="form.body = $event"
              @send="handleSendMessage"
            />
          </div>

          <div v-else class="chat-window__empty">
            <div class="empty-state">
              <h3 class="section-title">Выберите диалог</h3>
              <p class="section-copy">
                Слева показаны чаты по принятым заявкам. Если списка нет, откройте заявки и обработайте входящие обращения.
              </p>
              <div class="base-actions mt-4">
                <BaseButton size="m" label="Открыть заявки" @click="openRequests" />
              </div>
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>
