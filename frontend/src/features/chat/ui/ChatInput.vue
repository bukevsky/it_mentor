<script setup lang="ts">
import { nextTick, ref, watch } from "vue";

const props = defineProps<{
  body: string;
  attachmentFile: File | null;
  isUploadingAttachment: boolean;
  attachmentUploaded: boolean;
  disabled: boolean;
  isSending: boolean;
}>();

const emit = defineEmits<{
  (event: "update:body", value: string): void;
  (event: "update:attachmentFile", value: File | null): void;
  (event: "send"): void;
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const fileExt = (file: File) => {
  const parts = file.name.split(".");
  return parts.length > 1 ? parts[parts.length - 1].toUpperCase() : "FILE";
};

const resizeTextarea = async () => {
  await nextTick();
  if (!textareaRef.value) return;
  textareaRef.value.style.height = "auto";
  textareaRef.value.style.height = `${Math.min(textareaRef.value.scrollHeight, 112)}px`;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key !== "Enter" || event.shiftKey || event.isComposing) return;

  event.preventDefault();
  if (!props.disabled && !props.isSending) {
    emit("send");
    void nextTick(() => textareaRef.value?.focus());
  }
};

const onFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null;
  emit("update:attachmentFile", file);
  if (fileInputRef.value) fileInputRef.value.value = "";
};

const openFileDialog = (event: MouseEvent) => {
  (event.currentTarget as HTMLButtonElement | null)?.blur();
  fileInputRef.value?.click();
};

const submitFromButton = (event: MouseEvent) => {
  (event.currentTarget as HTMLButtonElement | null)?.blur();
  emit("send");
  void nextTick(() => textareaRef.value?.focus());
};

const removeAttachment = () => {
  emit("update:attachmentFile", null);
};

watch(() => props.body, () => { void resizeTextarea(); }, { immediate: true });
</script>

<template>
  <div class="chat-composer-card">
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*,application/pdf,text/plain,application/zip,.doc,.docx,.xls,.xlsx"
      class="chat-composer-card__file-input"
      @change="onFileChange"
    />

    <div v-if="attachmentFile" class="chat-composer-card__attachments">
      <div
        class="chat-attachment-tile"
        :class="{
          'chat-attachment-tile--uploading': isUploadingAttachment,
          'chat-attachment-tile--done': attachmentUploaded
        }"
      >
        <span class="chat-attachment-tile__ext">{{ fileExt(attachmentFile) }}</span>
        <span class="chat-attachment-tile__size">{{ formatFileSize(attachmentFile.size) }}</span>
        <div v-if="isUploadingAttachment" class="chat-attachment-tile__progress">
          <div class="chat-attachment-tile__progress-bar"></div>
        </div>
        <button
          type="button"
          class="chat-attachment-tile__remove"
          aria-label="Удалить вложение"
          @click="removeAttachment"
        >×</button>
      </div>
    </div>

    <div class="chat-composer-card__row">
      <button
        type="button"
        class="chat-composer-card__attach-btn"
        aria-label="Прикрепить изображение"
        @click="openFileDialog"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="18" height="18">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>

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

      <button
        type="button"
        class="chat-composer-card__send-btn"
        :disabled="disabled || isSending"
        aria-label="Отправить"
        @click="submitFromButton"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
          <line x1="12" y1="19" x2="12" y2="5"/>
          <polyline points="5 12 12 5 19 12"/>
        </svg>
      </button>
    </div>
  </div>
</template>
