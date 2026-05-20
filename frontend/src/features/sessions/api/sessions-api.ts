import type {
  CancelSessionRequest,
  CreateSessionRequest,
  PagedResponse,
  RescheduleSessionRequest,
  SessionResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const sessionsApi = {
  create(payload: CreateSessionRequest) {
    return request<SessionResponse>("/sessions", {
      method: "POST",
      body: payload
    });
  },
  list(params: { page?: number; size?: number; sort?: string } = {}) {
    return request<PagedResponse<SessionResponse>>(`/sessions${buildQuery(params)}`);
  },
  getById(sessionId: number) {
    return request<SessionResponse>(`/sessions/${sessionId}`);
  },
  reschedule(sessionId: number, payload: RescheduleSessionRequest) {
    return request<SessionResponse>(`/sessions/${sessionId}/reschedule`, {
      method: "PUT",
      body: payload
    });
  },
  cancel(sessionId: number, payload: CancelSessionRequest = {}) {
    return request<SessionResponse>(`/sessions/${sessionId}/cancel`, {
      method: "PUT",
      body: payload
    });
  },
  complete(sessionId: number) {
    return request<SessionResponse>(`/sessions/${sessionId}/complete`, {
      method: "PUT"
    });
  }
};
