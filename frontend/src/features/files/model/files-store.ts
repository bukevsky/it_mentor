import { ref } from "vue";
import { defineStore } from "pinia";
import type { ErrorResponse, FileUploadResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { filesApi } from "../api/files-api";

type UploadKind = "resume" | "portfolio" | "avatar" | "chat-attachment";

export const useFilesStore = defineStore("files", () => {
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
        message: "Сначала выберите файл.",
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

      responses.value = {
        ...responses.value,
        [kind]: response
      };
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/files/${kind}`);
    } finally {
      isUploading.value = false;
    }
  };

  return {
    error,
    isUploading,
    onChange,
    responses,
    selectedFiles,
    upload
  };
});
