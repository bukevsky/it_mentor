import type {
  MentoringRequestResponse,
  MentoringRequestStatus,
  RoleCode
} from "@/shared/api/contracts";
import type { Option } from "@/shared/lib/options";

export type RequestAction = "accept" | "clarify" | "reject" | "complete";
export type RequestActionMode = "idle" | "clarify" | "reject";
export type RequestSortOrder = "newest" | "oldest";
export type RequestStatusTone = "neutral" | "info" | "warning" | "success" | "danger" | "muted";

export interface RequestStatusMeta {
  label: string;
  tone: RequestStatusTone;
  lifecycle: string;
  isTerminal: boolean;
}

export const requestStatusDictionary: Record<MentoringRequestStatus, RequestStatusMeta> = {
  SENT: {
    label: "Новая",
    tone: "info",
    lifecycle: "Заявка ожидает первичного решения: принять, уточнить или отклонить.",
    isTerminal: false
  },
  REVIEWING: {
    label: "На рассмотрении",
    tone: "warning",
    lifecycle: "Заявка открыта и находится в разборе у получателя.",
    isTerminal: false
  },
  NEEDS_CLARIFICATION: {
    label: "Нужно уточнение",
    tone: "warning",
    lifecycle: "Получатель запросил детали, финальное решение еще не принято.",
    isTerminal: false
  },
  ACCEPTED: {
    label: "Принята",
    tone: "success",
    lifecycle: "Заявка согласована, по ней можно вести работу и завершить менторство.",
    isTerminal: false
  },
  REJECTED: {
    label: "Отклонена",
    tone: "danger",
    lifecycle: "Заявка закрыта отказом, повторные действия недоступны.",
    isTerminal: true
  },
  CANCELLED: {
    label: "Отменена",
    tone: "muted",
    lifecycle: "Инициатор отменил заявку до финального решения.",
    isTerminal: true
  },
  COMPLETED: {
    label: "Завершена",
    tone: "neutral",
    lifecycle: "Менторство по заявке завершено.",
    isTerminal: true
  }
};

export const requestSortOptions: Option<RequestSortOrder>[] = [
  { value: "newest", label: "Сначала новые" },
  { value: "oldest", label: "Сначала старые" }
];

export const requestStatusFilterOptions: Array<Option<MentoringRequestStatus | "">> = [
  { value: "", label: "Все статусы" },
  ...Object.entries(requestStatusDictionary).map(([value, meta]) => ({
    value: value as MentoringRequestStatus,
    label: meta.label
  }))
];

export const getRequestStatusMeta = (
  status?: string | null
): RequestStatusMeta => {
  if (!status || !(status in requestStatusDictionary)) {
    return {
      label: status ?? "Без статуса",
      tone: "neutral",
      lifecycle: "Статус заявки не распознан.",
      isTerminal: false
    };
  }

  return requestStatusDictionary[status as MentoringRequestStatus];
};

export const getRequestStatusClass = (status?: string | null) => {
  return `status-pill--${getRequestStatusMeta(status).tone}`;
};

export const isTriageStatus = (status: MentoringRequestStatus) => {
  return status === "SENT" || status === "REVIEWING" || status === "NEEDS_CLARIFICATION";
};

export const getAvailableRequestActions = (
  request: MentoringRequestResponse | null,
  roles: RoleCode[] = []
): RequestAction[] => {
  if (!request) {
    return [];
  }

  const isRecipient =
    (request.direction === "STUDENT_TO_MENTOR" && roles.includes("MENTOR")) ||
    (request.direction === "MENTOR_TO_STUDENT" && roles.includes("STUDENT"));

  if (isRecipient && isTriageStatus(request.status)) {
    return ["accept", "clarify", "reject"];
  }

  if (request.status === "ACCEPTED") {
    return ["complete"];
  }

  return [];
};

export const filterAndSortRequests = (
  requests: MentoringRequestResponse[],
  filters: { status: MentoringRequestStatus | ""; sortOrder: RequestSortOrder }
) => {
  const filtered = filters.status
    ? requests.filter((request) => request.status === filters.status)
    : requests;

  return [...filtered].sort((left, right) => {
    const leftTime = new Date(left.createdAt).getTime();
    const rightTime = new Date(right.createdAt).getTime();
    return filters.sortOrder === "newest" ? rightTime - leftTime : leftTime - rightTime;
  });
};

export const getRequestCounters = (requests: MentoringRequestResponse[]) => ({
  total: requests.length,
  new: requests.filter((request) => request.status === "SENT").length,
  reviewing: requests.filter((request) => request.status === "REVIEWING").length
});

export const findNextRequestId = (
  previousVisibleIds: number[],
  currentId: number,
  nextVisibleIds: number[]
) => {
  const currentIndex = previousVisibleIds.indexOf(currentId);
  const orderedCandidates =
    currentIndex >= 0
      ? [...previousVisibleIds.slice(currentIndex + 1), ...previousVisibleIds.slice(0, currentIndex)]
      : previousVisibleIds;

  return (
    orderedCandidates.find((id) => id !== currentId && nextVisibleIds.includes(id)) ??
    nextVisibleIds.find((id) => id !== currentId) ??
    null
  );
};
