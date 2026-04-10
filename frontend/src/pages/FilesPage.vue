<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useFilesStore } from "@/features/files/model/files-store";
import { formatDateTime } from "@/shared/lib/presenters";

type UploadKind = "resume" | "portfolio" | "avatar" | "chat-attachment";

const authStore = useAuthStore();
const filesStore = useFilesStore();

const { isAuthenticated } = storeToRefs(authStore);
const { error, isUploading, responses, selectedFiles } = storeToRefs(filesStore);

const inputRefs = ref<Partial<Record<UploadKind, HTMLInputElement | null>>>({});

const setInputRef = (kind: UploadKind, element: HTMLInputElement | null) => {
  inputRefs.value[kind] = element;
};

const openFilePicker = (kind: UploadKind) => {
  inputRefs.value[kind]?.click();
};

const uploadCards: Array<{
  kind: UploadKind;
  title: string;
  accept?: string;
  hint: string;
}> = [
  {
    kind: "resume",
    title: "Резюме",
    accept: "application/pdf",
    hint: "PDF, рекомендуется до 5 MB"
  },
  {
    kind: "portfolio",
    title: "Портфолио",
    accept: "application/pdf,image/png,image/jpeg",
    hint: "PDF, PNG или JPEG, до 10 MB"
  },
  {
    kind: "avatar",
    title: "Аватар",
    accept: "image/png,image/jpeg,image/webp",
    hint: "PNG, JPEG или WebP, до 2 MB"
  },
  {
    kind: "chat-attachment",
    title: "Вложение в чат",
    hint: "После загрузки используйте fileId при отправке сообщения"
  }
];
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для загрузки файлов нужно войти в систему.
    </div>

    <template v-else>
      <article class="app-panel">
        <p class="section-kicker">Файлы</p>
        <h3 class="section-title">Резюме, портфолио, аватар и вложения</h3>
        <p class="section-copy">
          Сначала выберите файл в карточке, затем отправьте его на backend. После загрузки ниже появятся fileId и метаданные.
        </p>
      </article>

      <div class="files-grid">
        <article v-for="card in uploadCards" :key="card.kind" class="upload-card">
          <div>
            <h4 class="request-card__title">{{ card.title }}</h4>
            <p class="request-card__copy">{{ card.hint }}</p>
          </div>

          <input
            :ref="(element) => setInputRef(card.kind, element as HTMLInputElement | null)"
            class="hidden-file-input"
            :accept="card.accept"
            type="file"
            @change="filesStore.onChange(card.kind, $event)"
          />

          <div class="file-picker">
            <div class="file-picker__summary">
              <span class="file-picker__label">Файл</span>
              <strong class="file-picker__name">
                {{ selectedFiles[card.kind]?.name ?? "Файл ещё не выбран" }}
              </strong>
            </div>
            <BaseButton
              variant="secondary"
              size="m"
              label="Выбрать файл"
              @click="openFilePicker(card.kind)"
            />
          </div>

          <div class="upload-card__actions">
            <BaseButton
              size="l"
              :label="isUploading ? 'Загрузка...' : 'Загрузить'"
              :loading="isUploading"
              :disabled="!selectedFiles[card.kind]"
              @click="filesStore.upload(card.kind)"
            />
          </div>

          <div v-if="responses[card.kind]" class="detail-stack">
            <ul class="clean-list upload-meta">
              <li><span>ID</span><strong>{{ responses[card.kind]?.id }}</strong></li>
              <li><span>Имя файла</span><strong>{{ responses[card.kind]?.originalFilename }}</strong></li>
              <li><span>Тип</span><strong>{{ responses[card.kind]?.fileType }}</strong></li>
              <li><span>Дата</span><strong>{{ formatDateTime(responses[card.kind]?.uploadedAt) }}</strong></li>
            </ul>
          </div>
        </article>
      </div>

      <div v-if="error" class="error-state">
        {{ error.message }}
      </div>
    </template>
  </section>
</template>
