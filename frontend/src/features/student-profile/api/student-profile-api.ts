import type {
  PagedResponse,
  PatchStudentProfileRequest,
  PutStudentLanguagesRequest,
  PutStudentSkillsRequest,
  StudentCompletionResponse,
  StudentFilesResponse,
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
  patch(payload: PatchStudentProfileRequest) {
    return request<StudentProfileResponse>("/profile/student", {
      method: "PATCH",
      body: payload
    });
  },
  getMine() {
    return request<StudentProfileResponse>("/profile/student/me");
  },
  getCompletion() {
    return request<StudentCompletionResponse>("/profile/student/me/completion");
  },
  replaceSkills(payload: PutStudentSkillsRequest) {
    return request<StudentProfileResponse>("/profile/student/skills", {
      method: "PUT",
      body: payload
    });
  },
  replaceLanguages(payload: PutStudentLanguagesRequest) {
    return request<StudentProfileResponse>("/profile/student/languages", {
      method: "PUT",
      body: payload
    });
  },
  getFiles() {
    return request<StudentFilesResponse>("/profile/student/me/files");
  },
  getById(id: number) {
    return request<StudentProfileResponse>(`/profiles/students/${id}`);
  },
  search(params: StudentSearchParams) {
    return request<PagedResponse<StudentProfileResponse>>(`/profiles/students${buildQuery(params)}`);
  }
};
