import type {
  NotificationPreferencesResponse,
  UpdateNotificationPreferencesRequest
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const notificationPreferencesApi = {
  getMine() {
    return request<NotificationPreferencesResponse>("/profile/me/notifications");
  },
  updateMine(payload: UpdateNotificationPreferencesRequest) {
    return request<NotificationPreferencesResponse>("/profile/me/notifications", {
      method: "PUT",
      body: payload
    });
  }
};
