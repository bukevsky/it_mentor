import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { UserInfo } from "@/entities/user/model/types";
import type {
  ErrorResponse,
  ForgotPasswordRequest,
  HealthResponse,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest
} from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/http";
import { buildFieldErrors, isErrorResponse, isHealthResponse, normalizeErrorResponse } from "@/shared/lib/api-errors";
import { tokenStorage } from "@/shared/lib/token-storage";
import { authApi } from "../api/auth-api";

type BackendHealth = "idle" | "checking" | "up" | "down";

const isBackendReachable = (health: HealthResponse) => {
  return (
    health.status === "UP" ||
    health.components?.readinessState?.status === "UP" ||
    health.components?.db?.status === "UP"
  );
};

export const useAuthStore = defineStore("auth", () => {
  const user = ref<UserInfo | null>(null);
  const sessionToken = ref(tokenStorage.get());
  const isInitializing = ref(true);
  const isSubmitting = ref(false);
  const isRecoveringPassword = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const backendHealth = ref<BackendHealth>("idle");

  const isAuthenticated = computed(() => Boolean(user.value && sessionToken.value));
  const fieldErrors = computed(() => buildFieldErrors(error.value?.details));
  const hasSession = computed(() => Boolean(sessionToken.value));

  const setError = (nextError: ErrorResponse | null) => {
    error.value = nextError;
  };

  const setSessionToken = (nextToken: string | null) => {
    sessionToken.value = nextToken;

    if (nextToken) {
      tokenStorage.set(nextToken);
      return;
    }

    tokenStorage.clear();
  };

  const clearSession = () => {
    setSessionToken(null);
    user.value = null;
  };

  const checkBackendHealth = async () => {
    backendHealth.value = "checking";

    try {
      const response = await authApi.getHealth();
      backendHealth.value = isBackendReachable(response) ? "up" : "down";
    } catch (rawError) {
      if (rawError instanceof ApiError && isHealthResponse(rawError.payload)) {
        backendHealth.value = isBackendReachable(rawError.payload) ? "up" : "down";
        return;
      }

      backendHealth.value = "down";
    }
  };

  const fetchCurrentUser = async () => {
    if (!sessionToken.value) {
      user.value = null;
      return null;
    }

    try {
      const currentUser = await authApi.getCurrentUser();
      user.value = currentUser;
      setError(null);
      return currentUser;
    } catch (rawError) {
      clearSession();

      if (rawError instanceof ApiError && isErrorResponse(rawError.payload)) {
        setError(rawError.payload);
      }

      return null;
    }
  };

  const initialize = async () => {
    isInitializing.value = true;
    await checkBackendHealth();
    await fetchCurrentUser();
    isInitializing.value = false;
  };

  const login = async (payload: LoginRequest) => {
    isSubmitting.value = true;
    setError(null);

    try {
      const response = await authApi.login(payload);
      setSessionToken(response.accessToken);
      user.value = response.user;
      return response.user;
    } catch (rawError) {
      clearSession();
      setError(normalizeErrorResponse(rawError, "/auth/login"));

      return null;
    } finally {
      isSubmitting.value = false;
    }
  };

  const register = async (payload: RegisterRequest) => {
    isSubmitting.value = true;
    setError(null);

    try {
      return await authApi.register(payload);
    } catch (rawError) {
      setError(normalizeErrorResponse(rawError, "/auth/register"));
      return null;
    } finally {
      isSubmitting.value = false;
    }
  };

  const forgotPassword = async (payload: ForgotPasswordRequest) => {
    isRecoveringPassword.value = true;
    setError(null);

    try {
      await authApi.forgotPassword(payload);
      return true;
    } catch (rawError) {
      setError(normalizeErrorResponse(rawError, "/auth/password/forgot"));
      return false;
    } finally {
      isRecoveringPassword.value = false;
    }
  };

  const resetPassword = async (payload: ResetPasswordRequest) => {
    isRecoveringPassword.value = true;
    setError(null);

    try {
      await authApi.resetPassword(payload);
      return true;
    } catch (rawError) {
      setError(normalizeErrorResponse(rawError, "/auth/password/reset"));
      return false;
    } finally {
      isRecoveringPassword.value = false;
    }
  };

  const logout = () => {
    clearSession();
    setError(null);
  };

  return {
    backendHealth,
    error,
    fieldErrors,
    hasSession,
    isAuthenticated,
    isInitializing,
    isRecoveringPassword,
    isSubmitting,
    user,
    checkBackendHealth,
    fetchCurrentUser,
    forgotPassword,
    initialize,
    login,
    logout
    ,
    register,
    resetPassword,
    setError
  };
});
