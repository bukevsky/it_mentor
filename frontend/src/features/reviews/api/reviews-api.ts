import type {
  PagedResponse,
  ReviewCreateRequest,
  ReviewResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const reviewsApi = {
  createReview(payload: ReviewCreateRequest) {
    return request<ReviewResponse>("/reviews", {
      method: "POST",
      body: payload
    });
  },
  getByRequest(requestId: number) {
    return request<ReviewResponse>(`/reviews/by-request/${requestId}`);
  },
  getMentorReviews(mentorProfileId: number, params: { page?: number; size?: number }) {
    return request<PagedResponse<ReviewResponse>>(
      `/profiles/mentors/${mentorProfileId}/reviews${buildQuery(params)}`,
      { auth: false }
    );
  },
  removeReview(reviewId: number) {
    return request<void>(`/reviews/${reviewId}`, {
      method: "DELETE"
    });
  }
};
