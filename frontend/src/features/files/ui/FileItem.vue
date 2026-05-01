<script setup lang="ts">
import { ref } from "vue";
import type { ManagedFile } from "@/features/files/model/files-store";
import { formatDateTime } from "@/shared/lib/presenters";
import { formatFileSize, getFileKindLabel } from "@/features/files/model/use-files";
import FilePreview from "./FilePreview.vue";

const props = defineProps<{
  file: ManagedFile;
}>();

const emit = defineEmits<{
  (event: "download", fileId: string): void;
  (event: "delete", fileId: string): void;
  (event: "replace", fileId: string, file: File): void;
  (event: "retry", fileId: string): void;
}>();

const replaceInputRef = ref<HTMLInputElement | null>(null);

const statusLabel = () => {
  if (props.file.status === "uploading") {
    return "Загрузка";
  }

  if (props.file.status === "error") {
    return "Ошибка";
  }

  return "Загружен";
};

const openReplaceDialog = () => {
  replaceInputRef.value?.click();
};

const handleReplace = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (file) {
    emit("replace", props.file.id, file);
  }

  input.value = "";
};
</script>

<template>
  <article :class="['file-item', `file-item--${file.status}`]">
    <FilePreview :file="file" />

    <div class="file-item__main">
      <div class="file-item__title-row">
        <strong class="file-item__name">{{ file.name }}</strong>
        <span :class="['file-status', `file-status--${file.status}`]">{{ statusLabel() }}</span>
      </div>
      <div class="file-item__meta">
        <span>{{ getFileKindLabel(file.kind) }}</span>
        <span>{{ formatFileSize(file.size) }}</span>
        <span>{{ formatDateTime(file.uploadedAt) }}</span>
      </div>
      <div v-if="file.status === 'uploading'" class="file-progress">
        <span :style="{ width: `${file.progress}%` }"></span>
      </div>
      <p v-if="file.errorMessage" class="file-item__error">{{ file.errorMessage }}</p>
    </div>

    <div class="file-item__actions">
      <button type="button" class="file-action" @click="emit('download', file.id)">
        Скачать
      </button>
      <button type="button" class="file-action" @click="openReplaceDialog">
        Заменить
      </button>
      <button
        v-if="file.status === 'error'"
        type="button"
        class="file-action"
        @click="emit('retry', file.id)"
      >
        Повторить
      </button>
      <button type="button" class="file-action file-action--danger" @click="emit('delete', file.id)">
        Удалить
      </button>
      <input ref="replaceInputRef" class="hidden-file-input" type="file" @change="handleReplace" />
    </div>
  </article>
</template>
