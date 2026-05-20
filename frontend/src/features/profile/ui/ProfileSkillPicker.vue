<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = withDefaults(
  defineProps<{
    skills: Array<{ skillId: string; level: string }>;
    options: Array<{ value: string; label: string }>;
    selectState: string;
    title?: string;
    placeholder?: string;
    defaultLevel?: string;
  }>(),
  {
    title: "Навыки",
    placeholder: "Выберите навыки",
    defaultLevel: "INTERMEDIATE"
  }
);

const emit = defineEmits<{
  (event: "add", skillId: string, level: string): void;
  (event: "remove", skillId: string): void;
}>();

const rootRef = ref<HTMLElement | null>(null);
const isOpen = ref(false);
const query = ref("");

const realOptions = computed(() => props.options.filter((option) => option.value));
const selectedIds = computed(() => new Set(props.skills.map((skill) => skill.skillId)));
const isDisabled = computed(() => props.selectState === "disabled" || realOptions.value.length === 0);

const selectedSummary = computed(() => {
  if (!props.skills.length) {
    return props.placeholder;
  }

  const labels = props.skills
    .map((skill) => realOptions.value.find((option) => option.value === skill.skillId)?.label)
    .filter(Boolean);

  if (labels.length <= 2) {
    return labels.join(", ");
  }

  return `${labels.slice(0, 2).join(", ")} +${labels.length - 2}`;
});

const filteredOptions = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();

  if (!normalizedQuery) {
    return realOptions.value;
  }

  return realOptions.value.filter((option) => option.label.toLowerCase().includes(normalizedQuery));
});

const toggleOpen = () => {
  if (isDisabled.value) {
    return;
  }

  isOpen.value = !isOpen.value;
};

const toggleSkill = (skillId: string) => {
  if (selectedIds.value.has(skillId)) {
    emit("remove", skillId);
    return;
  }

  emit("add", skillId, props.defaultLevel);
};

const closeByOutsideClick = (event: MouseEvent) => {
  if (!rootRef.value?.contains(event.target as Node)) {
    isOpen.value = false;
  }
};

const closeByEscape = (event: KeyboardEvent) => {
  if (event.key === "Escape") {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", closeByOutsideClick);
  document.addEventListener("keydown", closeByEscape);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeByOutsideClick);
  document.removeEventListener("keydown", closeByEscape);
});
</script>

<template>
  <div
    ref="rootRef"
    :class="['profile-skill-picker', { 'profile-skill-picker--open': isOpen, 'profile-skill-picker--disabled': isDisabled }]"
  >
    <span class="profile-skill-picker__title">{{ title }}</span>
    <button type="button" class="profile-skill-picker__trigger" :disabled="isDisabled" @click="toggleOpen">
      <span :class="{ 'profile-skill-picker__placeholder': !skills.length }">
        {{ selectedSummary }}
      </span>
      <span class="profile-skill-picker__chevron">v</span>
    </button>

    <div v-if="isOpen" class="profile-skill-picker__menu">
      <div class="profile-skill-picker__search">
        <input v-model="query" type="search" placeholder="Поиск навыка" />
      </div>

      <div class="profile-skill-picker__list">
        <button
          v-for="option in filteredOptions"
          :key="option.value"
          type="button"
          :class="['checkbox-row', { 'checkbox-row--checked': selectedIds.has(option.value) }]"
          @click="toggleSkill(option.value)"
        >
          <span class="checkbox-dot"></span>
          <span>{{ option.label }}</span>
        </button>

        <div v-if="!filteredOptions.length" class="profile-skill-picker__empty">
          Навыки не найдены.
        </div>
      </div>
    </div>
  </div>
</template>
