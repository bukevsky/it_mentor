import type {
  PagedResponse,
  StudentProfileRequest,
  StudentProfileResponse,
  StudentSearchParams
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const studentProfileApi = {
  upsert(payload: StudentProfileRequest) {
    return request<StudentProfileResponse>("/profile/student", {
      method: "PUT",
      body: payload
    });
  },
  getMine() {
    return request<StudentProfileResponse>("/profile/student/me");
  },
  getById(id: number) {
    return request<StudentProfileResponse>(`/profiles/students/${id}`);
  },
  search(params: StudentSearchParams) {
    return request<PagedResponse<StudentProfileResponse>>(`/profiles/students${buildQuery(params)}`);
  }
};
