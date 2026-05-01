<script setup lang="ts">
import type { ChatViewMessage } from "@/features/chat/model/chat-store";
import ChatMessageBubble from "./ChatMessageBubble.vue";

const props = defineProps<{
  messages: ChatViewMessage[];
  currentUserId?: number;
  isLoading: boolean;
  peerName: string;
}>();

const emit = defineEmits<{
  (event: "scroll-state", isNearBottom: boolean): void;
}>();

const dateLabel = (value: string) => {
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric"
  }).format(new Date(value));
};

const shouldShowDate = (message: ChatViewMessage, index: number) => {
  const previous = props.messages[index - 1];

  if (!previous) {
    return true;
  }

  return dateLabel(previous.createdAt) !== dateLabel(message.createdAt);
};

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement;
  emit("scroll-state", target.scrollHeight - target.scrollTop - target.clientHeight < 80);
};
</script>

<template>
  <div class="chat-thread-feed" @scroll="handleScroll">
    <div v-if="isLoading" class="chat-thread-feed__stack">
      <div v-for="index in 5" :key="index" class="chat-message-skeleton"></div>
    </div>

    <div v-else-if="messages.length" class="chat-thread-feed__stack">
      <template v-for="(message, index) in messages" :key="message.tempId ?? message.id">
        <div v-if="shouldShowDate(message, index)" class="chat-date-separator">
          {{ dateLabel(message.createdAt) }}
        </div>

        <ChatMessageBubble
          :message="message"
          :current-user-id="currentUserId"
          :peer-name="peerName"
        />
      </template>
    </div>

    <div v-else class="empty-state chat-thread-feed__empty">
      <div>
        <h3 class="section-title">Сообщений пока нет</h3>
        <p class="section-copy">Напишите первое сообщение по выбранной заявке.</p>
      </div>
    </div>
  </div>
</template>
