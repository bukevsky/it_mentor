import type {
  MentoringRequestClarifyRequest,
  MentoringRequestCreateRequest,
  MentoringRequestRejectRequest,
  MentoringRequestResponse,
  MentoringRequestStatus,
  PagedResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const mentoringApi = {
  create(payload: MentoringRequestCreateRequest) {
    return request<MentoringRequestResponse>("/mentoring/requests", {
      method: "POST",
      body: payload
    });
  },
  getList(params: { status?: MentoringRequestStatus; page?: number; size?: number }) {
    return request<PagedResponse<MentoringRequestResponse>>(
      `/mentoring/requests${buildQuery(params)}`
    );
  },
  getById(id: number) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}`);
  },
  markViewed(id: number) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/view`, {
      method: "PUT"
    });
  },
  requestClarification(id: number, payload: MentoringRequestClarifyRequest) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/needs-clarification`, {
      method: "PUT",
      body: payload
    });
  },
  accept(id: number) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/accept`, {
      method: "PUT"
    });
  },
  reject(id: number, payload: MentoringRequestRejectRequest) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/reject`, {
      method: "PUT",
      body: payload
    });
  },
  cancel(id: number) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/cancel`, {
      method: "PUT"
    });
  },
  complete(id: number) {
    return request<MentoringRequestResponse>(`/mentoring/requests/${id}/complete`, {
      method: "PUT"
    });
  }
};
