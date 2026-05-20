import type { PresenceResponse } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const presenceApi = {
  getByUserId(userId: number) {
    return request<PresenceResponse>(`/presence/${userId}`);
  }
};
