import type { AdminRoleRequest } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const adminApi = {
  assignRole(userId: number, payload: AdminRoleRequest) {
    return request<void>(`/admin/users/${userId}/role`, {
      method: "PUT",
      body: payload
    });
  }
};
