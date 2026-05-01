<script setup lang="ts">
import type { ReviewSummaryResponse } from "@/shared/api/contracts";
import ReviewRating from "./ReviewRating.vue";

defineProps<{
  summary: ReviewSummaryResponse;
  isLoading?: boolean;
}>();

const ratingRows = [5, 4, 3, 2, 1] as const;

const getPercent = (count: number, total: number) => {
  if (!total) {
    return 0;
  }

  return Math.round((count / total) * 100);
};
</script>

<template>
  <section class="reviews-stats">
    <article class="reviews-score">
      <span class="section-kicker">Общий рейтинг</span>
      <div class="reviews-score__value">
        {{ isLoading ? "..." : summary.averageRating.toFixed(1) }}
      </div>
      <ReviewRating :value="summary.averageRating" readonly />
      <p class="section-copy">{{ summary.totalReviews }} отзывов, {{ summary.moderationCount }} на модерации</p>
    </article>

    <article class="reviews-distribution">
      <div v-for="rating in ratingRows" :key="rating" class="reviews-distribution__row">
        <span>{{ rating }}★</span>
        <div class="reviews-distribution__bar">
          <span :style="{ width: `${getPercent(summary.distribution[rating], summary.totalReviews)}%` }"></span>
        </div>
        <strong>{{ summary.distribution[rating] }}</strong>
      </div>
    </article>
  </section>
</template>
