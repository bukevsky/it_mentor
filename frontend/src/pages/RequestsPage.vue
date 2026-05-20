<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRoute } from "vue-router";
import { BaseButton, BaseInput, BaseSelect } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useRequestsStore } from "@/features/mentoring/model/requests-store";
import { reviewsApi } from "@/features/reviews/api/reviews-api";
import ReviewRating from "@/features/reviews/ui/ReviewRating.vue";
import { ApiError } from "@/shared/api/http";
import type { MentoringRequestResponse, ReviewResponse } from "@/shared/api/contracts";
import {
  getAvailableRequestActions,
  getRequestCounters,
  getRequestScope,
  getRequestStatusClass,
  getRequestStatusMeta,
  requestScopeOptions,
  requestSortOptions,
  requestStatusFilterOptions
} from "@/features/mentoring/model/request-triage";
import { mentoringTypeOptions } from "@/shared/lib/options";
import { formatDateTime, fullName, getOptionLabel } from "@/shared/lib/presenters";

const route = useRoute();
const authStore = useAuthStore();
const requestsStore = useRequestsStore();
const searchQuery = ref("");
const activeReview = ref<ReviewResponse | null>(null);
const isReviewLoading = ref(false);
const isReviewSubmitting = ref(false);
const reviewRating = ref(0);
const reviewComment = ref("");
const reviewError = ref("");
const reviewSuccess = ref("");

const { isAuthenticated, user } = storeToRefs(authStore);
const {
  actionMode,
  activeRequest,
  error,
  fieldErrors,
  isLoadingDetail,
  isLoadingList,
  isSubmitting,
  requests,
  successMessage,
  visibleRequests
} = storeToRefs(requestsStore);

const counters = computed(() => getRequestCounters(requests.value, user.value?.roles ?? []));
const activeStatusMeta = computed(() => getRequestStatusMeta(activeRequest.value?.status));
const availableActions = computed(() =>
  getAvailableRequestActions(activeRequest.value, user.value?.roles ?? [])
);

const canAccept = computed(() => availableActions.value.includes("accept"));
const canClarify = computed(() => availableActions.value.includes("clarify"));
const canReject = computed(() => availableActions.value.includes("reject"));
const canComplete = computed(() => availableActions.value.includes("complete"));
const canCancel = computed(() => availableActions.value.includes("cancel"));
const hasActions = computed(() => availableActions.value.length > 0);
const isStudent = computed(() => user.value?.roles.includes("STUDENT") ?? false);
const isInitialLoading = computed(() => isLoadingList.value && !requests.value.length);
const isDetailLoading = computed(() => isLoadingDetail.value && !activeRequest.value);
const activeGoalLabel = computed(() =>
  getOptionLabel(mentoringTypeOptions, activeRequest.value?.goalType)
);

const getRequestSender = (request: MentoringRequestResponse) => {
  return request.direction === "MENTOR_TO_STUDENT"
    ? request.mentorProfile
    : request.studentProfile;
};

const getRequestRecipient = (request: MentoringRequestResponse) => {
  return request.direction === "MENTOR_TO_STUDENT"
    ? request.studentProfile
    : request.mentorProfile;
};

const getRequestSenderName = (request: MentoringRequestResponse) => fullName(getRequestSender(request));
const getRequestRecipientName = (request: MentoringRequestResponse) => fullName(getRequestRecipient(request));
const getRequestPeerName = (request: MentoringRequestResponse) =>
  getRequestScope(request, user.value?.roles ?? []) === "outgoing"
    ? getRequestRecipientName(request)
    : getRequestSenderName(request);
const getRequestScopeLabel = (request: MentoringRequestResponse) =>
  getRequestScope(request, user.value?.roles ?? []) === "outgoing" ? "Исходящая" : "Входящая";
const previewText = (value: string) => value.trim() || "Сообщение не заполнено";
const canShowReviewBlock = computed(() => activeRequest.value?.status === "COMPLETED");
const canCreateReviewForActiveRequest = computed(() =>
  canShowReviewBlock.value && isStudent.value && !activeReview.value
);

