<script setup lang="ts">
import { ref, watch } from "vue";
import type { ManagedFile } from "@/features/files/model/files-store";
import { imageCache, isImageAttachment } from "@/features/files/model/image-cache";

const props = defineProps<{
  file: ManagedFile;
}>();

const previewSrc = ref<string | null>(null);
const isLoadingPreview = ref(false);
const hasPreviewError = ref(false);
let previewVersion = 0;

const extension = (fileName: string) => {
  const value = fileName.split(".").pop() || "file";
  return value.slice(0, 4).toUpperCase();
};

watch(
  () => [props.file.backendId, props.file.previewUrl, props.file.contentType, props.file.name] as const,
  async () => {
    const version = ++previewVersion;
    previewSrc.value = null;
    hasPreviewError.value = false;
    isLoadingPreview.value = false;

    if (!isImageAttachment(props.file.contentType, props.file.name)) {
      return;
    }

    if (props.file.previewUrl?.startsWith("blob:")) {
      previewSrc.value = props.file.previewUrl;
      return;
    }

    if (!props.file.backendId) {
      return;
    }

    const cached = imageCache.get(props.file.backendId);
    if (cached) {
      previewSrc.value = cached;
      return;
    }

    isLoadingPreview.value = true;
    try {
      const src = await imageCache.load(props.file.backendId, props.file.name);
      if (version === previewVersion) {
        previewSrc.value = src;
      }
    } catch {
      if (version === previewVersion) {
        hasPreviewError.value = true;
      }
    } finally {
      if (version === previewVersion) {
        isLoadingPreview.value = false;
      }
    }
  },
  { immediate: true }
);
</script>

<template>
  <div class="file-preview">
    <img
      v-if="previewSrc"
      class="file-preview__image"
      :src="previewSrc"
      :alt="file.name"
    />
    <span v-else-if="isLoadingPreview" class="file-preview__extension">
      ...
    </span>
    <span v-else-if="hasPreviewError" class="file-preview__extension">
      IMG
    </span>
    <span v-else class="file-preview__extension">
      {{ extension(file.name) }}
    </span>
  </div>
</template>
