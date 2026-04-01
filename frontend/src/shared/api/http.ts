import { env } from "@/shared/config/env";
import { tokenStorage } from "@/shared/lib/token-storage";

interface RequestOptions extends Omit<RequestInit, "body"> {
  auth?: boolean;
  body?: BodyInit | object | null;
}

export class ApiError extends Error {
  status: number;
  payload: unknown;

  constructor(status: number, payload: unknown) {
    const message =
      typeof payload === "object" &&
      payload !== null &&
      "message" in payload &&
      typeof payload.message === "string"
        ? payload.message
        : `Request failed with status ${status}`;

    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

const isJsonContent = (contentType: string | null) =>
  contentType?.includes("application/json");

const createHeaders = (auth: boolean, headers?: HeadersInit, hasJsonBody?: boolean) => {
  const normalizedHeaders = new Headers(headers);

  if (auth) {
    const token = tokenStorage.get();

    if (token) {
      normalizedHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  if (hasJsonBody && !normalizedHeaders.has("Content-Type")) {
    normalizedHeaders.set("Content-Type", "application/json");
  }

  return normalizedHeaders;
};

export const request = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  const { auth = true, body, headers, ...restOptions } = options;
  const hasJsonBody = Boolean(body) && !(body instanceof FormData) && typeof body !== "string";

  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...restOptions,
    headers: createHeaders(auth, headers, hasJsonBody),
    body: hasJsonBody ? JSON.stringify(body) : (body ?? null)
  });

  if (!response.ok) {
    const payload = isJsonContent(response.headers.get("Content-Type"))
      ? await response.json()
      : null;

    if (response.status === 401) {
      tokenStorage.clear();
    }

    throw new ApiError(response.status, payload);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!isJsonContent(response.headers.get("Content-Type"))) {
    return undefined as T;
  }

  return (await response.json()) as T;
};

export const uploadFile = async <T>(path: string, file: File, auth = true): Promise<T> => {
  const formData = new FormData();
  formData.append("file", file);

  return request<T>(path, {
    auth,
    method: "POST",
    body: formData
  });
};
