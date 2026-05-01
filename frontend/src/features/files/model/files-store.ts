import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { ErrorResponse, FileType, FileUploadResponse } from "@/shared/api/contracts";
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

const FILES_STORAGE_KEY = "it-mentor.files";

const kindToFileType: Record<UploadKind, FileType> = {
  resume: "RESUME",
  portfolio: "PORTFOLIO",
  avatar: "AVATAR",
  "chat-attachment": "CHAT_ATTACHMENT"
};

const uploaders: Record<UploadKind, (file: File) => Promise<FileUploadResponse>> = {
  resume: filesApi.uploadResume,
  portfolio: filesApi.uploadPortfolio,
  avatar: filesApi.uploadAvatar,
  "chat-attachment": filesApi.uploadChatAttachment
};

const getStoredFiles = (): ManagedFile[] => {
  try {
    const rawValue = window.localStorage.getItem(FILES_STORAGE_KEY);

    if (!rawValue) {
      return [];
    }

    return (JSON.parse(rawValue) as ManagedFile[]).map((file) => ({
      ...file,
      sourceFile: undefined,
      previewUrl: undefined,
      progress: file.status === "uploaded" ? 100 : 0,
      status: file.status === "uploading" ? "error" : file.status
    }));
  } catch {
    return [];
  }
};

const persistFiles = (files: ManagedFile[]) => {
  const serializableFiles = files
    .filter((file) => file.status === "uploaded")
    .map(({ sourceFile, previewUrl, ...file }) => file);

  try {
    window.localStorage.setItem(FILES_STORAGE_KEY, JSON.stringify(serializableFiles));
  } catch {
    // Local persistence is an enhancement; upload flow should not fail because of storage.
  }
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

const createUploadedFile = (
  kind: UploadKind,
  sourceFile: File,
  response: FileUploadResponse,
  previous: ManagedFile
): ManagedFile => ({
  ...previous,
  id: `${response.fileType.toLowerCase()}-${response.id}`,
  backendId: response.id,
  kind,
  fileType: response.fileType,
  name: response.originalFilename,
  size: response.size,
  contentType: response.contentType,
  uploadedAt: response.uploadedAt,
  status: "uploaded",
  progress: 100,
  sourceFile,
  errorMessage: undefined
});

export const useFilesStore = defineStore("files", () => {
  const files = ref<ManagedFile[]>(getStoredFiles());
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const activeKind = ref<UploadKind | "all">("all");
  const currentUploadId = ref<string | null>(null);
  let progressTimer: ReturnType<typeof setInterval> | null = null;

  const isUploading = computed(() => files.value.some((file) => file.status === "uploading"));
  const currentUpload = computed(() => files.value.find((file) => file.id === currentUploadId.value) ?? null);

  const updateFile = (fileId: string, patch: Partial<ManagedFile>) => {
    files.value = files.value.map((file) => (file.id === fileId ? { ...file, ...patch } : file));
  };

  const removeSingleKindFiles = (kind: UploadKind, exceptFileId: string) => {
    if (kind !== "resume" && kind !== "avatar") {
      return;
    }

    files.value = files.value.filter((file) => file.kind !== kind || file.id === exceptFileId);
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

  const upload = async (kind: UploadKind, file: File, options: { replaceId?: string } = {}) => {
    const pendingFile = createTempFile(kind, file);
    error.value = null;
    successMessage.value = "";
    currentUploadId.value = pendingFile.id;

    if (options.replaceId) {
      files.value = files.value.map((item) => (item.id === options.replaceId ? pendingFile : item));
    } else {
      removeSingleKindFiles(kind, pendingFile.id);
      files.value = [pendingFile, ...files.value];
    }

    startProgress(pendingFile.id);

    try {
      const response = await uploaders[kind](file);
      const uploadedFile = createUploadedFile(kind, file, response, pendingFile);

      files.value = files.value.map((item) => (item.id === pendingFile.id ? uploadedFile : item));
      removeSingleKindFiles(kind, uploadedFile.id);
      persistFiles(files.value);
      successMessage.value = "Файл загружен.";
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
      return;
    }

    void upload(target.kind, target.sourceFile, { replaceId: target.id });
  };

  const replace = (fileId: string, file: File) => {
    const target = files.value.find((item) => item.id === fileId);

    if (!target) {
      return;
    }

    void upload(target.kind, file, { replaceId: fileId });
  };

  const remove = (fileId: string) => {
    files.value = files.value.filter((file) => file.id !== fileId);
    persistFiles(files.value);
  };

  const download = (fileId: string) => {
    const target = files.value.find((file) => file.id === fileId);

    if (!target?.sourceFile) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "DOWNLOAD_UNAVAILABLE",
        message: "Скачивание доступно для файлов, загруженных в текущей сессии.",
        path: "/files"
      };
      return;
    }

    const url = URL.createObjectURL(target.sourceFile);
    const link = document.createElement("a");
    link.href = url;
    link.download = target.name;
    link.click();
    URL.revokeObjectURL(url);
  };

  return {
    activeKind,
    currentUpload,
    download,
    error,
    files,
    isUploading,
    remove,
    replace,
    retry,
    successMessage,
    upload
  };
});
