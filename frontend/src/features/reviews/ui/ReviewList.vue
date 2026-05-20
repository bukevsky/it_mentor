<script setup lang="ts">
import type { ReviewResponse } from "@/shared/api/contracts";
import type { ReviewSort } from "@/features/reviews/model/use-reviews";
import ReviewItem from "./ReviewItem.vue";

defineProps<{
  reviews: ReviewResponse[];
  mentorOptions: Array<{ value: string; label: string }>;
  mentorId: string;
  mentorName: string;
  currentUserId?: number;
  sort: ReviewSort;
  isLoading?: boolean;
  isLoadingMentors?: boolean;
  errorMessage?: string;
  hideMentorFilter?: boolean;
}>();

const emit = defineEmits<{
  (event: "update:mentorId", value: string): void;
  (event: "update:sort", value: ReviewSort): void;
  (event: "retry"): void;
}>();
</script>

<template>
  <section class="reviews-feed">
    <div class="reviews-feed__toolbar">
      <div>
        <h2 class="section-title">Отзывы</h2>
        <p class="section-copy">
          {{ hideMentorFilter ? "Свежие оценки и комментарии по вашему менторству." : "Фильтруйте по ментору и смотрите свежие оценки качества." }}
        </p>
      </div>
      <div class="reviews-feed__filters">
        <label v-if="!hideMentorFilter" class="select-shell">
          <span>Ментор</span>
          <select :value="mentorId" @change="emit('update:mentorId', ($event.target as HTMLSelectElement).value)">
            <option value="" disabled>Выберите ментора</option>
            <option v-for="mentor in mentorOptions" :key="mentor.value" :value="mentor.value">
              {{ mentor.label }}
            </option>
          </select>
        </label>
        <label class="select-shell">
          <span>Сортировка</span>
          <select :value="sort" @change="emit('update:sort', ($event.target as HTMLSelectElement).value as ReviewSort)">
            <option value="newest">Сначала новые</option>
            <option value="rating">Сначала высокий рейтинг</option>
          </select>
        </label>
      </div>
    </div>

    <div v-if="isLoadingMentors && !hideMentorFilter" class="panel-state">Загружаем менторов...</div>
    <div v-else-if="!hideMentorFilter && !mentorOptions.length" class="empty-state">
      <h3>Менторы не найдены</h3>
      <p>Когда появятся профили менторов, здесь можно будет читать отзывы.</p>
    </div>
    <div v-else-if="isLoading" class="panel-state">Загружаем отзывы...</div>
    <div v-else-if="errorMessage" class="error-state">
      <p>{{ errorMessage }}</p>
      <button class="link-button" type="button" @click="emit('retry')">Повторить</button>
    </div>
    <div v-else-if="!reviews.length" class="empty-state">
      <h3>Пока нет отзывов</h3>
      <p>Когда завершатся заявки и появятся оценки, они будут здесь.</p>
    </div>
    <div v-else class="reviews-feed__list">
      <ReviewItem
        v-for="review in reviews"
        :key="review.id"
        :review="review"
        :mentor-name="mentorName"
        :current-user-id="currentUserId"
      />
    </div>
  </section>
</template>
