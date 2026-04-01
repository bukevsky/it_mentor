import type {
  ForgotPasswordRequest,
  HealthResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
  UserInfoResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const authApi = {
  register(payload: RegisterRequest) {
    return request<RegisterResponse>("/auth/register", {
      method: "POST",
      auth: false,
      body: payload
    });
  },
  login(payload: LoginRequest) {
    return request<LoginResponse>("/auth/login", {
      method: "POST",
      auth: false,
      body: payload
    });
  },
  getCurrentUser() {
    return request<UserInfoResponse>("/auth/me");
  },
  forgotPassword(payload: ForgotPasswordRequest) {
    return request<void>("/auth/password/forgot", {
      method: "POST",
      auth: false,
      body: payload
    });
  },
  resetPassword(payload: ResetPasswordRequest) {
    return request<void>("/auth/password/reset", {
      method: "POST",
      auth: false,
      body: payload
    });
  },
  getHealth() {
    return request<HealthResponse>("/actuator/health", {
      auth: false
    });
  }
};
