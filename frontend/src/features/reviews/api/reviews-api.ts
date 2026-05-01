import type {
  ReviewCreateRequest,
  ReviewParticipant,
  ReviewResponse,
  ReviewStatus,
  ReviewSummaryResponse
} from "@/shared/api/contracts";

const STORAGE_KEY = "it-mentor.reviews";

const seedReviews: ReviewResponse[] = [
  {
    id: 1,
    mentoringRequestId: 1184,
    author: { id: 301, name: "Александр Петров", role: "STUDENT" },
    recipient: { id: 42, name: "Дмитрий Морозов", role: "MENTOR" },
    rating: 5,
    status: "PUBLISHED",
    comment: "Помог разобрать архитектуру проекта и составить понятный план развития на месяц.",
    createdAt: "2026-04-20T13:24:00+03:00"
  },
  {
    id: 2,
    mentoringRequestId: 1192,
    author: { id: 302, name: "Мария Петрова", role: "STUDENT" },
    recipient: { id: 48, name: "Елена Соколова", role: "MENTOR" },
    rating: 4,
    status: "MODERATION",
    comment: "Сессии были структурными, много полезных материалов по backend и собеседованиям.",
    createdAt: "2026-04-24T10:10:00+03:00"
  },
  {
    id: 3,
    mentoringRequestId: 1201,
    author: { id: 44, name: "Игорь Лебедев", role: "MENTOR" },
    recipient: { id: 311, name: "Анна Кузнецова", role: "STUDENT" },
    rating: 5,
    status: "PUBLISHED",
    comment: "Студентка быстро внедряла правки, задавала точные вопросы и подготовила сильный финальный проект.",
    createdAt: "2026-04-26T18:35:00+03:00"
  }
];

const getStoredReviews = () => {
  if (typeof window === "undefined") {
    return seedReviews;
  }

  const saved = window.localStorage.getItem(STORAGE_KEY);
  if (!saved) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(seedReviews));
    return seedReviews;
  }

  try {
    return JSON.parse(saved) as ReviewResponse[];
  } catch {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(seedReviews));
    return seedReviews;
  }
};

const saveStoredReviews = (reviews: ReviewResponse[]) => {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(reviews));
  }
};

const getCurrentReviewer = (): ReviewParticipant => ({
  id: 0,
  name: "Вы",
  role: "STUDENT"
});

const buildSummary = (reviews: ReviewResponse[]): ReviewSummaryResponse => {
  const distribution: ReviewSummaryResponse["distribution"] = {
    1: 0,
    2: 0,
    3: 0,
    4: 0,
    5: 0
  };

  reviews.forEach((review) => {
    const normalizedRating = Math.min(5, Math.max(1, Math.round(review.rating))) as 1 | 2 | 3 | 4 | 5;
    distribution[normalizedRating] += 1;
  });

  const totalReviews = reviews.length;
  const ratingSum = reviews.reduce((sum, review) => sum + review.rating, 0);

  return {
    averageRating: totalReviews ? Number((ratingSum / totalReviews).toFixed(1)) : 0,
    totalReviews,
    moderationCount: reviews.filter((review) => review.status === "MODERATION").length,
    distribution
  };
};

export const reviewsApi = {
  async getReviews() {
    return getStoredReviews();
  },

  async getSummary() {
    return buildSummary(getStoredReviews());
  },

  async createReview(
    payload: ReviewCreateRequest,
    recipient: ReviewParticipant,
    author: ReviewParticipant = getCurrentReviewer()
  ) {
    const reviews = getStoredReviews();
    const nextReview: ReviewResponse = {
      id: Date.now(),
      mentoringRequestId: payload.mentoringRequestId,
      author,
      recipient,
      rating: payload.rating,
      comment: payload.comment,
      status: "MODERATION" satisfies ReviewStatus,
      createdAt: new Date().toISOString()
    };

    saveStoredReviews([nextReview, ...reviews]);
    return nextReview;
  }
};
