<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { BaseButton } from "conductor";
import type { ErrorResponse, ReviewCreateRequest, ReviewableRequest } from "@/shared/api/contracts";
import { formatDateTime, getOptionLabel } from "@/shared/lib/presenters";
import { mentoringTypeOptions } from "@/shared/lib/options";
import ReviewRating from "./ReviewRating.vue";

const props = defineProps<{
  requests: ReviewableRequest[];
  isLoading?: boolean;
  isSubmitting?: boolean;
  error?: ErrorResponse | null;
  successMessage?: string;
}>();

const emit = defineEmits<{
  (event: "submit", payload: ReviewCreateRequest): void;
}>();

const selectedRequestId = ref<number | null>(null);
const rating = ref(0);
const comment = ref("");
const localError = ref("");

const selectedRequest = computed(() =>
  props.requests.find((request) => request.id === selectedRequestId.value) ?? null
);

watch(
  () => props.requests,
  (requests) => {
    if (!selectedRequestId.value && requests.length) {
      selectedRequestId.value = requests[0].id;
    }
  },
  { immediate: true }
);

watch(
  () => props.successMessage,
  (message) => {
    if (message) {
      rating.value = 0;
      comment.value = "";
      localError.value = "";
    }
  }
);

const submit = () => {
  if (!selectedRequest.value) {
    localError.value = "Выберите завершенную заявку.";
    return;
  }

  if (!rating.value) {
    localError.value = "Поставьте оценку от 1 до 5.";
    return;
  }

  if (comment.value.trim().length < 10) {
    localError.value = "Добавьте короткий комментарий, чтобы отзыв был полезным.";
    return;
  }

  localError.value = "";
  emit("submit", {
    mentoringRequestId: selectedRequest.value.id,
    rating: rating.value,
    comment: comment.value.trim()
  });
};
</script>

<template>
  <form class="review-form" @submit.prevent="submit">
    <div class="review-form__head">
      <div>
        <p class="section-kicker">Новый отзыв</p>
        <h2 class="section-title">Оцените завершенную заявку</h2>
      </div>
      <span v-if="successMessage" class="status-pill status-pill--success">{{ successMessage }}</span>
    </div>

    <div v-if="isLoading" class="panel-state">Загружаем завершенные заявки...</div>
    <div v-else-if="!requests.length" class="empty-state empty-state--compact">
      <h3>Нет завершенных заявок</h3>
      <p>Оставить отзыв можно после завершения менторства.</p>
    </div>
    <template v-else>
      <label class="select-shell">
        <span>Заявка</span>
        <select
          :value="selectedRequestId ?? ''"
          @change="selectedRequestId = Number(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="request in requests" :key="request.id" :value="request.id">
            #{{ request.id }} · {{ request.participant.name }}
          </option>
        </select>
      </label>

      <div v-if="selectedRequest" class="review-form__request">
        <strong>Заявка #{{ selectedRequest.id }}</strong>
        <span>{{ selectedRequest.participant.name }} · {{ selectedRequest.participant.role === "MENTOR" ? "ментор" : "студент" }}</span>
        <span>{{ getOptionLabel(mentoringTypeOptions, selectedRequest.goalType) }}</span>
        <span>{{ formatDateTime(selectedRequest.completedAt) }}</span>
      </div>

      <div class="review-form__rating">
        <span>Рейтинг</span>
        <ReviewRating v-model="rating" :show-number="false" />
      </div>

      <label class="field">
        <span>Комментарий</span>
        <textarea v-model="comment" placeholder="Что было полезно? Что можно улучшить?"></textarea>
      </label>

      <p v-if="localError || error" class="form-error">{{ localError || error?.message }}</p>

      <BaseButton
        size="l"
        type="submit"
        :disabled="isSubmitting"
        :label="isSubmitting ? 'Отправляем...' : 'Отправить отзыв'"
      />
    </template>
  </form>
</template>
