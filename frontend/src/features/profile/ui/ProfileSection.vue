<script setup lang="ts">
defineProps<{
  title: string;
  summary: string;
  isOpen: boolean;
  isComplete?: boolean;
}>();

const emit = defineEmits<{
  (event: "open"): void;
}>();
</script>

<template>
  <section :class="['profile-section', { 'profile-section--open': isOpen }]">
    <button type="button" class="profile-section__head" @click="emit('open')">
      <span>
        <strong>{{ title }}</strong>
        <small>{{ summary }}</small>
      </span>
      <em :class="{ 'profile-section__state--done': isComplete }">
        {{ isComplete ? "Готово" : "Заполнить" }}
      </em>
    </button>

    <div v-if="isOpen" class="profile-section__body">
      <slot />
    </div>
  </section>
</template>
