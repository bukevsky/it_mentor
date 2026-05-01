<script setup lang="ts">
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useFilesStore } from "@/features/files/model/files-store";
import { useFiles } from "@/features/files/model/use-files";
import FileList from "@/features/files/ui/FileList.vue";
import FileUploader from "@/features/files/ui/FileUploader.vue";

const authStore = useAuthStore();
const filesStore = useFilesStore();

const { isAuthenticated } = storeToRefs(authStore);
const {
  activeCategory,
  activeKind,
  counters,
  currentUpload,
  error,
  fileCategories,
  formatFileSize,
  isUploading,
  successMessage,
  totalSize,
  uploadToActiveCategory,
  visibleFiles
} = useFiles();
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для загрузки файлов нужно войти в систему.
    </div>

    <template v-else>
      <article class="app-panel files-manager">
        <div class="files-manager__header">
          <div>
            <p class="section-kicker">Файлы</p>
            <h1 class="workspace-title">Менеджер файлов</h1>
            <p class="workspace-subtitle">
              Документы профиля, портфолио, аватар и вложения для чата в одном списке.
            </p>
          </div>
          <div class="files-manager__stats">
            <span>{{ counters.all }} файлов</span>
            <strong>{{ formatFileSize(totalSize) }}</strong>
          </div>
        </div>

        <div class="files-manager__body">
          <aside class="file-type-list" aria-label="Типы файлов">
            <button
              v-for="category in fileCategories"
              :key="category.kind"
              type="button"
              :class="['file-type-item', { 'file-type-item--active': activeKind === category.kind }]"
              @click="activeKind = category.kind"
            >
              <span>
                <strong>{{ category.label }}</strong>
                <small>{{ category.description }}</small>
              </span>
              <em>{{ counters[category.kind] }}</em>
            </button>
          </aside>

          <main class="files-workspace">
            <FileUploader
              :category="activeCategory"
              :current-upload="currentUpload"
              :is-uploading="isUploading"
              @upload="uploadToActiveCategory"
            />

            <div class="files-toolbar">
              <div>
                <h3 class="section-title">{{ activeCategory.label }}</h3>
                <p class="section-copy">{{ visibleFiles.length }} элементов</p>
              </div>
              <div class="files-toolbar__legend">
                <span>Загружен</span>
                <span>Ошибка</span>
              </div>
            </div>

            <FileList
              :files="visibleFiles"
              @download="filesStore.download"
              @delete="filesStore.remove"
              @replace="filesStore.replace"
              @retry="filesStore.retry"
            />
          </main>
        </div>
      </article>

      <div v-if="successMessage" class="success-state file-toast">
        {{ successMessage }}
      </div>

      <div v-if="error" class="error-state file-toast">
        {{ error.message }}
      </div>
    </template>
  </section>
</template>
