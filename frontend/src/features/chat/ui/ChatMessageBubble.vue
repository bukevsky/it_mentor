<script setup lang="ts">
import type { ChatViewMessage } from "@/features/chat/model/chat-store";

const props = defineProps<{
  message: ChatViewMessage;
  currentUserId?: number;
  peerName: string;
}>();

const timeLabel = (value: string) => {
  return new Intl.DateTimeFormat("ru-RU", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
};

const deliveryLabel = () => {
  if (props.message.deliveryStatus === "sending") {
    return "sending";
  }

  if (props.message.deliveryStatus === "error") {
    return "error";
  }

  if (props.message.senderUserId === props.currentUserId) {
    return "sent";
  }

  return "";
};
</script>

<template>
  <article
    :class="[
      'chat-bubble',
      {
        'chat-bubble--mine': message.senderUserId === currentUserId,
        'chat-bubble--error': message.deliveryStatus === 'error',
        'chat-bubble--sending': message.deliveryStatus === 'sending'
      }
    ]"
  >
    <strong v-if="message.senderUserId !== currentUserId" class="chat-bubble__author">
      {{ peerName }}
    </strong>
    <div class="chat-bubble__body">
      {{ message.body || "Сообщение без текста" }}
    </div>
    <div v-if="message.attachment" class="chat-bubble__attachment">
      Вложение: {{ message.attachment.originalFilename }}
    </div>
    <div class="chat-bubble__footer">
      <span>{{ timeLabel(message.createdAt) }}</span>
      <span v-if="deliveryLabel()" class="chat-bubble__status">{{ deliveryLabel() }}</span>
    </div>
  </article>
</template>
