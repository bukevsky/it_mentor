import type { ProfileSummaryResponse } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const profileSummaryApi = {
  getMySummary() {
    return request<ProfileSummaryResponse>("/profile/me");
  }
};
