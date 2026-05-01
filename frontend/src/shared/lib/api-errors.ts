import type { ErrorResponse, HealthResponse } from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/http";

export const isRecord = (value: unknown): value is Record<string, unknown> => {
  return Boolean(value && typeof value === "object");
};

export const isErrorResponse = (value: unknown): value is ErrorResponse => {
  return Boolean(
    isRecord(value) &&
      typeof value.status === "number" &&
      typeof value.error === "string" &&
      typeof value.message === "string" &&
      typeof value.path === "string"
  );
};

export const isHealthResponse = (value: unknown): value is HealthResponse => {
  return Boolean(isRecord(value) && typeof value.status === "string");
};

const getHttpErrorMessage = (status: number, fallbackMessage: string) => {
  if (fallbackMessage && !fallbackMessage.startsWith("Request failed with status")) {
    return fallbackMessage;
  }

  if (status === 400) {
    return "Проверьте введенные данные и попробуйте еще раз.";
  }

  if (status === 401 || status === 403) {
    return "Неверный email или пароль.";
  }

  if (status === 404) {
    return "Запрошенные данные не найдены.";
  }

  if (status >= 500) {
    return "Сервер не смог обработать запрос. Попробуйте позже.";
  }

  return "Не удалось выполнить запрос к backend.";
};

export const normalizeErrorResponse = (rawError: unknown, fallbackPath = ""): ErrorResponse => {
  if (rawError instanceof ApiError && isErrorResponse(rawError.payload)) {
    return rawError.payload;
  }

  if (rawError instanceof ApiError) {
    return {
      timestamp: new Date().toISOString(),
      status: rawError.status,
      error: "HTTP_ERROR",
      message:
        rawError.status >= 500 && rawError.payload === null
          ? "Backend API недоступен. Запустите сервер и попробуйте снова."
          : getHttpErrorMessage(rawError.status, rawError.message),
      path: fallbackPath
    };
  }

  if (rawError instanceof Error) {
    return {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "NETWORK_ERROR",
      message: rawError.message,
      path: fallbackPath
    };
  }

  return {
    timestamp: new Date().toISOString(),
    status: 0,
    error: "UNKNOWN_ERROR",
    message: "Не удалось выполнить запрос к backend",
    path: fallbackPath
  };
};

export const buildFieldErrors = (details?: string[]) => {
  return (details || []).reduce<Record<string, string>>((accumulator, detail) => {
    const [field, ...messageParts] = detail.split(":");

    if (!field || messageParts.length === 0) {
      return accumulator;
    }

    accumulator[field.trim()] = messageParts.join(":").trim();
    return accumulator;
  }, {});
};
