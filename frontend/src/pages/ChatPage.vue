<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, Status } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useChatStore } from "@/features/chat/model/chat-store";
import { formatDateTime } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const chatStore = useChatStore();

const { isAuthenticated, user } = storeToRefs(authStore);
const { activeChat, chats, error, form, isBusy, messages, successMessage } = storeToRefs(chatStore);
const threadRef = ref<HTMLElement | null>(null);

const chatItems = computed(() => {
  return [...(chats.value?.content ?? [])].sort(
    (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  );
});

const orderedMessages = computed(() => {
  return [...(messages.value?.content ?? [])].sort(
    (left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime()
  );
});

const selectedChatTitle = computed(() => {
  return activeChat.value ? `Заявка #${activeChat.value.mentoringRequestId}` : "Выберите диалог";
});

const selectedChatSubtitle = computed(() => {
  if (!activeChat.value) {
    return "Откройте чат из списка слева или найдите его по ID заявки.";
  }

  return getPeerLabel(activeChat.value);
});

const activeChatCreatedAt = computed(() => {
  return activeChat.value ? formatDateTime(activeChat.value.createdAt) : "";
});

const activeChatRequestId = computed(() => {
  return activeChat.value ? `#${activeChat.value.mentoringRequestId}` : "Не выбран";
});

const canSendMessage = computed(() => {
  return Boolean(
    activeChat.value &&
    (form.value.body.trim().length > 0 || form.value.attachmentFileId.trim().length > 0)
  );
});

const scrollThreadToBottom = async () => {
  await nextTick();

  if (!threadRef.value) {
    return;
  }

  threadRef.value.scrollTop = threadRef.value.scrollHeight;
};

function getPeerLabel(chat: { studentUserId: number; mentorUserId: number }) {
  if (user.value?.id === chat.studentUserId) {
    return `Ментор · ID ${chat.mentorUserId}`;
  }

  if (user.value?.id === chat.mentorUserId) {
    return `Студент · ID ${chat.studentUserId}`;
  }

  return `Студент ID ${chat.studentUserId} · Ментор ID ${chat.mentorUserId}`;
}

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void chatStore.loadChats();
  },
  { immediate: true }
);

watch(
  () => orderedMessages.value,
  () => {
    void scrollThreadToBottom();
  },
  { deep: true }
);
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для работы с чатами нужно войти в систему.
    </div>

    <template v-else>
      <div class="chat-workspace">
        <aside class="app-panel chat-sidebar-panel">
          <div class="chat-sidebar-panel__head">
            <div>
              <p class="section-kicker">Чаты</p>
              <h3 class="section-title">Диалоги</h3>
            </div>
            <BaseButton
              variant="secondary"
              size="m"
              :label="isBusy ? 'Обновление...' : 'Обновить'"
              :loading="isBusy"
              @click="chatStore.loadChats"
            />
          </div>

          <div class="chat-sidebar-search">
            <BaseInput v-model="form.requestId" title="Открыть по ID заявки" placeholder="Например, 12" />
            <BaseButton
              variant="secondary"
              size="l"
              label="Найти чат"
              :disabled="isBusy"
              @click="chatStore.openByRequestId"
            />
          </div>

          <div v-if="chatItems.length" class="chat-sidebar-list">
            <button
              v-for="chat in chatItems"
              :key="chat.id"
              type="button"
              :class="['chat-sidebar-item', { 'chat-sidebar-item--active': activeChat?.id === chat.id }]"
              @click="chatStore.loadMessages(chat.id)"
            >
              <div class="chat-sidebar-item__top">
                <strong>Заявка #{{ chat.mentoringRequestId }}</strong>
                <span>{{ formatDateTime(chat.createdAt) }}</span>
              </div>
              <div class="chat-sidebar-item__title">{{ getPeerLabel(chat) }}</div>
              <div class="chat-sidebar-item__meta">
                <span>Чат #{{ chat.id }}</span>
                <span v-if="activeChat?.id === chat.id">Открыт сейчас</span>
              </div>
            </button>
          </div>

          <div v-else class="empty-state chat-sidebar-empty">
            Чаты пока не найдены.
          </div>
        </aside>

        <article class="app-panel chat-window-panel">
          <header class="chat-window__header">
            <div class="chat-window__heading">
              <p class="section-kicker">Активный диалог</p>
              <h3 class="section-title">{{ selectedChatTitle }}</h3>
              <p class="section-copy">{{ selectedChatSubtitle }}</p>
            </div>
            <div v-if="activeChat" class="chat-window__summary">
              <Status type="secondary" size="s" :label="`Заявка ${activeChatRequestId}`" />
              <span>Открыт {{ activeChatCreatedAt }}</span>
            </div>
          </header>

          <div v-if="error" class="error-state mt-4">
            {{ error.message }}
          </div>
          <div v-if="successMessage" class="success-state mt-4">
            {{ successMessage }}
          </div>

          <div v-if="activeChat" class="chat-window__body">
            <div ref="threadRef" class="chat-thread-feed">
              <div v-if="orderedMessages.length" class="chat-thread-feed__stack">
                <article
                  v-for="message in orderedMessages"
                  :key="message.id"
                  :class="['chat-bubble', { 'chat-bubble--mine': message.senderUserId === user?.id }]"
                >
                  <div class="chat-bubble__meta">
                    <strong>{{ message.senderUserId === user?.id ? 'Вы' : `ID ${message.senderUserId}` }}</strong>
                    <span>{{ formatDateTime(message.createdAt) }}</span>
                  </div>
                  <div class="chat-bubble__body">
                    {{ message.body || "Сообщение без текста" }}
                  </div>
                  <div v-if="message.attachment" class="chat-bubble__attachment">
                    Вложение: {{ message.attachment.originalFilename }}
                  </div>
                </article>
              </div>

              <div v-else class="empty-state chat-thread-feed__empty">
                В этом диалоге пока нет сообщений.
              </div>
            </div>

            <div class="chat-composer-card">
              <BaseInput
                v-model="form.attachmentFileId"
                title="ID вложения"
                placeholder="Опционально, из раздела Файлы"
              />
              <label class="chat-composer-card__field">
                <span>Сообщение</span>
                <textarea
                  v-model="form.body"
                  placeholder="Напишите сообщение по выбранной заявке."
                ></textarea>
              </label>
              <div class="chat-composer-card__actions">
                <BaseButton
                  size="l"
                  label="Отправить"
                  :disabled="isBusy || !canSendMessage"
                  :loading="isBusy"
                  @click="chatStore.sendMessage"
                />
              </div>
            </div>
          </div>

          <div v-else class="chat-window__empty">
            <div class="empty-state">
              Выберите диалог слева или откройте чат по ID заявки.
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>
