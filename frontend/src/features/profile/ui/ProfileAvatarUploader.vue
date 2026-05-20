<script setup lang="ts">
import { computed, ref } from "vue";

const props = defineProps<{
  fullName: string;
  avatarSrc: string | null;
  isUploading: boolean;
}>();

const emit = defineEmits<{
  (event: "upload", file: File): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);

const initials = computed(() => {
  const parts = props.fullName.trim().split(/\s+/).filter(Boolean);
  const value = parts.length > 1 ? `${parts[0][0]}${parts[1][0]}` : props.fullName.trim().slice(0, 2);
  return value.toUpperCase() || "IM";
});

const openFileDialog = () => {
  if (!props.isUploading) {
    inputRef.value?.click();
  }
};

const handleFile = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (file) {
    emit("upload", file);
  }

  input.value = "";
};
</script>

<template>
  <div class="profile-avatar-uploader">
    <button
      type="button"
      class="profile-avatar-uploader__preview"
      :disabled="isUploading"
      :aria-label="avatarSrc ? 'Заменить аватар' : 'Загрузить аватар'"
      @click="openFileDialog"
    >
      <img v-if="avatarSrc" :src="avatarSrc" :alt="fullName" />
      <span v-else>{{ initials }}</span>
    </button>

    <div class="profile-avatar-uploader__meta">
      <strong>Аватар профиля</strong>
      <button type="button" :disabled="isUploading" @click="openFileDialog">
        {{ isUploading ? "Загрузка..." : avatarSrc ? "Заменить фото" : "Загрузить фото" }}
      </button>
    </div>

    <input
      ref="inputRef"
      class="hidden-file-input"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      @change="handleFile"
    />
  </div>
</template>
