import type { StudentProfileRequest, StudentProfileResponse } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

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
  }
};
