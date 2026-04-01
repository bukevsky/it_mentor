<script setup lang="ts">
import { computed } from "vue";
import { formatJson } from "@/shared/lib/json";

const props = withDefaults(
  defineProps<{
    title?: string;
    value?: unknown;
    emptyText?: string;
  }>(),
  {
    title: "Ответ",
    value: undefined,
    emptyText: "Пока нет данных"
  }
);

const content = computed(() => {
  return props.value === undefined ? props.emptyText : formatJson(props.value);
});
</script>

<template>
  <div class="json-preview">
    <p class="json-preview__title">{{ title }}</p>
    <pre>{{ content }}</pre>
  </div>
</template>
