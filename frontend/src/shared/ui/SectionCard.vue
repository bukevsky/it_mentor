<script setup lang="ts">
defineProps<{
  kicker: string;
  title: string;
  description: string;
}>();
</script>

<template>
  <article class="panel section-card">
    <div class="panel-head section-card__head">
      <div>
        <p class="panel-kicker">{{ kicker }}</p>
        <h2>{{ title }}</h2>
      </div>
    </div>

    <p class="hint section-card__description">{{ description }}</p>
    <slot />
  </article>
</template>
