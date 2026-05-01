<script setup lang="ts">
import { computed, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseSelect } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useRequestsStore } from "@/features/mentoring/model/requests-store";
import type { MentoringRequestResponse } from "@/shared/api/contracts";
import {
  getAvailableRequestActions,
  getRequestCounters,
  getRequestStatusClass,
  getRequestStatusMeta,
  requestSortOptions,
  requestStatusFilterOptions
} from "@/features/mentoring/model/request-triage";
import { mentoringTypeOptions } from "@/shared/lib/options";
import { formatDateTime, fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const requestsStore = useRequestsStore();

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

const counters = computed(() => getRequestCounters(requests.value));
const activeStatusMeta = computed(() => getRequestStatusMeta(activeRequest.value?.status));
const availableActions = computed(() =>
  getAvailableRequestActions(activeRequest.value, user.value?.roles ?? [])
);

const canAccept = computed(() => availableActions.value.includes("accept"));
const canClarify = computed(() => availableActions.value.includes("clarify"));
const canReject = computed(() => availableActions.value.includes("reject"));
const canComplete = computed(() => availableActions.value.includes("complete"));
const hasActions = computed(() => availableActions.value.length > 0);
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
const previewText = (value: string) => value.trim() || "Сообщение не заполнено";

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      requestsStore.clearActiveRequest();
      return;
    }

    void requestsStore.loadRequests();
  },
  { immediate: true }
);

watch(
  () => [requestsStore.filters.status, requestsStore.filters.sortOrder],
  () => {
    void requestsStore.selectFirstVisibleRequest();
  }
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

      <div class="request-workspace">
        <aside class="app-panel request-list-panel">
          <div class="request-list-panel__header">
            <div>
              <h3 class="section-title">Заявки</h3>
              <p class="section-copy">Очередь для быстрого разбора входящих запросов.</p>
            </div>

            <div class="request-counters" aria-label="Счетчики заявок">
              <span class="request-counter">
                <strong>{{ counters.total }}</strong>
                всего
              </span>
              <span class="request-counter request-counter--info">
                <strong>{{ counters.new }}</strong>
                новые
              </span>
              <span class="request-counter request-counter--warning">
                <strong>{{ counters.reviewing }}</strong>
                на рассмотрении
              </span>
            </div>

            <div class="request-filter-grid">
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

            <BaseButton
              variant="secondary"
              size="m"
              :label="isLoadingList ? 'Обновление...' : 'Обновить'"
              :loading="isLoadingList"
              :disabled="isLoadingList || isSubmitting"
              @click="requestsStore.loadRequests"
            />
          </div>

          <div v-if="isInitialLoading" class="empty-state request-panel-state">
            Загружаем заявки...
          </div>

          <div v-else-if="error && !requests.length" class="error-state request-panel-state">
            {{ error.message }}
          </div>

          <div v-else-if="!visibleRequests.length" class="empty-state request-panel-state">
            По выбранным условиям заявок нет.
          </div>

          <div v-else class="compact-list request-list">
            <button
              v-for="request in visibleRequests"
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
                  <span>Кому</span>
                  <strong class="request-list-item__name">{{ getRequestRecipientName(request) }}</strong>
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

        <article class="app-panel request-detail-panel">
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
              <div>
                <h3 class="section-title">Заявка #{{ activeRequest.id }}</h3>
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
