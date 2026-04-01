import type { FileUploadResponse } from "@/shared/api/contracts";
import { uploadFile } from "@/shared/api/http";

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
  }
};
