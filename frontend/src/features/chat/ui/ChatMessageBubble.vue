<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { filesApi } from "@/features/files/api/files-api";
import { imageCache, isImageAttachment } from "@/features/files/model/image-cache";
import type { ChatViewMessage } from "@/features/chat/model/chat-store";

const props = defineProps<{
  message: ChatViewMessage;
  currentUserId?: number;
  peerName: string;
}>();

const isDownloading = ref(false);
const imageSrc = ref<string | null>(null);
const imageLoading = ref(false);
const imageError = ref(false);
const isPreviewOpen = ref(false);
const previewZoom = ref(1);
const downloadError = ref("");
const downloadProgress = ref<number | null>(null);
let imageLoadVersion = 0;

const timeLabel = (value: string) =>
  new Intl.DateTimeFormat("ru-RU", { hour: "2-digit", minute: "2-digit" }).format(new Date(value));

const deliveryLabel = () => {
  if (props.message.senderUserId !== props.currentUserId) return "";

  const labels = {
    sending: "sending",
    error: "error",
    SENT: "sent",
    DELIVERED: "delivered",
    READ: "read"
  } as const;

  return labels[props.message.deliveryStatus];
};

const fileExt = (filename = "") => {
  const parts = filename.split(".");
  return parts.length > 1 ? parts[parts.length - 1].toUpperCase() : "FILE";
};

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const imageAttachment = computed(() => {
  const attachment = props.message.attachment;
  return attachment && isImageAttachment(attachment.contentType, attachment.originalFilename)
    ? attachment
    : null;
});

const loadImagePreview = async () => {
  const attachment = imageAttachment.value;
  if (!attachment || imageLoading.value) return;

  const currentVersion = ++imageLoadVersion;
  imageLoading.value = true;
  imageError.value = false;
  downloadError.value = "";

  try {
    const loadedSrc = await imageCache.load(attachment.fileId, attachment.originalFilename);
    if (currentVersion === imageLoadVersion) {
      imageSrc.value = loadedSrc;
    }
  } catch {
    if (currentVersion === imageLoadVersion) {
      imageError.value = true;
    }
  } finally {
    if (currentVersion === imageLoadVersion) {
      imageLoading.value = false;
    }
  }
};

const downloadAttachment = async () => {
  if (!props.message.attachment || isDownloading.value) return;
  isDownloading.value = true;
  downloadError.value = "";
  downloadProgress.value = 0;
  try {
    await filesApi.download(props.message.attachment.fileId, props.message.attachment.originalFilename || "file", {
      useGestureWindow: true,
      onProgress: (progress) => {
        downloadProgress.value = progress;
      }
    });
  } catch (error) {
    if (!imageCache.download(props.message.attachment.fileId, props.message.attachment.originalFilename || "file")) {
      downloadError.value = error instanceof Error ? error.message : "Не удалось скачать файл.";
    }
  } finally {
    isDownloading.value = false;
    downloadProgress.value = null;
  }
};

const openPreview = () => {
  if (!imageSrc.value || !imageAttachment.value) return;
  previewZoom.value = 1;
  isPreviewOpen.value = true;
};

const closePreview = () => {
  isPreviewOpen.value = false;
  previewZoom.value = 1;
};

const zoomIn = () => {
  previewZoom.value = Math.min(previewZoom.value + 0.25, 4);
};

const zoomOut = () => {
  previewZoom.value = Math.max(previewZoom.value - 0.25, 0.5);
};

const resetZoom = () => {
  previewZoom.value = 1;
};

const handlePreviewWheel = (event: WheelEvent) => {
  event.preventDefault();
  if (event.deltaY < 0) {
    zoomIn();
  } else {
    zoomOut();
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === "Escape") {
    closePreview();
  }
};

watch(
  () => props.message.attachment,
  (att) => {
    ++imageLoadVersion;
    imageSrc.value = null;
    imageError.value = false;
    imageLoading.value = false;
    closePreview();

    if (!att || !isImageAttachment(att.contentType, att.originalFilename)) return;

    imageSrc.value = imageCache.get(att.fileId) ?? null;
    if (!imageSrc.value) {
      void loadImagePreview();
    }
  },
  { immediate: true }
);

