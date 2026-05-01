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
import { reviewsApi } from "../api/reviews-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { fullName } from "@/shared/lib/presenters";

const mapReviewableRequest = (
  request: MentoringRequestResponse,
  isMentor: boolean
): ReviewableRequest => {
  const participant = isMentor
    ? {
        id: request.studentProfile.id,
        name: fullName(request.studentProfile),
        role: "STUDENT" as const
      }
    : {
        id: request.mentorProfile.id,
        name: fullName(request.mentorProfile),
        role: "MENTOR" as const
      };

  return {
    id: request.id,
    participant,
    goalType: request.goalType,
    completedAt: request.completedAt
  };
};

export const useCreateReview = () => {
  const authStore = useAuthStore();
  const reviewableRequests = ref<ReviewableRequest[]>([]);
  const isLoadingRequests = ref(false);
  const isSubmitting = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");

  const isMentor = computed(() => authStore.user?.roles.includes("MENTOR") ?? false);

  const loadReviewableRequests = async () => {
    if (!authStore.isAuthenticated) {
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

      reviewableRequests.value = response.content.map((request) =>
        mapReviewableRequest(request, isMentor.value)
      );
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests?status=COMPLETED");
    } finally {
      isLoadingRequests.value = false;
    }
  };

  const createReview = async (payload: ReviewCreateRequest): Promise<ReviewResponse | null> => {
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
      const review = await reviewsApi.createReview(payload, request.participant, {
        id: authStore.user?.id ?? 0,
        name: "Вы",
        role: isMentor.value ? "MENTOR" : "STUDENT"
      });
      successMessage.value = "Отзыв отправлен на модерацию.";
      return review;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/reviews");
      return null;
    } finally {
      isSubmitting.value = false;
    }
  };

  return {
    error,
    isLoadingRequests,
    isSubmitting,
    reviewableRequests,
    successMessage,
    createReview,
    loadReviewableRequests
  };
};