const resetReviewState = () => {
  activeReview.value = null;
  reviewRating.value = 0;
  reviewComment.value = "";
  reviewError.value = "";
  reviewSuccess.value = "";
};

const loadActiveReview = async () => {
  const request = activeRequest.value;

  resetReviewState();

  if (!request || request.status !== "COMPLETED") {
    return;
  }

  isReviewLoading.value = true;

  try {
    activeReview.value = await reviewsApi.getByRequest(request.id);
  } catch (rawError) {
    if (rawError instanceof ApiError && rawError.status === 404) {
      return;
    }

    reviewError.value = rawError instanceof Error ? rawError.message : "Не удалось загрузить отзыв.";
  } finally {
    isReviewLoading.value = false;
  }
};

const submitReview = async () => {
  const request = activeRequest.value;

  if (!request || !canCreateReviewForActiveRequest.value) {
    return;
  }

  if (!reviewRating.value) {
    reviewError.value = "Поставьте оценку от 1 до 5.";
    return;
  }

  isReviewSubmitting.value = true;
  reviewError.value = "";
  reviewSuccess.value = "";

  try {
    activeReview.value = await reviewsApi.createReview({
      mentoringRequestId: request.id,
      rating: reviewRating.value,
      comment: reviewComment.value.trim() || null
    });
    reviewComment.value = "";
    reviewSuccess.value = "Отзыв сохранён.";
  } catch (rawError) {
    reviewError.value = rawError instanceof Error ? rawError.message : "Не удалось сохранить отзыв.";
  } finally {
    isReviewSubmitting.value = false;
  }
};

const searchedVisibleRequests = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();

  if (!query) {
    return visibleRequests.value;
  }

  return visibleRequests.value.filter((request) => {
    const haystack = [
      getRequestSenderName(request),
      getRequestRecipientName(request),
      request.message,
      getOptionLabel(mentoringTypeOptions, request.goalType),
      getRequestStatusMeta(request.status).label
    ].join(" ").toLowerCase();

    return haystack.includes(query);
  });
});

watch(
  () => user.value?.id ?? null,
  (nextUserId) => {
    searchQuery.value = "";
    requestsStore.resetFilters();

    if (!nextUserId) {
      requestsStore.clearActiveRequest();
      return;
    }

    void requestsStore.loadRequests().then(() => {
      const qId = route.query.requestId;
      if (qId && typeof qId === "string") {
        void requestsStore.openRequest(Number(qId));
      }
    });
  },
  { immediate: true }
);

watch(
  () => [requestsStore.filters.status, requestsStore.filters.scope, requestsStore.filters.sortOrder],
  () => {
    void requestsStore.selectFirstVisibleRequest();
  }
);

watch(
  () => [activeRequest.value?.id, activeRequest.value?.status] as const,
  () => {
    void loadActiveReview();
  },
  { immediate: true }
);
</script>

