import { computed } from "vue";
import { storeToRefs } from "pinia";
import { useFilesStore, type ManagedFile, type UploadKind } from "./files-store";

export interface FileCategory {
  kind: UploadKind | "all";
  label: string;
  description: string;
  accept?: string;
}

export const fileCategories: FileCategory[] = [
  {
    kind: "all",
    label: "Все файлы",
    description: "Документы профиля и вложения"
  },
  {
    kind: "resume",
    label: "Резюме",
    description: "PDF, один актуальный файл",
    accept: "application/pdf"
  },
  {
    kind: "portfolio",
    label: "Портфолио",
    description: "PDF, PNG или JPEG",
    accept: "application/pdf,image/png,image/jpeg"
  },
  {
    kind: "avatar",
    label: "Аватар",
    description: "PNG, JPEG или WebP",
    accept: "image/png,image/jpeg,image/webp"
  },
  {
    kind: "chat-attachment",
    label: "Вложения",
    description: "Изображения, PDF, TXT, ZIP, DOC/XLS",
    accept: "image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain,application/zip,.doc,.docx,.xls,.xlsx"
  }
];

export const getFileKindLabel = (kind: UploadKind) => {
  return fileCategories.find((category) => category.kind === kind)?.label ?? "Файл";
};

export const formatFileSize = (size: number) => {
  if (size < 1024) {
    return `${size} Б`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} КБ`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} МБ`;
};

const sortFiles = (files: ManagedFile[]) => {
  return [...files].sort(
    (left, right) => new Date(right.uploadedAt).getTime() - new Date(left.uploadedAt).getTime()
  );
};

export const useFiles = () => {
  const filesStore = useFilesStore();
  const { activeKind, currentUpload, error, files, isLoading, isUploading, successMessage } = storeToRefs(filesStore);

  const activeCategory = computed(() => {
    return fileCategories.find((category) => category.kind === activeKind.value) ?? fileCategories[0];
  });

  const visibleFiles = computed(() => {
    if (activeKind.value === "all") {
      return sortFiles(files.value);
    }

    return sortFiles(files.value.filter((file) => file.kind === activeKind.value));
  });

  const counters = computed(() => {
    return fileCategories.reduce<Record<string, number>>((accumulator, category) => {
      accumulator[category.kind] =
        category.kind === "all"
          ? files.value.length
          : files.value.filter((file) => file.kind === category.kind).length;
      return accumulator;
    }, {});
  });

  const totalSize = computed(() => {
    return files.value.reduce((total, file) => total + file.size, 0);
  });

  const uploadToActiveCategory = (file: File) => {
    const targetKind = activeKind.value === "all" ? "chat-attachment" : activeKind.value;
    void filesStore.upload(targetKind, file);
  };

  return {
    activeCategory,
    activeKind,
    counters,
    currentUpload,
    error,
    fileCategories,
    files,
    formatFileSize,
    getFileKindLabel,
    isLoading,
    isUploading,
    successMessage,
    totalSize,
    uploadToActiveCategory,
    visibleFiles
  };
};
