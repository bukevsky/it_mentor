import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { UserInfo } from "@/entities/user/model/types";
import type { ErrorResponse, LoginRequest } from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/http";
import { tokenStorage } from "@/shared/lib/token-storage";
import { authApi } from "../api/auth-api";

type BackendHealth = "idle" | "checking" | "up" | "down";

const buildFieldErrors = (details?: string[]) => {
  return (details || []).reduce<Record<string, string>>((accumulator, detail) => {
    const [field, ...messageParts] = detail.split(":");

    if (!field || messageParts.length === 0) {
      return accumulator;
    }

    accumulator[field.trim()] = messageParts.join(":").trim();
    return accumulator;
  }, {});
};

export const useAuthStore = defineStore("auth", () => {
  const user = ref<UserInfo | null>(null);
  const isInitializing = ref(true);
  const isSubmitting = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const backendHealth = ref<BackendHealth>("idle");

  const isAuthenticated = computed(() => Boolean(user.value && tokenStorage.get()));
  const fieldErrors = computed(() => buildFieldErrors(error.value?.details));
  const hasSession = computed(() => Boolean(tokenStorage.get()));

  const setError = (nextError: ErrorResponse | null) => {
    error.value = nextError;
  };

  const clearSession = () => {
    tokenStorage.clear();
    user.value = null;
  };

  const checkBackendHealth = async () => {
    backendHealth.value = "checking";

    try {
      const response = await authApi.getHealth();
      backendHealth.value = response.status === "UP" ? "up" : "down";
    } catch {
      backendHealth.value = "down";
    }
  };

  const fetchCurrentUser = async () => {
    if (!tokenStorage.get()) {
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

      if (rawError instanceof ApiError) {
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
      tokenStorage.set(response.accessToken);
      user.value = response.user;
      return response.user;
    } catch (rawError) {
      clearSession();

      if (rawError instanceof ApiError) {
        setError(rawError.payload);
      } else {
        setError({
          timestamp: new Date().toISOString(),
          status: 0,
          error: "NETWORK_ERROR",
          message: "Не удалось выполнить запрос к backend",
          path: "/auth/login"
        });
      }

      return null;
    } finally {
      isSubmitting.value = false;
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
    isSubmitting,
    user,
    checkBackendHealth,
    fetchCurrentUser,
    initialize,
    login,
    logout
  };
});
