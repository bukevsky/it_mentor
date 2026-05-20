<script setup lang="ts">
import { ref } from "vue";
import type { ReviewResponse } from "@/shared/api/contracts";
import { formatDateTime } from "@/shared/lib/presenters";
import ReviewRating from "./ReviewRating.vue";

defineProps<{
  review: ReviewResponse;
  mentorName: string;
  currentUserId?: number;
}>();

const isExpanded = ref(false);
</script>

<template>
  <article class="review-card">
    <div class="review-card__header">
      <div>
        <strong>
          {{ review.reviewerUserId === currentUserId ? "Вы" : `Студент #${review.reviewerUserId}` }}
          →
          {{ mentorName }}
        </strong>
        <p>{{ formatDateTime(review.createdAt) }} · заявка #{{ review.mentoringRequestId }}</p>
      </div>
    </div>

    <ReviewRating :value="review.rating" readonly />

    <p class="review-card__text" :class="{ 'review-card__text--expanded': isExpanded }">
      {{ review.comment || "Без комментария." }}
    </p>

    <button v-if="(review.comment?.length ?? 0) > 120" class="link-button" type="button" @click="isExpanded = !isExpanded">
      {{ isExpanded ? "Свернуть" : "Читать полностью" }}
    </button>
  </article>
</template>
