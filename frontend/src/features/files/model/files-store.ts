import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { ErrorResponse, FileResponse, FileType, FileUploadResponse } from "@/shared/api/contracts";
import { imageCache, isImageAttachment } from "@/features/files/model/image-cache";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { filesApi } from "../api/files-api";

export type UploadKind = "resume" | "portfolio" | "avatar" | "chat-attachment";
export type ManagedFileStatus = "uploaded" | "uploading" | "error";

export interface ManagedFile {
  id: string;
  backendId: number | null;
  kind: UploadKind;
  fileType: FileType;
  name: string;
  size: number;
  contentType: string;
  uploadedAt: string;
  status: ManagedFileStatus;
  progress: number;
  sourceFile?: File;
  previewUrl?: string;
  errorMessage?: string;
}

const kindToFileType: Record<UploadKind, FileType> = {
  resume: "RESUME",
  portfolio: "PORTFOLIO",
  avatar: "AVATAR",
  "chat-attachment": "CHAT_ATTACHMENT"
};

const fileTypeToKind: Record<FileType, UploadKind> = {
  RESUME: "resume",
  PORTFOLIO: "portfolio",
  AVATAR: "avatar",
  CHAT_ATTACHMENT: "chat-attachment"
};

const uploaders: Record<UploadKind, (file: File) => Promise<FileUploadResponse>> = {
  resume: filesApi.uploadResume,
  portfolio: filesApi.uploadPortfolio,
  avatar: filesApi.uploadAvatar,
  "chat-attachment": filesApi.uploadChatAttachment
};

const createObjectPreview = (file: File) => {
  if (!file.type.startsWith("image/")) {
    return undefined;
  }

  return URL.createObjectURL(file);
};

const createTempFile = (kind: UploadKind, file: File): ManagedFile => ({
  id: `temp-${kind}-${Date.now()}`,
  backendId: null,
  kind,
  fileType: kindToFileType[kind],
  name: file.name,
  size: file.size,
  contentType: file.type || "application/octet-stream",
  uploadedAt: new Date().toISOString(),
  status: "uploading",
  progress: 8,
  sourceFile: file,
  previewUrl: createObjectPreview(file)
});

const createManagedFile = (response: FileResponse | FileUploadResponse): ManagedFile => ({
  id: `${response.fileType.toLowerCase()}-${response.id}`,
  backendId: response.id,
  kind: fileTypeToKind[response.fileType],
  fileType: response.fileType,
  name: response.originalFilename,
  size: response.size,
  contentType: response.contentType,
  uploadedAt: response.uploadedAt,
  status: "uploaded",
  progress: 100,
  previewUrl: "previewUrl" in response && response.previewUrl?.startsWith("blob:")
    ? response.previewUrl
    : undefined
});

const createClientError = (message: string, path: string): ErrorResponse => ({
  timestamp: new Date().toISOString(),
  status: 0,
  error: "FILES_ERROR",
  message,
  path
});

