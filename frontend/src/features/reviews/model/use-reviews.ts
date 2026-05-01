import { computed, reactive, ref } from "vue";
import type { ErrorResponse, ReviewResponse, ReviewSummaryResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { reviewsApi } from "../api/reviews-api";

export type ReviewSort = "newest" | "rating";

const emptySummary: ReviewSummaryResponse = {
  averageRating: 0,
  totalReviews: 0,
  moderationCount: 0,
  distribution: {
    1: 0,
    2: 0,
    3: 0,
    4: 0,
    5: 0
  }
};

export const useReviews = () => {
  const reviews = ref<ReviewResponse[]>([]);
  const summary = ref<ReviewSummaryResponse>(emptySummary);
  const isLoading = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const filters = reactive({
    mentorId: "",
    sort: "newest" as ReviewSort
  });

  const mentorOptions = computed(() => {
    const recipients = new Map<number, string>();

    reviews.value.forEach((review) => {
      if (review.recipient.role === "MENTOR") {
        recipients.set(review.recipient.id, review.recipient.name);
      }
    });

    return Array.from(recipients, ([id, label]) => ({ value: String(id), label }));
  });

  const visibleReviews = computed(() => {
    const filtered = filters.mentorId
      ? reviews.value.filter((review) => String(review.recipient.id) === filters.mentorId)
      : reviews.value;

    return [...filtered].sort((left, right) => {
      if (filters.sort === "rating") {
        return right.rating - left.rating || new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
      }

      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
  });

  const loadReviews = async () => {
    isLoading.value = true;
    error.value = null;

    try {
      const [reviewList, reviewSummary] = await Promise.all([
        reviewsApi.getReviews(),
        reviewsApi.getSummary()
      ]);

      reviews.value = reviewList;
      summary.value = reviewSummary;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/reviews");
    } finally {
      isLoading.value = false;
    }
  };

  const addReview = async (review: ReviewResponse) => {
    reviews.value = [review, ...reviews.value.filter((item) => item.id !== review.id)];
    summary.value = await reviewsApi.getSummary();
  };

  return {
    error,
    filters,
    isLoading,
    mentorOptions,
    reviews,
    summary,
    visibleReviews,
    addReview,
    loadReviews
  };
};
