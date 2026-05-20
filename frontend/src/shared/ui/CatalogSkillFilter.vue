<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue: string[];
    options: Array<{ value: string; label: string }>;
    title?: string;
    placeholder?: string;
  }>(),
  {
    title: "Навык",
    placeholder: "Любой навык"
  }
);

const emit = defineEmits<{
  (event: "update:modelValue", value: string[]): void;
}>();

const rootRef = ref<HTMLElement | null>(null);
const isOpen = ref(false);
const query = ref("");

const selectedIds = computed(() => new Set(props.modelValue));
const realOptions = computed(() => props.options.filter((option) => option.value));
const isDisabled = computed(() => realOptions.value.length === 0);

const selectedSummary = computed(() => {
  if (!props.modelValue.length) {
    return props.placeholder;
  }

  const labels = props.modelValue
    .map((id) => realOptions.value.find((option) => option.value === id)?.label)
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
  if (!isDisabled.value) {
    isOpen.value = !isOpen.value;
  }
};

const toggleSkill = (skillId: string) => {
  if (selectedIds.value.has(skillId)) {
    emit("update:modelValue", props.modelValue.filter((id) => id !== skillId));
    return;
  }

  emit("update:modelValue", [...props.modelValue, skillId]);
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
  <div ref="rootRef" :class="['catalog-skill-filter', { 'catalog-skill-filter--open': isOpen }]">
    <span class="catalog-skill-filter__title">{{ title }}</span>
    <button type="button" class="catalog-skill-filter__trigger" :disabled="isDisabled" @click="toggleOpen">
      <span :class="{ 'catalog-skill-filter__placeholder': !modelValue.length }">
        {{ selectedSummary }}
      </span>
      <span class="catalog-skill-filter__count" v-if="modelValue.length">{{ modelValue.length }}</span>
      <span class="catalog-skill-filter__chevron">v</span>
    </button>

    <div v-if="isOpen" class="catalog-skill-filter__menu">
      <div class="catalog-skill-filter__search">
        <input v-model="query" type="search" placeholder="Поиск навыка" />
      </div>

      <div class="catalog-skill-filter__list">
        <label
          v-for="option in filteredOptions"
          :key="option.value"
          class="catalog-skill-filter__option"
        >
          <input
            type="checkbox"
            :checked="selectedIds.has(option.value)"
            @change="toggleSkill(option.value)"
          />
          <span class="catalog-skill-filter__box"></span>
          <span>{{ option.label }}</span>
        </label>

        <div v-if="!filteredOptions.length" class="catalog-skill-filter__empty">
          Навыки не найдены.
        </div>
      </div>
    </div>
  </div>
</template>
