<script setup lang="ts">
defineProps<{
  about: string;
  max: string;
}>();

const emit = defineEmits<{
  (event: "update:about", value: string): void;
  (event: "update:max", value: string): void;
}>();
</script>

<template>
  <div class="student-section-grid">
    <label class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
      <span class="base-input__title">Коротко о себе и целях</span>
      <span class="input">
        <textarea
          :value="about"
          placeholder="Например: хочу попасть на frontend-стажировку, подтянуть TypeScript и собрать портфолио."
          @input="emit('update:about', ($event.target as HTMLTextAreaElement).value)"
        ></textarea>
      </span>
    </label>

    <label class="profile-input-shell base-input base-input__default base-input__size-m">
      <span class="base-input__title">Контакт для связи</span>
      <span class="input">
        <input
          :value="max"
          placeholder="@username или ссылка"
          @input="emit('update:max', ($event.target as HTMLInputElement).value)"
        />
      </span>
    </label>
  </div>
</template>
