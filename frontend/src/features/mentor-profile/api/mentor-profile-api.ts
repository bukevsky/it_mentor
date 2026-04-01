import type {
  MentorCardResponse,
  MentorProfileRequest,
  MentorProfileResponse,
  MentorSearchParams,
  PagedResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const mentorProfileApi = {
  upsert(payload: MentorProfileRequest) {
    return request<MentorProfileResponse>("/profile/mentor", {
      method: "PUT",
      body: payload
    });
  },
  getMine() {
    return request<MentorProfileResponse>("/profile/mentor/me");
  },
  getById(id: number) {
    return request<MentorProfileResponse>(`/profiles/mentors/${id}`);
  },
  search(params: MentorSearchParams) {
    return request<PagedResponse<MentorCardResponse>>(`/profiles/mentors${buildQuery(params)}`);
  }
};
