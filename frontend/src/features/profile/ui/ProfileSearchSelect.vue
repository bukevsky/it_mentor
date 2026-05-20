<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue: string;
    options: Array<{ value: string; label: string }>;
    selectState: string;
    title: string;
    placeholder?: string;
  }>(),
  {
    placeholder: "Начните вводить"
  }
);

const emit = defineEmits<{
  (event: "update:modelValue", value: string): void;
}>();

const rootRef = ref<HTMLElement | null>(null);
const isOpen = ref(false);
const query = ref("");

const realOptions = computed(() => props.options.filter((option) => option.value));
const selectedOption = computed(() => realOptions.value.find((option) => option.value === props.modelValue));
const isDisabled = computed(() => props.selectState === "disabled" || realOptions.value.length === 0);

const filteredOptions = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();

  if (!normalizedQuery) {
    return realOptions.value.slice(0, 40);
  }

  return realOptions.value
    .filter((option) => option.label.toLowerCase().includes(normalizedQuery))
    .slice(0, 40);
});

const syncQuery = () => {
  query.value = selectedOption.value?.label ?? "";
};

const openSelect = () => {
  if (!isDisabled.value) {
    isOpen.value = true;
  }
};

const selectOption = (value: string) => {
  emit("update:modelValue", value);
  isOpen.value = false;
};

const handleInput = (value: string) => {
  query.value = value;
  isOpen.value = true;

  if (props.modelValue && selectedOption.value?.label !== value) {
    emit("update:modelValue", "");
  }
};

const closeByOutsideClick = (event: MouseEvent) => {
  if (!rootRef.value?.contains(event.target as Node)) {
    isOpen.value = false;
    syncQuery();
  }
};

const closeByEscape = (event: KeyboardEvent) => {
  if (event.key === "Escape") {
    isOpen.value = false;
    syncQuery();
  }
};

watch(() => props.modelValue, syncQuery, { immediate: true });
watch(realOptions, syncQuery);

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
    :class="['profile-search-select', { 'profile-search-select--open': isOpen, 'profile-search-select--disabled': isDisabled }]"
  >
    <label class="profile-search-select__label">
      <span>{{ title }}</span>
      <span class="profile-search-select__field">
        <input
          :value="query"
          :placeholder="placeholder"
          :disabled="isDisabled"
          autocomplete="off"
          @focus="openSelect"
          @input="handleInput(($event.target as HTMLInputElement).value)"
        />
        <span class="profile-search-select__chevron">v</span>
      </span>
    </label>

    <div v-if="isOpen" class="profile-search-select__menu">
      <button
        v-for="option in filteredOptions"
        :key="option.value"
        type="button"
        :class="['profile-search-select__option', { 'profile-search-select__option--selected': option.value === modelValue }]"
        @click="selectOption(option.value)"
      >
        {{ option.label }}
      </button>

      <div v-if="!filteredOptions.length" class="profile-search-select__empty">
        Ничего не найдено.
      </div>
    </div>
  </div>
</template>
