<script setup lang="ts">
import type { ChatDialogItem } from "@/features/chat/model/use-chat";
import { formatDateTime } from "@/shared/lib/presenters";

defineProps<{
  item: ChatDialogItem;
}>();

const emit = defineEmits<{
  (event: "open", chatId: number): void;
}>();
</script>

<template>
  <button
    type="button"
    :class="['chat-sidebar-item', { 'chat-sidebar-item--active': item.isActive }]"
    @click="emit('open', item.chat.id)"
  >
    <div class="chat-sidebar-item__top">
      <strong>{{ item.peerName }}</strong>
      <span>{{ formatDateTime(item.lastActivityAt) }}</span>
    </div>
    <div class="chat-sidebar-item__title">
      <span>{{ item.peerRole }} · заявка #{{ item.chat.mentoringRequestId }}</span>
      <span v-if="item.unreadCount" class="chat-unread-dot">{{ item.unreadCount }}</span>
    </div>
    <p class="chat-sidebar-item__goal">{{ item.requestGoal }}</p>
    <p class="chat-sidebar-item__preview">{{ item.preview }}</p>
  </button>
</template>