export const useFilesStore = defineStore("files", () => {
  const files = ref<ManagedFile[]>([]);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const activeKind = ref<UploadKind | "all">("all");
  const currentUploadId = ref<string | null>(null);
  const isLoading = ref(false);
  let progressTimer: ReturnType<typeof setInterval> | null = null;

  const isUploading = computed(() => files.value.some((file) => file.status === "uploading"));
  const currentUpload = computed(() => files.value.find((file) => file.id === currentUploadId.value) ?? null);

  const updateFile = (fileId: string, patch: Partial<ManagedFile>) => {
    files.value = files.value.map((file) => (file.id === fileId ? { ...file, ...patch } : file));
  };

  const startProgress = (fileId: string) => {
    if (progressTimer) {
      clearInterval(progressTimer);
    }

    progressTimer = setInterval(() => {
      const target = files.value.find((file) => file.id === fileId);

      if (!target || target.status !== "uploading") {
        return;
      }

      updateFile(fileId, { progress: Math.min(target.progress + 12, 92) });
    }, 180);
  };

  const stopProgress = () => {
    if (progressTimer) {
      clearInterval(progressTimer);
      progressTimer = null;
    }
  };

  const loadFiles = async () => {
    isLoading.value = true;
    error.value = null;

    try {
      const response = await filesApi.list({ page: 0, size: 200, sort: "uploadedAt,desc" });
      files.value = response.content.map(createManagedFile);
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/files");
    } finally {
      isLoading.value = false;
    }
  };

  const upload = async (kind: UploadKind, file: File, options: { replaceId?: string } = {}) => {
    const pendingFile = createTempFile(kind, file);
    error.value = null;
    successMessage.value = "";
    currentUploadId.value = pendingFile.id;

    if (options.replaceId) {
      files.value = files.value.map((item) => (item.id === options.replaceId ? pendingFile : item));
    } else {
      files.value = [pendingFile, ...files.value];
    }

    startProgress(pendingFile.id);

    try {
      const response = await uploaders[kind](file);
      if (isImageAttachment(response.contentType, response.originalFilename)) {
        imageCache.prime(response.id, file, response.originalFilename);
      }
      files.value = files.value.map((item) =>
        item.id === pendingFile.id ? createManagedFile(response) : item
      );
      successMessage.value = "Файл загружен.";
      await loadFiles();
    } catch (rawError) {
      const nextError = normalizeErrorResponse(rawError, `/files/${kind}`);
      updateFile(pendingFile.id, {
        status: "error",
        progress: 0,
        errorMessage: nextError.message
      });
      error.value = nextError;
    } finally {
      stopProgress();
      currentUploadId.value = null;
    }
  };

  const retry = (fileId: string) => {
    const target = files.value.find((file) => file.id === fileId);

    if (!target?.sourceFile) {
      error.value = createClientError("Повтор доступен только для файла из текущей попытки загрузки.", "/files");
      return;
    }

    void upload(target.kind, target.sourceFile, { replaceId: target.id });
  };

  const replace = async (fileId: string, file: File) => {
    const target = files.value.find((item) => item.id === fileId);

    if (!target) {
      return;
    }

    if (!target.backendId) {
      void upload(target.kind, file, { replaceId: fileId });
      return;
    }

    error.value = null;
    successMessage.value = "";
    currentUploadId.value = target.id;
    updateFile(target.id, {
      status: "uploading",
      progress: 8,
      sourceFile: file,
      previewUrl: createObjectPreview(file)
    });
    startProgress(target.id);

    try {
      const response = await filesApi.replace(target.backendId, file);
      if (isImageAttachment(response.contentType, response.originalFilename)) {
        imageCache.prime(response.id, file, response.originalFilename);
      }
      files.value = files.value.map((item) =>
        item.id === target.id ? createManagedFile(response) : item
      );
      successMessage.value = "Файл заменён.";
      await loadFiles();
    } catch (rawError) {
      const nextError = normalizeErrorResponse(rawError, `/files/${target.backendId}/replace`);
      updateFile(target.id, {
        status: "error",
        progress: 0,
        errorMessage: nextError.message
      });
      error.value = nextError;
    } finally {
      stopProgress();
      currentUploadId.value = null;
    }
  };

  const remove = async (fileId: string) => {
    const target = files.value.find((file) => file.id === fileId);

    if (!target) {
      return;
    }

    if (!target.backendId) {
      files.value = files.value.filter((file) => file.id !== fileId);
      return;
    }

    error.value = null;
    successMessage.value = "";

    try {
      await filesApi.remove(target.backendId);
      files.value = files.value.filter((file) => file.id !== fileId);
      successMessage.value = "Файл удалён.";
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/files/${target.backendId}`);
    }
  };

  const download = async (fileId: string) => {
    const target = files.value.find((file) => file.id === fileId);

    if (!target) {
      return;
    }

    if (!target.backendId) {
      error.value = createClientError("Файл ещё не сохранён на сервере.", "/files");
      return;
    }

    error.value = null;

    try {
      await filesApi.download(target.backendId, target.name);
    } catch (rawError) {
      error.value = createClientError(
        rawError instanceof Error ? rawError.message : "Не удалось скачать файл.",
        `/files/${target.backendId}/download`
      );
    }
  };

  return {
    activeKind,
    currentUpload,
    download,
    error,
    files,
    isLoading,
    isUploading,
    loadFiles,
    remove,
    replace,
    retry,
    successMessage,
    upload
  };
});
