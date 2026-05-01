<script setup lang="ts">
defineProps<{
  preview: {
    fullName: string;
    city: string;
    position: string;
    about: string;
    skills: Array<{ id: string; label: string; level: string }>;
    workFormats: string[];
    employmentTypes: string[];
    languages: Array<{ id: string; label: string; level: string }>;
  };
}>();
</script>

<template>
  <aside class="profile-live-preview">
    <div class="profile-live-preview__cover"></div>
    <div class="profile-live-preview__avatar">
      {{ preview.fullName.slice(0, 1).toUpperCase() }}
    </div>

    <div class="profile-live-preview__body">
      <p class="section-kicker">Предпросмотр</p>
      <h3>{{ preview.fullName }}</h3>
      <p class="profile-live-preview__position">{{ preview.position }}</p>
      <p class="profile-live-preview__meta">{{ preview.city }}</p>

      <p class="profile-live-preview__about">{{ preview.about }}</p>

      <div class="profile-live-preview__group">
        <strong>Навыки</strong>
        <div v-if="preview.skills.length" class="profile-preview-tags">
          <span v-for="skill in preview.skills.slice(0, 6)" :key="skill.id">
            {{ skill.label }}
          </span>
        </div>
        <small v-else>Добавьте навыки</small>
      </div>

      <div class="profile-live-preview__group">
        <strong>Формат</strong>
        <p>{{ [...preview.employmentTypes, ...preview.workFormats].join(" · ") || "Не указан" }}</p>
      </div>

      <div class="profile-live-preview__group">
        <strong>Языки</strong>
        <p>
          {{ preview.languages.map((language) => `${language.label} ${language.level}`).join(", ") || "Не указаны" }}
        </p>
      </div>
    </div>
  </aside>
</template>
