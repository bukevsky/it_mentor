import { computed, reactive, ref } from "vue";
import type {
  ErrorResponse,
  MentorCardResponse,
  ReviewResponse,
  ReviewSummaryResponse
} from "@/shared/api/contracts";
import { mentorProfileApi } from "@/features/mentor-profile/api/mentor-profile-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { fullName } from "@/shared/lib/presenters";
import { reviewsApi } from "../api/reviews-api";

export type ReviewSort = "newest" | "rating";

const emptySummary: ReviewSummaryResponse = {
  averageRating: 0,
  totalReviews: 0,
  distribution: {
    1: 0,
    2: 0,
    3: 0,
    4: 0,
    5: 0
  }
};

const buildSummary = (reviews: ReviewResponse[]): ReviewSummaryResponse => {
  const distribution: ReviewSummaryResponse["distribution"] = {
    1: 0,
    2: 0,
    3: 0,
    4: 0,
    5: 0
  };

  reviews.forEach((review) => {
    const rating = Math.min(5, Math.max(1, Math.round(review.rating))) as 1 | 2 | 3 | 4 | 5;
    distribution[rating] += 1;
  });

  const totalReviews = reviews.length;
  const ratingSum = reviews.reduce((sum, review) => sum + review.rating, 0);

  return {
    averageRating: totalReviews ? Number((ratingSum / totalReviews).toFixed(1)) : 0,
    totalReviews,
    distribution
  };
};

export const useReviews = () => {
  const mentors = ref<MentorCardResponse[]>([]);
  const reviews = ref<ReviewResponse[]>([]);
  const isLoadingMentors = ref(false);
  const isLoading = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const filters = reactive({
    mentorId: "",
    sort: "newest" as ReviewSort
  });

  const mentorOptions = computed(() =>
    mentors.value.map((mentor) => ({
      value: String(mentor.id),
      label: fullName(mentor)
    }))
  );

  const selectedMentor = computed(
    () => mentors.value.find((mentor) => String(mentor.id) === filters.mentorId) ?? null
  );

  const summary = computed(() => buildSummary(reviews.value));

  const visibleReviews = computed(() => {
    return [...reviews.value].sort((left, right) => {
      if (filters.sort === "rating") {
        return right.rating - left.rating || new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
      }

      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
  });

  const loadMentors = async () => {
    isLoadingMentors.value = true;
    error.value = null;

    try {
      const content: MentorCardResponse[] = [];
      let page = 0;
      let last = false;

      while (!last) {
        const response = await mentorProfileApi.search({
          page,
          size: 50,
          sort: "createdAt,desc"
        });

        content.push(...response.content);
        last = response.last;
        page += 1;
      }

      mentors.value = content;

      if (!filters.mentorId && content.length) {
        filters.mentorId = String(content[0].id);
      }
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/profiles/mentors");
    } finally {
      isLoadingMentors.value = false;
    }
  };

  const loadReviews = async () => {
    if (!filters.mentorId) {
      reviews.value = [];
      return;
    }

    isLoading.value = true;
    error.value = null;

    try {
      const content: ReviewResponse[] = [];
      let page = 0;
      let last = false;

      while (!last) {
        const response = await reviewsApi.getMentorReviews(Number(filters.mentorId), {
          page,
          size: 100
        });

        content.push(...response.content);
        last = response.last;
        page += 1;
      }

      reviews.value = content;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/profiles/mentors/${filters.mentorId}/reviews`);
    } finally {
      isLoading.value = false;
    }
  };

  const selectMentor = async (mentorId: number | string) => {
    filters.mentorId = String(mentorId);
    await loadReviews();
  };

  return {
    error,
    filters,
    isLoading,
    isLoadingMentors,
    mentorOptions,
    mentors,
    reviews,
    selectedMentor,
    summary,
    visibleReviews,
    loadMentors,
    loadReviews,
    selectMentor
  };
};
