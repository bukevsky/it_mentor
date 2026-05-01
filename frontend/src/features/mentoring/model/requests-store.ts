import { computed, reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ErrorResponse,
  MentoringRequestClarifyRequest,
  MentoringRequestRejectRequest,
  MentoringRequestResponse,
  MentoringRequestStatus,
  PagedResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import {
  filterAndSortRequests,
  findNextRequestId,
  getRequestStatusMeta,
  type RequestActionMode,
  type RequestSortOrder
} from "./request-triage";
import { mentoringApi } from "../api/mentoring-api";

type FieldErrors = {
  clarificationNote?: string;
  reason?: string;
};

export const useRequestsStore = defineStore("requests", () => {
  const authStore = useAuthStore();

  const filters = reactive<{
    status: MentoringRequestStatus | "";
    sortOrder: RequestSortOrder;
  }>({
    status: "",
    sortOrder: "newest"
  });

  const notes = reactive({
    clarificationNote: "",
    reason: ""
  });

  const listResponse = ref<PagedResponse<MentoringRequestResponse> | null>(null);
  const activeRequest = ref<MentoringRequestResponse | null>(null);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const fieldErrors = ref<FieldErrors>({});
  const actionMode = ref<RequestActionMode>("idle");
  const isLoadingList = ref(false);
  const isLoadingDetail = ref(false);
  const isSubmitting = ref(false);
  let successTimer: ReturnType<typeof setTimeout> | null = null;

  const requests = computed(() => listResponse.value?.content ?? []);
  const visibleRequests = computed(() => filterAndSortRequests(requests.value, filters));
  const isBusy = computed(() => isLoadingList.value || isLoadingDetail.value || isSubmitting.value);

  const clearSuccessSoon = () => {
    if (successTimer) {
      clearTimeout(successTimer);
    }

    successTimer = setTimeout(() => {
      successMessage.value = "";
      successTimer = null;
    }, 4200);
  };

  const setSuccess = (message: string) => {
    successMessage.value = message;
    clearSuccessSoon();
  };

  const resetActionMode = () => {
    actionMode.value = "idle";
    fieldErrors.value = {};
  };

  const setActionMode = (mode: RequestActionMode) => {
    actionMode.value = mode;
    fieldErrors.value = {};

    if (mode === "clarify" && !notes.clarificationNote.trim()) {
      notes.clarificationNote = "";
    }

    if (mode === "reject" && !notes.reason.trim()) {
      notes.reason = "";
    }
  };

  const openRequest = async (requestId: number) => {
    isLoadingDetail.value = true;
    error.value = null;
    resetActionMode();

    try {
      activeRequest.value = await mentoringApi.getById(requestId);
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/mentoring/requests/${requestId}`);
    } finally {
      isLoadingDetail.value = false;
    }
  };

  const clearActiveRequest = () => {
    activeRequest.value = null;
    resetActionMode();
  };

  const selectFirstVisibleRequest = async () => {
    const currentId = activeRequest.value?.id;

    if (currentId && visibleRequests.value.some((request) => request.id === currentId)) {
      return;
    }

    const firstRequest = visibleRequests.value[0];
    if (firstRequest) {
      await openRequest(firstRequest.id);
      return;
    }

    clearActiveRequest();
  };

  const loadRequests = async (options: { selectFirst?: boolean } = {}) => {
    if (!authStore.isAuthenticated) {
      return;
    }

    isLoadingList.value = true;
    error.value = null;

    try {
      listResponse.value = await mentoringApi.getList({
        page: 0,
        size: 100
      });

      if (options.selectFirst ?? true) {
        await selectFirstVisibleRequest();
      }
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests");
    } finally {
      isLoadingList.value = false;
    }
  };

  const runRequestAction = async (
    path: string,
    actionLabel: string,
    action: () => Promise<MentoringRequestResponse>
  ) => {
    if (!activeRequest.value) {
      return;
    }

    const previousRequestId = activeRequest.value.id;
    const previousVisibleIds = visibleRequests.value.map((request) => request.id);

    isSubmitting.value = true;
    error.value = null;
    successMessage.value = "";
    fieldErrors.value = {};

    try {
      const updatedRequest = await action();
      await loadRequests({ selectFirst: false });

      const nextVisibleIds = visibleRequests.value.map((request) => request.id);
      const nextRequestId = findNextRequestId(previousVisibleIds, previousRequestId, nextVisibleIds);

      if (nextRequestId) {
        await openRequest(nextRequestId);
      } else {
        clearActiveRequest();
      }

      const nextStatus = getRequestStatusMeta(updatedRequest.status).label.toLowerCase();
      setSuccess(`${actionLabel}: заявка #${updatedRequest.id} теперь ${nextStatus}.`);
      notes.clarificationNote = "";
      notes.reason = "";
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, path);
    } finally {
      isSubmitting.value = false;
    }
  };

  const acceptRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/accept`,
      "Принято",
      () => mentoringApi.accept(activeRequest.value!.id)
    );
  };

  const completeRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/complete`,
      "Завершено",
      () => mentoringApi.complete(activeRequest.value!.id)
    );
  };

  const submitClarification = () => {
    if (!activeRequest.value) return;

    if (!notes.clarificationNote.trim()) {
      fieldErrors.value = {
        clarificationNote: "Напишите вопрос или список деталей, которые нужно уточнить."
      };
      return;
    }

    const payload: MentoringRequestClarifyRequest = {
      clarificationNote: notes.clarificationNote.trim()
    };

    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/needs-clarification`,
      "Запрошено уточнение",
      () => mentoringApi.requestClarification(activeRequest.value!.id, payload)
    );
  };

  const submitRejection = () => {
    if (!activeRequest.value) return;

    if (!notes.reason.trim()) {
      fieldErrors.value = {
        reason: "Укажите причину отказа, чтобы студент понял решение."
      };
      return;
    }

    const payload: MentoringRequestRejectRequest = {
      reason: notes.reason.trim()
    };

    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/reject`,
      "Отклонено",
      () => mentoringApi.reject(activeRequest.value!.id, payload)
    );
  };

  const markViewed = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/view`,
      "Взято в разбор",
      () => mentoringApi.markViewed(activeRequest.value!.id)
    );
  };

  const cancelRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/cancel`,
      "Отозвано",
      () => mentoringApi.cancel(activeRequest.value!.id)
    );
  };

  return {
    acceptRequest,
    actionMode,
    activeRequest,
    cancelRequest,
    clearActiveRequest,
    completeRequest,
    error,
    fieldErrors,
    filters,
    isBusy,
    isLoadingDetail,
    isLoadingList,
    isSubmitting,
    listResponse,
    loadRequests,
    markViewed,
    notes,
    openRequest,
    requests,
    resetActionMode,
    selectFirstVisibleRequest,
    setActionMode,
    submitClarification,
    submitRejection,
    successMessage,
    visibleRequests
  };
});
