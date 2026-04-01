import type { UserInfo } from "@/entities/user/model/types";
import type { HealthResponse, LoginRequest, LoginResponse } from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const authApi = {
  login(payload: LoginRequest) {
    return request<LoginResponse>("/auth/login", {
      method: "POST",
      auth: false,
      body: payload
    });
  },
  getCurrentUser() {
    return request<UserInfo>("/auth/me");
  },
  getHealth() {
    return request<HealthResponse>("/actuator/health", {
      auth: false
    });
  }
};
