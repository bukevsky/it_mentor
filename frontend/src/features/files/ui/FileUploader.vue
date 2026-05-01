<script setup lang="ts">
import { ref } from "vue";
import { BaseButton } from "conductor";
import type { ManagedFile } from "@/features/files/model/files-store";
import type { FileCategory } from "@/features/files/model/use-files";
import { formatFileSize } from "@/features/files/model/use-files";

const props = defineProps<{
  category: FileCategory;
  currentUpload: ManagedFile | null;
  isUploading: boolean;
}>();

const emit = defineEmits<{
  (event: "upload", file: File): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);
const isDragActive = ref(false);

const openFilePicker = () => {
  inputRef.value?.click();
};

const uploadFile = (file?: File) => {
  if (!file) {
    return;
  }

  emit("upload", file);
};

const handleInput = (event: Event) => {
  const input = event.target as HTMLInputElement;
  uploadFile(input.files?.[0]);
  input.value = "";
};

const handleDrop = (event: DragEvent) => {
  isDragActive.value = false;
  uploadFile(event.dataTransfer?.files?.[0]);
};
</script>

<template>
  <section
    :class="['file-uploader', { 'file-uploader--active': isDragActive }]"
    @dragover.prevent="isDragActive = true"
    @dragleave.prevent="isDragActive = false"
    @drop.prevent="handleDrop"
  >
    <input
      ref="inputRef"
      class="hidden-file-input"
      type="file"
      :accept="props.category.accept"
      @change="handleInput"
    />

    <div class="file-uploader__content">
      <div class="file-uploader__icon">+</div>
      <div>
        <h3 class="section-title">Загрузка файла</h3>
        <p class="section-copy">
          {{ category.description }}
        </p>
      </div>
    </div>

    <BaseButton
      size="m"
      :label="isUploading ? 'Загрузка...' : 'Загрузить файл'"
      :loading="isUploading"
      @click="openFilePicker"
    />

    <div v-if="currentUpload" class="file-uploader__progress">
      <div class="file-uploader__progress-head">
        <span>{{ currentUpload.name }}</span>
        <strong>{{ formatFileSize(currentUpload.size) }}</strong>
      </div>
      <div class="file-progress">
        <span :style="{ width: `${currentUpload.progress}%` }"></span>
      </div>
    </div>
  </section>
</template>
