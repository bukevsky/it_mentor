<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { BaseButton } from "conductor";

const props = defineProps<{
  body: string;
  disabled: boolean;
  isSending: boolean;
}>();

const emit = defineEmits<{
  (event: "update:body", value: string): void;
  (event: "send"): void;
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);

const resizeTextarea = async () => {
  await nextTick();

  if (!textareaRef.value) {
    return;
  }

  textareaRef.value.style.height = "auto";
  textareaRef.value.style.height = `${Math.min(textareaRef.value.scrollHeight, 112)}px`;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key !== "Enter" || event.shiftKey) {
    return;
  }

  event.preventDefault();

  if (!props.disabled) {
    emit("send");
  }
};

watch(
  () => props.body,
  () => {
    void resizeTextarea();
  },
  { immediate: true }
);
</script>

<template>
  <div class="chat-composer-card">
    <label class="chat-composer-card__field">
      <textarea
        ref="textareaRef"
        :value="body"
        rows="1"
        placeholder="Сообщение"
        @input="emit('update:body', ($event.target as HTMLTextAreaElement).value)"
        @keydown="handleKeydown"
      ></textarea>
    </label>
    <div class="chat-composer-card__actions">
      <BaseButton
        size="m"
        label="Отправить"
        :disabled="disabled"
        :loading="isSending"
        @click="emit('send')"
      />
    </div>
  </div>
</template>