watch(isPreviewOpen, (isOpen) => {
  if (isOpen) {
    window.addEventListener("keydown", handleKeydown);
  } else {
    window.removeEventListener("keydown", handleKeydown);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
});
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

    <div v-if="message.body" class="chat-bubble__body">
      {{ message.body }}
    </div>

    <template v-if="message.attachment">
      <div v-if="imageAttachment" class="chat-bubble__image-block">
        <div class="chat-bubble__image-wrap">
          <div v-if="imageLoading" class="chat-bubble__image-placeholder">
            <span>Загрузка...</span>
          </div>
          <button
            v-else-if="imageError"
            type="button"
            class="chat-bubble__image-placeholder chat-bubble__image-placeholder--button"
            @click="loadImagePreview"
          >
            Не удалось загрузить. Повторить
          </button>
          <button
            v-else-if="imageSrc"
            type="button"
            class="chat-bubble__image-button"
            :aria-label="`Открыть ${message.attachment.originalFilename}`"
            @click="openPreview"
          >
            <img
              :src="imageSrc"
              class="chat-bubble__image"
              :alt="message.attachment.originalFilename"
            />
          </button>
          <button
            v-else
            type="button"
            class="chat-bubble__image-placeholder chat-bubble__image-placeholder--button"
            @click="loadImagePreview"
          >
            Загрузить изображение
          </button>
        </div>
        <button
          type="button"
          class="chat-bubble__image-download-btn"
          :disabled="isDownloading"
          @click="downloadAttachment"
        >
          <span
            v-if="isDownloading"
            class="chat-download-progress"
            :style="{ '--progress': `${downloadProgress ?? 35}%` }"
            aria-hidden="true"
          ></span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
            stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          {{ isDownloading ? "Скачивание..." : "Скачать" }}
        </button>
        <p v-if="downloadError" class="chat-bubble__download-error">{{ downloadError }}</p>
      </div>

      <!-- File tile -->
      <button
        v-else
        type="button"
        class="chat-bubble__attachment"
        :disabled="isDownloading"
        @click="downloadAttachment"
      >
        <div class="chat-bubble__attachment-info">
          <span class="chat-bubble__attachment-ext">{{ fileExt(message.attachment.originalFilename) }}</span>
          <span class="chat-bubble__attachment-name">{{ message.attachment.originalFilename }}</span>
          <span class="chat-bubble__attachment-size">{{ formatSize(message.attachment.size) }}</span>
        </div>
        <span
          v-if="isDownloading"
          class="chat-download-progress chat-download-progress--tile"
          :style="{ '--progress': `${downloadProgress ?? 35}%` }"
          aria-hidden="true"
        ></span>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round" width="18" height="18"
          class="chat-bubble__attachment-icon">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="7 10 12 15 17 10"/>
          <line x1="12" y1="15" x2="12" y2="3"/>
        </svg>
      </button>
      <p v-if="downloadError" class="chat-bubble__download-error">{{ downloadError }}</p>
    </template>

    <div class="chat-bubble__footer">
      <span>{{ timeLabel(message.createdAt) }}</span>
      <span v-if="deliveryLabel()" class="chat-bubble__status">{{ deliveryLabel() }}</span>
    </div>

    <Teleport to="body">
      <div
        v-if="isPreviewOpen && imageSrc && imageAttachment"
        class="chat-image-viewer"
        role="dialog"
        aria-modal="true"
        :aria-label="imageAttachment.originalFilename"
        @click.self="closePreview"
      >
        <div class="chat-image-viewer__toolbar">
          <strong class="chat-image-viewer__title">{{ imageAttachment.originalFilename }}</strong>
          <div class="chat-image-viewer__actions">
            <button type="button" class="chat-image-viewer__button" @click="zoomOut">−</button>
            <button type="button" class="chat-image-viewer__button" @click="resetZoom">
              {{ Math.round(previewZoom * 100) }}%
            </button>
            <button type="button" class="chat-image-viewer__button" @click="zoomIn">+</button>
            <button
              type="button"
              class="chat-image-viewer__button"
              :disabled="isDownloading"
              @click="downloadAttachment"
            >
              Скачать
            </button>
            <button type="button" class="chat-image-viewer__button" @click="closePreview">Закрыть</button>
          </div>
        </div>
        <div class="chat-image-viewer__stage" @wheel="handlePreviewWheel">
          <img
            class="chat-image-viewer__image"
            :src="imageSrc"
            :alt="imageAttachment.originalFilename"
            :style="{ transform: `scale(${previewZoom})` }"
          />
        </div>
      </div>
    </Teleport>
  </article>
</template>
