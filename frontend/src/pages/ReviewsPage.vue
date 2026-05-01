<script setup lang="ts">
import { onMounted, ref } from "vue";
import { BaseButton } from "conductor";
import { useCreateReview } from "@/features/reviews/model/use-create-review";
import { useReviews } from "@/features/reviews/model/use-reviews";
import ReviewForm from "@/features/reviews/ui/ReviewForm.vue";
import ReviewList from "@/features/reviews/ui/ReviewList.vue";
import ReviewStats from "@/features/reviews/ui/ReviewStats.vue";
import type { ReviewCreateRequest } from "@/shared/api/contracts";

const {
  error,
  filters,
  isLoading,
  mentorOptions,
  summary,
  visibleReviews,
  addReview,
  loadReviews
} = useReviews();

const {
  error: createError,
  isLoadingRequests,
  isSubmitting,
  reviewableRequests,
  successMessage,
  createReview,
  loadReviewableRequests
} = useCreateReview();

const isFormOpen = ref(false);

const submitReview = async (payload: ReviewCreateRequest) => {
  const review = await createReview(payload);

  if (review) {
    await addReview(review);
  }
};

onMounted(() => {
  void loadReviews();
  void loadReviewableRequests();
});
</script>

<template>
  <section class="app-section reviews-page">
    <div class="workspace-header">
      <div>
        <h1 class="workspace-title">Отзывы и рейтинг</h1>
        <p class="workspace-subtitle">
          Качество менторства, реальные завершенные заявки и отзывы, которые помогают принять решение.
        </p>
      </div>
      <BaseButton
        size="l"
        :label="isFormOpen ? 'Закрыть форму' : 'Оставить отзыв'"
        @click="isFormOpen = !isFormOpen"
      />
    </div>

    <ReviewStats :summary="summary" :is-loading="isLoading" />

    <ReviewForm
      v-if="isFormOpen"
      :requests="reviewableRequests"
      :is-loading="isLoadingRequests"
      :is-submitting="isSubmitting"
      :error="createError"
      :success-message="successMessage"
      @submit="submitReview"
    />

    <ReviewList
      :reviews="visibleReviews"
      :mentor-options="mentorOptions"
      :mentor-id="filters.mentorId"
      :sort="filters.sort"
      :is-loading="isLoading"
      :error-message="error?.message"
      @update:mentor-id="filters.mentorId = $event"
      @update:sort="filters.sort = $event"
      @retry="loadReviews"
    />
  </section>
</template>
