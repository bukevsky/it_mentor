<script setup lang="ts">
import { ref } from "vue";
import type { ErrorResponse, FileUploadResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { filesApi } from "../api/files-api";

type UploadKind = "resume" | "portfolio" | "avatar" | "chat-attachment";

const selectedFiles = ref<Record<UploadKind, File | null>>({
  resume: null,
  portfolio: null,
  avatar: null,
  "chat-attachment": null
});
const responses = ref<Partial<Record<UploadKind, FileUploadResponse>>>({});
const error = ref<ErrorResponse | null>(null);
const isUploading = ref(false);

const onChange = (kind: UploadKind, event: Event) => {
  const input = event.target as HTMLInputElement;
  selectedFiles.value[kind] = input.files?.[0] ?? null;
};

const upload = async (kind: UploadKind) => {
  const file = selectedFiles.value[kind];

  if (!file) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: "Сначала выберите файл",
      path: `/files/${kind}`
    };
    return;
  }

  isUploading.value = true;
  error.value = null;

  try {
    const response =
      kind === "resume"
        ? await filesApi.uploadResume(file)
        : kind === "portfolio"
          ? await filesApi.uploadPortfolio(file)
          : kind === "avatar"
            ? await filesApi.uploadAvatar(file)
            : await filesApi.uploadChatAttachment(file);

    responses.value = { ...responses.value, [kind]: response };
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, `/files/${kind}`);
  } finally {
    isUploading.value = false;
  }
};
</script>

<template>
  <SectionCard
    description="Файловые endpoints остаются в read-only отношении к backend и вызываются через multipart/form-data с полем file."
    kicker="Files"
    title="Загрузка файлов"
  >
    <div class="stack">
      <div class="upload-grid">
        <label class="field">
          <span>POST /files/resume (PDF, max 5MB)</span>
          <input accept="application/pdf" type="file" @change="onChange('resume', $event)" />
        </label>
        <button class="ghost-button" :disabled="isUploading" type="button" @click="upload('resume')">
          Upload resume
        </button>

        <label class="field">
          <span>POST /files/portfolio (PDF/JPEG/PNG, max 10MB)</span>
          <input accept="application/pdf,image/png,image/jpeg" type="file" @change="onChange('portfolio', $event)" />
        </label>
        <button class="ghost-button" :disabled="isUploading" type="button" @click="upload('portfolio')">
          Upload portfolio
        </button>

        <label class="field">
          <span>POST /files/avatar (JPEG/PNG/WebP, max 2MB)</span>
          <input accept="image/png,image/jpeg,image/webp" type="file" @change="onChange('avatar', $event)" />
        </label>
        <button class="ghost-button" :disabled="isUploading" type="button" @click="upload('avatar')">
          Upload avatar
        </button>

        <label class="field">
          <span>POST /files/chat-attachment (any)</span>
          <input type="file" @change="onChange('chat-attachment', $event)" />
        </label>
        <button class="ghost-button" :disabled="isUploading" type="button" @click="upload('chat-attachment')">
          Upload chat attachment
        </button>
      </div>

      <div v-if="error" class="error-box">
        <strong>{{ error.error }}</strong>
        <p>{{ error.message }}</p>
      </div>

      <div class="section-grid">
        <JsonPreview :value="responses.resume" title="Resume response" />
        <JsonPreview :value="responses.portfolio" title="Portfolio response" />
        <JsonPreview :value="responses.avatar" title="Avatar response" />
        <JsonPreview :value="responses['chat-attachment']" title="Chat attachment response" />
      </div>
    </div>
  </SectionCard>
</template>
