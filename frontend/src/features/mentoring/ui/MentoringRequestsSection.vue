<script setup lang="ts">
import { reactive, ref } from "vue";
import type {
  ErrorResponse,
  MentoringRequestClarifyRequest,
  MentoringRequestCreateRequest,
  MentoringRequestRejectRequest,
  MentoringRequestResponse,
  PagedResponse
} from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { formatJson, parseJsonInput, parseOptionalNumber } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { mentoringApi } from "../api/mentoring-api";

const createDraft = ref(
  formatJson<MentoringRequestCreateRequest>({
    targetProfileId: 10,
    goalType: "PRACTICE",
    message: "Хочу пройти практику по Java/Spring. Готов уделять 20 часов в неделю."
  })
);
const clarifyDraft = ref(
  formatJson<MentoringRequestClarifyRequest>({
    clarificationNote: "Уточните, пожалуйста, ваш текущий уровень знания Java."
  })
);
const rejectDraft = ref(
  formatJson<MentoringRequestRejectRequest>({
    reason: "Ваш уровень пока не соответствует требованиям программы."
  })
);

const requestId = ref("");
const listFilters = reactive({
  status: "",
  page: "0",
  size: "20"
});

const lastResponse = ref<MentoringRequestResponse>();
const listResponse = ref<PagedResponse<MentoringRequestResponse>>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);

const execute = async <T>(action: () => Promise<T>, path: string, target: { value: T | undefined }) => {
  isLoading.value = true;
  error.value = null;

  try {
    target.value = await action();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, path);
  } finally {
    isLoading.value = false;
  }
};

const getRequiredId = (path: string) => {
  const id = parseOptionalNumber(requestId.value);

  if (id === undefined) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: "Укажите корректный ID заявки",
      path
    };
    return null;
  }

  return id;
};

const createRequest = async () => {
  await execute(
    () => mentoringApi.create(parseJsonInput<MentoringRequestCreateRequest>(createDraft.value)),
    "/mentoring/requests",
    lastResponse
  );
};

const loadList = async () => {
    await execute(
      () =>
        mentoringApi.getList({
          status: (listFilters.status || undefined) as never,
          page: parseOptionalNumber(listFilters.page) ?? 0,
          size: parseOptionalNumber(listFilters.size) ?? 20
        }),
      "/mentoring/requests",
      listResponse
    );
};

const loadById = async () => {
  const id = getRequiredId("/mentoring/requests/{id}");
  if (id === null) return;
  await execute(() => mentoringApi.getById(id), `/mentoring/requests/${id}`, lastResponse);
};

const markViewed = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/view");
  if (id === null) return;
  await execute(() => mentoringApi.markViewed(id), `/mentoring/requests/${id}/view`, lastResponse);
};

const clarify = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/needs-clarification");
  if (id === null) return;
  await execute(
    () => mentoringApi.requestClarification(id, parseJsonInput<MentoringRequestClarifyRequest>(clarifyDraft.value)),
    `/mentoring/requests/${id}/needs-clarification`,
    lastResponse
  );
};

const accept = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/accept");
  if (id === null) return;
  await execute(() => mentoringApi.accept(id), `/mentoring/requests/${id}/accept`, lastResponse);
};

const reject = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/reject");
  if (id === null) return;
  await execute(
    () => mentoringApi.reject(id, parseJsonInput<MentoringRequestRejectRequest>(rejectDraft.value)),
    `/mentoring/requests/${id}/reject`,
    lastResponse
  );
};

const cancel = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/cancel");
  if (id === null) return;
  await execute(() => mentoringApi.cancel(id), `/mentoring/requests/${id}/cancel`, lastResponse);
};

const complete = async () => {
  const id = getRequiredId("/mentoring/requests/{id}/complete");
  if (id === null) return;
  await execute(() => mentoringApi.complete(id), `/mentoring/requests/${id}/complete`, lastResponse);
};
</script>

<template>
  <SectionCard
    description="Полный lifecycle mentoring requests: create, list, get by id и все state transitions из guide."
    kicker="Mentoring Requests"
    title="Заявки на менторство"
  >
    <div class="section-grid section-grid--wide">
      <div class="stack">
        <label class="field">
          <span>MentoringRequestCreateRequest</span>
          <textarea v-model="createDraft" rows="8" />
        </label>
        <button class="primary-button" :disabled="isLoading" type="button" @click="createRequest">
          POST /mentoring/requests
        </button>

        <div class="form-grid form-grid--columns">
          <label class="field">
            <span>status</span>
            <input v-model.trim="listFilters.status" placeholder="SENT" type="text" />
          </label>
          <label class="field">
            <span>page</span>
            <input v-model.trim="listFilters.page" type="text" />
          </label>
          <label class="field">
            <span>size</span>
            <input v-model.trim="listFilters.size" type="text" />
          </label>
        </div>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="loadList">
          GET /mentoring/requests
        </button>

        <label class="field">
          <span>ID заявки</span>
          <input v-model="requestId" placeholder="1" type="text" />
        </label>

        <div class="actions">
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadById">
            GET /{id}
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="markViewed">
            PUT /view
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="accept">
            PUT /accept
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="cancel">
            PUT /cancel
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="complete">
            PUT /complete
          </button>
        </div>
      </div>

      <div class="stack">
        <label class="field">
          <span>Clarify payload</span>
          <textarea v-model="clarifyDraft" rows="6" />
        </label>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="clarify">
          PUT /needs-clarification
        </button>

        <label class="field">
          <span>Reject payload</span>
          <textarea v-model="rejectDraft" rows="6" />
        </label>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="reject">
          PUT /reject
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>
    </div>

    <div class="section-grid">
      <JsonPreview :value="lastResponse" title="MentoringRequestResponse" />
      <JsonPreview :value="listResponse" title="PagedResponse<MentoringRequestResponse>" />
    </div>
  </SectionCard>
</template>
