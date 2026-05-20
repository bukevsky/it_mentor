<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { BaseButton } from "conductor";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useReviews } from "@/features/reviews/model/use-reviews";
import ReviewList from "@/features/reviews/ui/ReviewList.vue";
import ReviewStats from "@/features/reviews/ui/ReviewStats.vue";
import { mentorProfileApi } from "@/features/mentor-profile/api/mentor-profile-api";
import type { MentorProfileResponse } from "@/shared/api/contracts";
import { fullName } from "@/shared/lib/presenters";

const {
  error,
  filters,
  isLoading,
  isLoadingMentors,
  mentorOptions,
  selectedMentor,
  summary,
  visibleReviews,
  loadMentors,
  loadReviews
} = useReviews();

const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const ownMentorProfile = ref<MentorProfileResponse | null>(null);
const isLoadingOwnProfile = ref(false);
const ownProfileError = ref("");
const isAdmin = computed(() => user.value?.roles.includes("ADMIN") ?? false);
const isMentor = computed(() => user.value?.roles.includes("MENTOR") ?? false);
const isPersonalMentorView = computed(() => isMentor.value && !isAdmin.value);
const selectedMentorName = computed(() =>
  ownMentorProfile.value
    ? fullName(ownMentorProfile.value)
    : selectedMentor.value
      ? `${selectedMentor.value.firstName} ${selectedMentor.value.lastName}`
      : "Выбранный ментор"
);

watch(
  () => filters.mentorId,
  (nextMentorId, previousMentorId) => {
    if (!isPersonalMentorView.value && nextMentorId && nextMentorId !== previousMentorId) {
      void loadReviews();
    }
  }
);

const loadPersonalMentorReviews = async () => {
  if (!isMentor.value) {
    return;
  }

  isLoadingOwnProfile.value = true;
  ownProfileError.value = "";

  try {
    ownMentorProfile.value = await mentorProfileApi.getMine();
    filters.mentorId = String(ownMentorProfile.value.id);
    await loadReviews();
  } catch (rawError) {
    ownProfileError.value = rawError instanceof Error ? rawError.message : "Не удалось загрузить профиль ментора.";
  } finally {
    isLoadingOwnProfile.value = false;
  }
};

onMounted(async () => {
  if (isPersonalMentorView.value) {
    await loadPersonalMentorReviews();
    return;
  }

  await loadMentors();
  await loadReviews();
});
</script>

<template>
  <section class="app-section reviews-page">
    <div class="workspace-header">
      <div>
        <h1 class="workspace-title">{{ isPersonalMentorView ? "Мои отзывы" : "Отзывы и рейтинг" }}</h1>
        <p class="workspace-subtitle">
          {{ isPersonalMentorView
            ? "Оценки студентов, средний рейтинг и динамика качества вашего менторства."
            : "Качество менторства, реальные завершенные заявки и отзывы, которые помогают принять решение." }}
        </p>
      </div>
      <BaseButton
        v-if="isPersonalMentorView"
        class="reviews-refresh"
        variant="secondary"
        size="l"
        :label="isLoadingOwnProfile || isLoading ? 'Обновляем...' : 'Обновить'"
        :loading="isLoadingOwnProfile || isLoading"
        @click="loadPersonalMentorReviews"
      />
    </div>

    <div v-if="ownProfileError" class="error-state">{{ ownProfileError }}</div>

    <ReviewStats :summary="summary" :is-loading="isLoading || isLoadingOwnProfile" />

    <ReviewList
      :reviews="visibleReviews"
      :mentor-options="mentorOptions"
      :mentor-id="filters.mentorId"
      :mentor-name="selectedMentorName"
      :current-user-id="user?.id"
      :sort="filters.sort"
      :is-loading="isLoading"
      :is-loading-mentors="isLoadingMentors"
      :error-message="error?.message"
      :hide-mentor-filter="isPersonalMentorView"
      @update:mentor-id="filters.mentorId = $event"
      @update:sort="filters.sort = $event"
      @retry="loadReviews"
    />
  </section>
</template>
