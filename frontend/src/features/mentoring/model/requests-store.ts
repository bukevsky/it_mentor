import { reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ErrorResponse,
  MentoringRequestClarifyRequest,
  MentoringRequestRejectRequest,
  MentoringRequestResponse,
  PagedResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { mentoringApi } from "../api/mentoring-api";

export const useRequestsStore = defineStore("requests", () => {
  const authStore = useAuthStore();

  const filters = reactive({
    status: ""
  });

  const notes = reactive({
    clarificationNote: "Подскажите, пожалуйста, сколько часов в неделю вы готовы уделять работе с ментором?",
    reason: "Сейчас не получается взять этот запрос в работу."
  });

  const listResponse = ref<PagedResponse<MentoringRequestResponse> | null>(null);
  const activeRequest = ref<MentoringRequestResponse | null>(null);
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isBusy = ref(false);

  const loadRequests = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    isBusy.value = true;
    error.value = null;

    try {
      listResponse.value = await mentoringApi.getList({
        status: filters.status || undefined,
        page: 0,
        size: 20
      });
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests");
    } finally {
      isBusy.value = false;
    }
  };

  const openRequest = async (requestId: number) => {
    isBusy.value = true;
    error.value = null;

    try {
      activeRequest.value = await mentoringApi.getById(requestId);
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, `/mentoring/requests/${requestId}`);
    } finally {
      isBusy.value = false;
    }
  };

  const runRequestAction = async (
    path: string,
    action: () => Promise<MentoringRequestResponse>
  ) => {
    isBusy.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      activeRequest.value = await action();
      successMessage.value = `Статус обновлён: ${activeRequest.value.status}`;
      await loadRequests();
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, path);
    } finally {
      isBusy.value = false;
    }
  };

  const markViewed = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/view`,
      () => mentoringApi.markViewed(activeRequest.value!.id)
    );
  };

  const acceptRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/accept`,
      () => mentoringApi.accept(activeRequest.value!.id)
    );
  };

  const cancelRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/cancel`,
      () => mentoringApi.cancel(activeRequest.value!.id)
    );
  };

  const completeRequest = () => {
    if (!activeRequest.value) return;
    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/complete`,
      () => mentoringApi.complete(activeRequest.value!.id)
    );
  };

  const clarifyRequest = () => {
    if (!activeRequest.value) return;

    const payload: MentoringRequestClarifyRequest = {
      clarificationNote: notes.clarificationNote
    };

    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/needs-clarification`,
      () => mentoringApi.requestClarification(activeRequest.value!.id, payload)
    );
  };

  const rejectRequest = () => {
    if (!activeRequest.value) return;

    const payload: MentoringRequestRejectRequest = {
      reason: notes.reason
    };

    void runRequestAction(
      `/mentoring/requests/${activeRequest.value.id}/reject`,
      () => mentoringApi.reject(activeRequest.value!.id, payload)
    );
  };

  return {
    acceptRequest,
    activeRequest,
    cancelRequest,
    clarifyRequest,
    completeRequest,
    error,
    filters,
    isBusy,
    listResponse,
    loadRequests,
    markViewed,
    notes,
    openRequest,
    rejectRequest,
    successMessage
  };
});
