import { computed, ref } from "vue";
import type {
  ErrorResponse,
  MentoringRequestResponse,
  ReviewCreateRequest,
  ReviewResponse,
  ReviewableRequest
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { fullName } from "@/shared/lib/presenters";
import { reviewsApi } from "../api/reviews-api";

const mapReviewableRequest = (request: MentoringRequestResponse): ReviewableRequest => ({
  id: request.id,
  mentorProfileId: request.mentorProfileId,
  mentorName: fullName(request.mentorProfile),
  goalType: request.goalType,
  completedAt: request.completedAt
});

export const useCreateReview = () => {
  const authStore = useAuthStore();
  const reviewableRequests = ref<ReviewableRequest[]>([]);
  const isLoadingRequests = ref(false);
  const isSubmitting = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");

  const canCreateReviews = computed(() => authStore.user?.roles.includes("STUDENT") ?? false);

  const loadReviewableRequests = async () => {
    if (!authStore.isAuthenticated || !canCreateReviews.value) {
      reviewableRequests.value = [];
      return;
    }

    isLoadingRequests.value = true;
    error.value = null;

    try {
      const response = await mentoringApi.getList({
        status: "COMPLETED",
        page: 0,
        size: 100
      });

      reviewableRequests.value = response.content.map(mapReviewableRequest);
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests?status=COMPLETED");
    } finally {
      isLoadingRequests.value = false;
    }
  };

  const createReview = async (payload: ReviewCreateRequest): Promise<ReviewResponse | null> => {
    if (!canCreateReviews.value) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 403,
        error: "FORBIDDEN",
        message: "Оставлять отзывы могут только студенты.",
        path: "/reviews"
      };
      return null;
    }

    const request = reviewableRequests.value.find((item) => item.id === payload.mentoringRequestId);

    if (!request) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 400,
        error: "Bad Request",
        message: "Выберите завершенную заявку для отзыва.",
        path: "/reviews"
      };
      return null;
    }

    isSubmitting.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      const review = await reviewsApi.createReview(payload);
      reviewableRequests.value = reviewableRequests.value.filter((item) => item.id !== request.id);
      successMessage.value = "Отзыв опубликован.";
      return review;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/reviews");
      return null;
    } finally {
      isSubmitting.value = false;
    }
  };

  return {
    canCreateReviews,
    error,
    isLoadingRequests,
    isSubmitting,
    reviewableRequests,
    successMessage,
    createReview,
    loadReviewableRequests
  };
};
