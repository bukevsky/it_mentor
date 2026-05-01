<script setup lang="ts">
import { BaseButton, Status } from "conductor";
import type { ChatResponse } from "@/shared/api/contracts";

defineProps<{
  chat: ChatResponse | null;
  title: string;
  subtitle: string;
  requestGoal: string;
  socketStatusLabel: string;
  isOnline: boolean;
}>();

const emit = defineEmits<{
  (event: "open-requests"): void;
}>();
</script>

<template>
  <header class="chat-window__header">
    <div class="chat-window__heading">
      <h3 class="section-title">{{ title }}</h3>
      <p class="section-copy">
        {{ subtitle }}
        <span :class="['chat-presence-dot', { 'chat-presence-dot--online': isOnline }]"></span>
        {{ socketStatusLabel }}
      </p>
    </div>
    <div v-if="chat" class="chat-window__summary">
      <Status type="secondary" size="s" :label="`#${chat.mentoringRequestId}`" />
      <span>{{ requestGoal }}</span>
      <BaseButton
        variant="clear"
        size="s"
        label="К заявке"
        @click="emit('open-requests')"
      />
    </div>
  </header>
</template>
