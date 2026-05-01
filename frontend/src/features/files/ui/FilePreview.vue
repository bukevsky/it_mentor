<script setup lang="ts">
import type { ManagedFile } from "@/features/files/model/files-store";

defineProps<{
  file: ManagedFile;
}>();

const extension = (fileName: string) => {
  const value = fileName.split(".").pop() || "file";
  return value.slice(0, 4).toUpperCase();
};
</script>

<template>
  <div class="file-preview">
    <img
      v-if="file.previewUrl"
      class="file-preview__image"
      :src="file.previewUrl"
      :alt="file.name"
    />
    <span v-else class="file-preview__extension">
      {{ extension(file.name) }}
    </span>
  </div>
</template>
