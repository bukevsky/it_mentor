<script setup lang="ts">
import { BaseButton, BaseInput } from "conductor";
import type { ChatDialogItem } from "@/features/chat/model/use-chat";
import ChatItem from "./ChatItem.vue";

const props = defineProps<{
  items: ChatDialogItem[];
  searchQuery: string;
  isLoading: boolean;
  hasLoaded: boolean;
  hasAnyChats: boolean;
}>();

const emit = defineEmits<{
  (event: "update:searchQuery", value: string): void;
  (event: "open", chatId: number): void;
  (event: "open-requests"): void;
}>();
</script>

<template>
  <aside class="app-panel chat-sidebar-panel">
    <div class="chat-sidebar-search">
      <BaseInput
        :model-value="props.searchQuery"
        title="Поиск диалога"
        placeholder="Имя, роль или заявка"
        @update:model-value="emit('update:searchQuery', String($event))"
      />
    </div>

    <div v-if="isLoading && !hasLoaded" class="chat-sidebar-list">
      <div v-for="index in 4" :key="index" class="chat-sidebar-skeleton"></div>
    </div>

    <div v-else-if="items.length" class="chat-sidebar-list">
      <ChatItem
        v-for="item in items"
        :key="item.chat.id"
        :item="item"
        @open="emit('open', $event)"
      />
    </div>

    <div v-else-if="hasAnyChats" class="empty-state chat-sidebar-empty chat-sidebar-empty--compact">
      По этому поиску диалогов нет.
    </div>

    <div v-else class="empty-state chat-sidebar-empty">
      <div>
        <h3 class="section-title">Диалогов пока нет</h3>
        <p class="section-copy">
          Чат появится после принятия заявки. Перейдите к заявкам, чтобы открыть активные обращения.
        </p>
        <div class="base-actions mt-4">
          <BaseButton size="m" label="Открыть заявки" @click="emit('open-requests')" />
        </div>
      </div>
    </div>
  </aside>
</template>
