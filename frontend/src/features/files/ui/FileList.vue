<script setup lang="ts">
import type { ManagedFile } from "@/features/files/model/files-store";
import FileItem from "./FileItem.vue";

defineProps<{
  files: ManagedFile[];
}>();

const emit = defineEmits<{
  (event: "download", fileId: string): void;
  (event: "delete", fileId: string): void;
  (event: "replace", fileId: string, file: File): void;
  (event: "retry", fileId: string): void;
}>();
</script>

<template>
  <div class="file-list">
    <div v-if="files.length" class="file-list__stack">
      <FileItem
        v-for="file in files"
        :key="file.id"
        :file="file"
        @download="emit('download', $event)"
        @delete="emit('delete', $event)"
        @replace="(fileId, nextFile) => emit('replace', fileId, nextFile)"
        @retry="emit('retry', $event)"
      />
    </div>

    <div v-else class="empty-state file-list__empty">
      Файлов в этом разделе пока нет.
    </div>
  </div>
</template>
