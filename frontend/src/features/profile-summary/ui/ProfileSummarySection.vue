<script setup lang="ts">
import { ref } from "vue";
import type { ErrorResponse, ProfileSummaryResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { profileSummaryApi } from "../api/profile-summary-api";

const response = ref<ProfileSummaryResponse>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);

const loadSummary = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    response.value = await profileSummaryApi.getMySummary();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, "/profile/me");
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <SectionCard
    description="Первый защищенный запрос после логина. Показывает роль текущего пользователя и наличие профильной сущности."
    kicker="Profile Summary"
    title="Сводка профиля"
  >
    <div class="actions">
      <button class="primary-button" :disabled="isLoading" type="button" @click="loadSummary">
        {{ isLoading ? "Загрузка..." : "GET /profile/me" }}
      </button>
    </div>

    <div v-if="error" class="error-box">
      <strong>{{ error.error }}</strong>
      <p>{{ error.message }}</p>
    </div>

    <JsonPreview :value="response" title="ProfileSummaryResponse" />
  </SectionCard>
</template>
