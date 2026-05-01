<script setup lang="ts">
import { ref } from "vue";
import type { ReviewResponse } from "@/shared/api/contracts";
import { formatDateTime } from "@/shared/lib/presenters";
import ReviewRating from "./ReviewRating.vue";

defineProps<{
  review: ReviewResponse;
}>();

const isExpanded = ref(false);

const statusLabel = {
  PUBLISHED: "Опубликован",
  MODERATION: "На модерации",
  REJECTED: "Отклонен"
};
</script>

<template>
  <article class="review-card">
    <div class="review-card__header">
      <div>
        <strong>{{ review.author.name }} → {{ review.recipient.name }}</strong>
        <p>{{ formatDateTime(review.createdAt) }} · заявка #{{ review.mentoringRequestId }}</p>
      </div>
      <span
        class="status-pill"
        :class="{
          'status-pill--success': review.status === 'PUBLISHED',
          'status-pill--warning': review.status === 'MODERATION',
          'status-pill--danger': review.status === 'REJECTED'
        }"
      >
        {{ statusLabel[review.status] }}
      </span>
    </div>

    <ReviewRating :value="review.rating" readonly />

    <p class="review-card__text" :class="{ 'review-card__text--expanded': isExpanded }">
      {{ review.comment }}
    </p>

    <button v-if="review.comment.length > 120" class="link-button" type="button" @click="isExpanded = !isExpanded">
      {{ isExpanded ? "Свернуть" : "Читать полностью" }}
    </button>
  </article>
</template>
