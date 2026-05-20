import type { FileResponse, FileType, FileUploadResponse, PagedResponse } from "@/shared/api/contracts";
import { env } from "@/shared/config/env";
import { request, uploadFile } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";
import { tokenStorage } from "@/shared/lib/token-storage";

const FILE_REQUEST_IDLE_TIMEOUT_MS = 120000;

const uploadReplacement = (fileId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  return request<FileResponse>(`/files/${fileId}/replace`, {
    method: "PUT",
    body: formData
  });
};

const getFilenameFromDisposition = (value: string | null) => {
  if (!value) {
    return "";
  }

  const match = value.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  const filename = match?.[1];
  if (!filename) {
    return "";
  }

  try {
    return decodeURIComponent(filename);
  } catch {
    return filename;
  }
};

export interface DownloadedFileBlob {
  blob: Blob;
  filename: string;
}

export interface FileDownloadOptions {
  onProgress?: (progress: number | null) => void;
  useGestureWindow?: boolean;
}

const saveBlob = (blob: Blob, filename: string, targetWindow?: Window | null) => {
  const url = URL.createObjectURL(blob);

  if (targetWindow && !targetWindow.closed) {
    targetWindow.location.href = url;
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    return;
  }

  const link = document.createElement("a");

  link.href = url;
  link.download = filename || "file";
  link.rel = "noopener";
  link.style.display = "none";

  document.body.appendChild(link);
  link.click();
  link.remove();

  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};

export const fetchFileBlob = (
  fileId: number,
  fallbackFilename: string,
  options: FileDownloadOptions = {}
): Promise<DownloadedFileBlob> =>
  new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const token = tokenStorage.get();
    const fallback = fallbackFilename || "file";
    let completed = false;
    let timeout: number | null = null;

    const cleanup = () => {
      completed = true;
      if (timeout !== null) {
        window.clearTimeout(timeout);
        timeout = null;
      }
      options.onProgress?.(null);
    };

    const armTimeout = () => {
      if (timeout !== null) {
        window.clearTimeout(timeout);
      }
      timeout = window.setTimeout(() => {
        if (completed) return;
        cleanup();
        xhr.abort();
        reject(new Error("Файл слишком долго загружается. Попробуйте ещё раз."));
      }, FILE_REQUEST_IDLE_TIMEOUT_MS);
    };

    armTimeout();

    xhr.ontimeout = () => {
      if (completed) return;
      cleanup();
      reject(new Error("Файл слишком долго загружается. Попробуйте ещё раз."));
    };

    xhr.open("GET", `${env.apiBaseUrl}/files/${fileId}/download`, true);
    xhr.responseType = "blob";

    if (token) {
      xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    }

    xhr.onprogress = (event) => {
      armTimeout();
      if (event.lengthComputable && event.total > 0) {
        options.onProgress?.(Math.round((event.loaded / event.total) * 100));
      } else {
        options.onProgress?.(null);
      }
    };

    xhr.onload = () => {
      cleanup();

      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(`Не удалось скачать файл (${xhr.status}).`));
        return;
      }

      const blob = xhr.response instanceof Blob
        ? xhr.response
        : new Blob([xhr.response], {
            type: xhr.getResponseHeader("Content-Type") || "application/octet-stream"
          });

      const filename = getFilenameFromDisposition(xhr.getResponseHeader("Content-Disposition")) || fallback;
      resolve({ blob, filename });
    };

    xhr.onerror = () => {
      cleanup();
      reject(new Error("Не удалось скачать файл из-за ошибки сети."));
    };

    xhr.onabort = () => {
      if (!completed) {
        cleanup();
        reject(new Error("Скачивание отменено."));
      }
    };

    xhr.send();
  });

export const filesApi = {
  uploadResume(file: File) {
    return uploadFile<FileUploadResponse>("/files/resume", file);
  },
  uploadPortfolio(file: File) {
    return uploadFile<FileUploadResponse>("/files/portfolio", file);
  },
  uploadAvatar(file: File) {
    return uploadFile<FileUploadResponse>("/files/avatar", file);
  },
  uploadChatAttachment(file: File) {
    return uploadFile<FileUploadResponse>("/files/chat-attachment", file);
  },
  list(params: { type?: FileType; page?: number; size?: number; sort?: string } = {}) {
    return request<PagedResponse<FileResponse>>(`/files${buildQuery(params)}`);
  },
  remove(fileId: number) {
    return request<void>(`/files/${fileId}`, { method: "DELETE" });
  },
  replace(fileId: number, file: File) {
    return uploadReplacement(fileId, file);
  },
  async download(fileId: number, fallbackFilename: string, options: FileDownloadOptions = {}) {
    const targetWindow = options.useGestureWindow ? window.open("about:blank", "_blank", "noopener") : null;

    const { blob, filename } = await fetchFileBlob(fileId, fallbackFilename, options);
    saveBlob(blob, filename, targetWindow);
  }
};
