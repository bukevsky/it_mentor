import type { ComplaintResponse, CreateComplaintRequest } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const complaintsApi = {
  create(payload: CreateComplaintRequest) {
    return request<ComplaintResponse>("/complaints", {
      method: "POST",
      body: payload
    });
  }
};
