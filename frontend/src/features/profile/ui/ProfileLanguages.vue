<script setup lang="ts">
import { computed, ref } from "vue";
import { BaseButton, BaseSelect } from "conductor";
import type { StudentFormLanguage } from "@/features/profile/model/profiles-store";
import { languageLevelOptions } from "@/shared/lib/options";

const props = defineProps<{
  languages: StudentFormLanguage[];
  languageOptions: Array<{ value: string; label: string }>;
  selectState: string;
}>();

const emit = defineEmits<{
  (event: "add", languageId: string, level: string): void;
  (event: "remove", languageId: string): void;
  (event: "update-level", languageId: string, level: string): void;
}>();

const selectedLanguageId = ref("");
const selectedLevel = ref("B2");

const availableLanguageOptions = computed(() => {
  const selectedIds = new Set(props.languages.map((language) => language.languageId));
  return props.languageOptions.filter((option) => option.value && !selectedIds.has(option.value));
});

const getLanguageLabel = (languageId: string) => {
  return props.languageOptions.find((option) => option.value === languageId)?.label ?? "Язык";
};

const addLanguage = () => {
  emit("add", selectedLanguageId.value, selectedLevel.value);
  selectedLanguageId.value = "";
  selectedLevel.value = "B2";
};
</script>

<template>
  <div class="profile-skills">
    <div class="profile-skills__add">
      <BaseSelect
        v-model="selectedLanguageId"
        size="m"
        title="Язык"
        placeholder="Выберите язык"
        :state="selectState"
        :options="availableLanguageOptions"
      />
      <BaseSelect v-model="selectedLevel" size="m" title="Уровень" :options="languageLevelOptions" />
      <BaseButton
        size="m"
        label="Добавить"
        :disabled="!selectedLanguageId"
        @click="addLanguage"
      />
    </div>

    <div v-if="languages.length" class="profile-tag-list">
      <div v-for="language in languages" :key="language.languageId" class="profile-tag">
        <strong>{{ getLanguageLabel(language.languageId) }}</strong>
        <BaseSelect
          :model-value="language.level"
          size="s"
          title=""
          :options="languageLevelOptions"
          @update:model-value="emit('update-level', language.languageId, String($event))"
        />
        <button type="button" class="profile-tag__remove" @click="emit('remove', language.languageId)">
          Удалить
        </button>
      </div>
    </div>

    <div v-else class="empty-state profile-inline-empty">
      Добавьте языки, если они важны для стажировки или проекта.
    </div>
  </div>
</template>
