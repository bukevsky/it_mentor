<script setup lang="ts">
import { employmentTypeOptions, workFormatOptions } from "@/shared/lib/options";

defineProps<{
  employmentTypes: string[];
  workFormats: string[];
  hoursPerWeek: number | null;
  availableFrom: string;
}>();

const emit = defineEmits<{
  (event: "toggle-employment", value: string): void;
  (event: "toggle-work-format", value: string): void;
  (event: "update:hoursPerWeek", value: number | null): void;
  (event: "update:availableFrom", value: string): void;
}>();
</script>

<template>
  <div class="student-section-grid">
    <div class="profile-chip-group">
      <span class="profile-chip-group__label">Формат занятости</span>
      <div class="profile-chip-list">
        <button
          v-for="option in employmentTypeOptions"
          :key="option.value"
          type="button"
          :class="['profile-chip', { 'profile-chip--active': employmentTypes.includes(option.value) }]"
          @click="emit('toggle-employment', option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div class="profile-chip-group">
      <span class="profile-chip-group__label">Формат работы</span>
      <div class="profile-chip-list">
        <button
          v-for="option in workFormatOptions"
          :key="option.value"
          type="button"
          :class="['profile-chip', { 'profile-chip--active': workFormats.includes(option.value) }]"
          @click="emit('toggle-work-format', option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div class="form-grid form-grid--two conductor-grid">
      <label class="profile-input-shell base-input base-input__default base-input__size-m">
        <span class="base-input__title">Часов в неделю</span>
        <span class="input">
          <input
            :value="hoursPerWeek ?? ''"
            min="1"
            max="80"
            type="number"
            @input="emit('update:hoursPerWeek', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
          />
        </span>
      </label>

      <label class="profile-input-shell profile-input-shell--date base-input base-input__default base-input__size-m">
        <span class="base-input__title">Доступен с</span>
        <span class="input">
          <input
            :value="availableFrom"
            type="date"
            @input="emit('update:availableFrom', ($event.target as HTMLInputElement).value)"
          />
        </span>
      </label>
    </div>
  </div>
</template>