<template>
  <section class="app-section requests-page">
    <div v-if="!isAuthenticated" class="empty-state">
      Для работы с заявками нужно войти в систему.
    </div>

    <template v-else>
      <div v-if="successMessage" class="request-toast success-state">
        {{ successMessage }}
      </div>

      <header class="requests-hero">
        <div class="requests-hero__text">
          <h1 class="workspace-title">Заявки</h1>
          <p class="workspace-subtitle">
            Управляйте входящими запросами и отслеживайте отправленные приглашения.
          </p>
        </div>
      </header>

      <div class="request-workspace">
        <aside class="app-panel request-list-panel">
          <div class="request-list-panel__header">
            <div class="request-list-panel__toolbar">
              <BaseButton
                class="mobile-refresh"
                variant="secondary"
                size="m"
                :label="isLoadingList ? 'Обновление...' : 'Обновить'"
                :loading="isLoadingList"
                :disabled="isLoadingList || isSubmitting"
                @click="requestsStore.loadRequests"
              />
            </div>

            <div class="request-counters" aria-label="Счетчики заявок">
              <button
                type="button"
                class="request-counter"
                :class="{ 'request-counter--active': requestsStore.filters.scope === 'all' }"
                @click="requestsStore.filters.scope = 'all'"
              >
                <strong>{{ counters.total }}</strong>
                всего
              </button>
              <button
                type="button"
                class="request-counter request-counter--info"
                :class="{ 'request-counter--active': requestsStore.filters.scope === 'incoming' }"
                @click="requestsStore.filters.scope = 'incoming'"
              >
                <strong>{{ counters.incoming }}</strong>
                входящие
              </button>
              <button
                type="button"
                class="request-counter request-counter--warning"
                :class="{ 'request-counter--active': requestsStore.filters.scope === 'outgoing' }"
                @click="requestsStore.filters.scope = 'outgoing'"
              >
                <strong>{{ counters.outgoing }}</strong>
                исходящие
              </button>
              <span class="request-counter request-counter--success">
                <strong>{{ counters.accepted }}</strong>
                принятые
              </span>
            </div>

            <BaseInput
              v-model="searchQuery"
              title=""
              start-icon="search"
              placeholder="Поиск по имени, цели или сообщению..."
            />

            <div class="request-filter-grid">
              <BaseSelect
                v-model="requestsStore.filters.scope"
                title="Тип"
                :options="requestScopeOptions"
              />
              <BaseSelect
                v-model="requestsStore.filters.status"
                title="Статус"
                placeholder="Все статусы"
                :options="requestStatusFilterOptions"
              />
              <BaseSelect
                v-model="requestsStore.filters.sortOrder"
                title="Сортировка"
                :options="requestSortOptions"
              />
            </div>
          </div>

          <div v-if="isInitialLoading" class="empty-state request-panel-state">
            Загружаем заявки...
          </div>

          <div v-else-if="error && !requests.length" class="error-state request-panel-state">
            {{ error.message }}
          </div>

          <div v-else-if="!searchedVisibleRequests.length" class="empty-state request-panel-state">
            По выбранным условиям заявок нет.
          </div>

          <div v-else class="compact-list request-list">
            <button
              v-for="request in searchedVisibleRequests"
              :key="request.id"
              type="button"
              :class="[
                'request-list-item',
                activeRequest?.id === request.id ? 'request-list-item--active' : ''
              ]"
              @click="requestsStore.openRequest(request.id)"
            >
              <div class="request-list-item__top">
                <div class="request-list-item__name-block">
                  <span>{{ getRequestScopeLabel(request) }}</span>
                  <strong class="request-list-item__name">{{ getRequestPeerName(request) }}</strong>
                </div>
                <span
                  class="status-pill"
                  :class="getRequestStatusClass(request.status)"
                >
                  {{ getRequestStatusMeta(request.status).label }}
                </span>
              </div>
              <div class="request-list-item__meta">
                <span>{{ getOptionLabel(mentoringTypeOptions, request.goalType) }}</span>
                <span>{{ formatDateTime(request.createdAt) }}</span>
              </div>
              <p class="request-list-item__preview">
                {{ previewText(request.message) }}
              </p>
            </button>
          </div>
        </aside>

        <article
          class="app-panel request-detail-panel"
          :class="{ 'request-detail-panel--open': !!activeRequest }"
        >
          <button
            v-if="activeRequest"
            type="button"
            class="request-detail-close"
            @click="requestsStore.clearActiveRequest()"
          >
            ← Назад к заявкам
          </button>

          <div v-if="isDetailLoading" class="empty-state request-detail-state">
            Загружаем детали заявки...
          </div>

          <div v-else-if="!activeRequest" class="empty-state request-detail-state">
            <h3 class="section-title">В очереди нет выбранной заявки</h3>
            <p class="section-copy">
              После обработки текущей заявки здесь автоматически появится следующая.
              Если очередь пуста, измените фильтр или обновите список.
            </p>
            <div class="base-actions mt-4">
              <BaseButton
                variant="secondary"
                size="m"
                label="Показать все статусы"
                :disabled="isSubmitting"
                @click="requestsStore.filters.status = ''"
              />
              <BaseButton
                class="mobile-refresh"
                size="m"
                label="Обновить"
                :loading="isLoadingList"
                :disabled="isLoadingList || isSubmitting"
                @click="requestsStore.loadRequests"
              />
            </div>
          </div>

          <template v-else>
            <div class="request-detail-panel__header">
              <div class="request-detail-panel__person">
                <div class="avatar request-detail-panel__avatar">
                  {{ getRequestPeerName(activeRequest).slice(0, 1) }}
                </div>
                <div>
                  <p class="section-kicker">{{ getRequestScopeLabel(activeRequest) }} заявка #{{ activeRequest.id }}</p>
                  <h3 class="section-title">{{ getRequestPeerName(activeRequest) }}</h3>
                  <p class="helper-text">{{ activeGoalLabel }} · {{ formatDateTime(activeRequest.createdAt) }}</p>
                </div>
              </div>
              <span
                class="status-pill"
                :class="getRequestStatusClass(activeRequest.status)"
              >
                {{ activeStatusMeta.label }}
              </span>
            </div>

            <div class="request-detail-panel__body">
              <section class="request-summary-card">
                <div class="request-summary-card__item">
                  <span>От кого</span>
                  <strong>{{ getRequestSenderName(activeRequest) }}</strong>
                </div>
                <div class="request-summary-card__item">
                  <span>Кому</span>
                  <strong>{{ getRequestRecipientName(activeRequest) }}</strong>
                </div>
                <div class="request-summary-card__item">
                  <span>Цель</span>
                  <strong>{{ activeGoalLabel }}</strong>
                </div>
                <div class="request-summary-card__item">
                  <span>Тип</span>
                  <strong>{{ getRequestScopeLabel(activeRequest) }}</strong>
                </div>
                <div class="request-summary-card__item">
                  <span>Создана</span>
                  <strong>{{ formatDateTime(activeRequest.createdAt) }}</strong>
                </div>
                <div class="request-summary-card__item request-summary-card__item--wide">
                  <span>Жизненный цикл</span>
                  <strong>{{ activeStatusMeta.lifecycle }}</strong>
                </div>
              </section>

              <section class="request-info-block">
                <p class="section-kicker">Цель менторства</p>
                <div class="detail-box">
                  {{ activeGoalLabel }}
                </div>
              </section>

              <section class="request-info-block">
                <p class="section-kicker">Сообщение</p>
                <div class="request-message-box">
                  {{ previewText(activeRequest.message) }}
                </div>
              </section>

              <section
                v-if="activeRequest.clarificationNote"
                class="request-info-block"
              >
                <p class="section-kicker">Запрошенное уточнение</p>
                <div class="detail-box">
                  {{ activeRequest.clarificationNote }}
                </div>
              </section>

              <section
                v-if="activeRequest.reason"
                class="request-info-block"
              >
                <p class="section-kicker">Причина отклонения</p>
                <div class="detail-box detail-box--danger">
                  {{ activeRequest.reason }}
                </div>
              </section>

              <section v-if="canShowReviewBlock" class="request-info-block request-review-block">
                <div class="request-review-block__header">
                  <div>
                    <p class="section-kicker">Отзыв о менторе</p>
                    <h3 class="section-title">Оценка по завершённой заявке</h3>
                  </div>
                  <span v-if="reviewSuccess" class="status-pill status-pill--success">{{ reviewSuccess }}</span>
                </div>

                <div v-if="isReviewLoading" class="panel-state">Загружаем отзыв...</div>

                <div v-else-if="activeReview" class="request-review-card">
                  <ReviewRating :value="activeReview.rating" readonly />
                  <p>{{ activeReview.comment || "Комментарий не заполнен." }}</p>
                  <span class="helper-text">
                    Оставлен {{ formatDateTime(activeReview.createdAt) }}
                  </span>
                </div>

                <form
                  v-else-if="canCreateReviewForActiveRequest"
                  class="request-review-form"
                  @submit.prevent="submitReview"
                >
                  <div class="review-form__rating">
                    <span>Оценка</span>
                    <ReviewRating v-model="reviewRating" :show-number="false" />
                  </div>
                  <label class="field request-action-field">
                    <span>Комментарий</span>
                    <textarea
                      v-model="reviewComment"
                      rows="4"
                      placeholder="Что было полезно? Что можно улучшить?"
                    />
                  </label>
                  <BaseButton
                    size="m"
                    type="submit"
                    :loading="isReviewSubmitting"
                    :disabled="isReviewSubmitting"
                    label="Сохранить отзыв"
                  />
                </form>

                <div v-else class="empty-state empty-state--compact">
                  <p>Студент ещё не оставил отзыв по этой заявке.</p>
                </div>

                <p v-if="reviewError" class="form-error">{{ reviewError }}</p>
              </section>

              <label
                v-if="actionMode === 'clarify'"
                class="field request-action-field"
              >
                <span>Что нужно уточнить</span>
                <textarea
                  v-model="requestsStore.notes.clarificationNote"
                  rows="4"
                  placeholder="Например: уточните сроки, ожидаемый формат встреч или уровень подготовки."
                />
                <small v-if="fieldErrors.clarificationNote" class="helper-text request-field-error">
                  {{ fieldErrors.clarificationNote }}
                </small>
              </label>

              <label
                v-if="actionMode === 'reject'"
                class="field request-action-field"
              >
                <span>Причина отказа</span>
                <textarea
                  v-model="requestsStore.notes.reason"
                  rows="4"
                  placeholder="Кратко объясните, почему заявку нельзя взять в работу."
                />
                <small v-if="fieldErrors.reason" class="helper-text request-field-error">
                  {{ fieldErrors.reason }}
                </small>
              </label>

              <div v-if="error" class="error-state">
                {{ error.message }}
              </div>
            </div>

            <div class="request-action-bar">
              <div class="request-action-bar__hint">
                <strong>{{ activeStatusMeta.label }}</strong>
                <span v-if="actionMode === 'idle'">Выберите следующее действие по заявке.</span>
                <span v-else>Заполните поле и подтвердите действие.</span>
              </div>

              <div v-if="actionMode === 'idle'" class="request-action-bar__buttons">
                <BaseButton
                  v-if="canCancel"
                  class="request-button--danger"
                  variant="secondary"
                  size="m"
                  label="Отозвать"
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="requestsStore.cancelRequest"
                />
                <BaseButton
                  v-if="canAccept"
                  size="m"
                  label="Принять"
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="requestsStore.acceptRequest"
                />
                <BaseButton
                  v-if="canClarify"
                  variant="secondary"
                  size="m"
                  label="Уточнить"
                  :disabled="isSubmitting"
                  @click="requestsStore.setActionMode('clarify')"
                />
                <BaseButton
                  v-if="canComplete"
                  size="m"
                  label="Завершить"
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="requestsStore.completeRequest"
                />
                <BaseButton
                  v-if="canReject"
                  class="request-button--danger"
                  variant="secondary"
                  size="m"
                  label="Отклонить"
                  :disabled="isSubmitting"
                  @click="requestsStore.setActionMode('reject')"
                />
                <span v-if="!hasActions" class="helper-text">
                  Для этого статуса нет доступных действий.
                </span>
              </div>

              <div v-else class="request-action-bar__buttons">
                <BaseButton
                  variant="clear"
                  size="m"
                  label="Отменить"
                  :disabled="isSubmitting"
                  @click="requestsStore.resetActionMode"
                />
                <BaseButton
                  v-if="actionMode === 'clarify'"
                  variant="secondary"
                  size="m"
                  label="Отправить уточнение"
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="requestsStore.submitClarification"
                />
                <BaseButton
                  v-if="actionMode === 'reject'"
                  class="request-button--danger"
                  variant="secondary"
                  size="m"
                  label="Отклонить заявку"
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="requestsStore.submitRejection"
                />
              </div>
            </div>
          </template>
        </article>
      </div>
    </template>
  </section>
</template>
