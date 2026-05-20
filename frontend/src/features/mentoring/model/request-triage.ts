import type {
  MentoringRequestResponse,
  MentoringRequestStatus,
  RoleCode
} from "@/shared/api/contracts";
import type { Option } from "@/shared/lib/options";

export type RequestAction = "accept" | "clarify" | "reject" | "complete" | "cancel";
export type RequestActionMode = "idle" | "clarify" | "reject";
export type RequestSortOrder = "newest" | "oldest";
export type RequestScope = "all" | "incoming" | "outgoing";
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

export const requestScopeOptions: Option<RequestScope>[] = [
  { value: "all", label: "Все" },
  { value: "incoming", label: "Входящие" },
  { value: "outgoing", label: "Исходящие" }
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
  const isInitiator =
    (request.direction === "STUDENT_TO_MENTOR" && roles.includes("STUDENT")) ||
    (request.direction === "MENTOR_TO_STUDENT" && roles.includes("MENTOR"));

  if (isRecipient && isTriageStatus(request.status)) {
    return ["accept", "clarify", "reject"];
  }

  if (isInitiator && isTriageStatus(request.status)) {
    return ["cancel"];
  }

  if (request.status === "ACCEPTED") {
    return ["complete"];
  }

  return [];
};

export const getRequestScope = (
  request: MentoringRequestResponse,
  roles: RoleCode[] = []
): Exclude<RequestScope, "all"> => {
  const isOutgoing =
    (request.direction === "STUDENT_TO_MENTOR" && roles.includes("STUDENT")) ||
    (request.direction === "MENTOR_TO_STUDENT" && roles.includes("MENTOR"));

  return isOutgoing ? "outgoing" : "incoming";
};

export const filterAndSortRequests = (
  requests: MentoringRequestResponse[],
  filters: { status: MentoringRequestStatus | ""; sortOrder: RequestSortOrder; scope?: RequestScope },
  roles: RoleCode[] = []
) => {
  const filtered = requests.filter((request) => {
    const matchesStatus = filters.status ? request.status === filters.status : true;
    const matchesScope = !filters.scope || filters.scope === "all"
      ? true
      : getRequestScope(request, roles) === filters.scope;

    return matchesStatus && matchesScope;
  });

  return [...filtered].sort((left, right) => {
    const leftTime = new Date(left.createdAt).getTime();
    const rightTime = new Date(right.createdAt).getTime();
    return filters.sortOrder === "newest" ? rightTime - leftTime : leftTime - rightTime;
  });
};

export const getRequestCounters = (requests: MentoringRequestResponse[], roles: RoleCode[] = []) => ({
  total: requests.length,
  new: requests.filter((request) => request.status === "SENT").length,
  reviewing: requests.filter((request) => request.status === "REVIEWING").length,
  accepted: requests.filter((request) => request.status === "ACCEPTED").length,
  incoming: requests.filter((request) => getRequestScope(request, roles) === "incoming").length,
  outgoing: requests.filter((request) => getRequestScope(request, roles) === "outgoing").length
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
