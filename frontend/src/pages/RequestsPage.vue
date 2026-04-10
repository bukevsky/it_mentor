<script setup lang="ts">
import { watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseSelect } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useRequestsStore } from "@/features/mentoring/model/requests-store";
import { mentoringRequestStatusOptions, mentoringTypeOptions } from "@/shared/lib/options";
import { formatDateTime, fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const requestsStore = useRequestsStore();

const { isAuthenticated } = storeToRefs(authStore);
const { activeRequest, error, filters, isBusy, listResponse, notes, successMessage } = storeToRefs(requestsStore);

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void requestsStore.loadRequests();
  },
  { immediate: true }
);
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для работы с заявками нужно войти в систему.
    </div>

    <template v-else>
      <div class="split-grid">
        <article class="app-panel">
          <p class="section-kicker">Очередь</p>
          <h3 class="section-title">Список заявок</h3>
          <p class="section-copy">
            Отслеживайте статус, открывайте детали и выполняйте нужные переходы по заявке.
          </p>

          <div class="form-grid mt-6">
            <BaseSelect
              v-model="filters.status"
              title="Статус"
              placeholder="Все статусы"
              :options="mentoringRequestStatusOptions"
            />

            <div class="base-actions">
              <BaseButton
                size="l"
                :label="isBusy ? 'Обновление...' : 'Обновить список'"
                :loading="isBusy"
                @click="requestsStore.loadRequests"
              />
            </div>
          </div>

          <div v-if="listResponse?.content.length" class="detail-stack mt-6">
            <article
              v-for="request in listResponse.content"
              :key="request.id"
              class="request-card"
            >
              <div>
                <h4 class="request-card__title">Заявка #{{ request.id }}</h4>
                <p class="request-card__copy">
                  {{ fullName(request.studentProfile) }} → {{ fullName(request.mentorProfile) }}
                </p>
              </div>

              <ul class="clean-list request-meta">
                <li>
                  <span>Статус</span>
                  <strong>{{ getOptionLabel(mentoringRequestStatusOptions, request.status) }}</strong>
                </li>
                <li>
                  <span>Цель</span>
                  <strong>{{ getOptionLabel(mentoringTypeOptions, request.goalType) }}</strong>
                </li>
                <li>
                  <span>Создана</span>
                  <strong>{{ formatDateTime(request.createdAt) }}</strong>
                </li>
              </ul>

              <BaseButton
                variant="secondary"
                size="m"
                label="Открыть"
                @click="requestsStore.openRequest(request.id)"
              />
            </article>
          </div>

          <div v-else class="empty-state mt-6">
            Список заявок пуст.
          </div>
        </article>

        <article class="app-panel">
          <p class="section-kicker">Детали</p>
          <h3 class="section-title">Выбранная заявка</h3>

          <div v-if="activeRequest" class="detail-stack mt-4">
            <ul class="clean-list request-meta">
              <li><span>ID</span><strong>#{{ activeRequest.id }}</strong></li>
              <li><span>Статус</span><strong>{{ getOptionLabel(mentoringRequestStatusOptions, activeRequest.status) }}</strong></li>
              <li><span>Цель</span><strong>{{ getOptionLabel(mentoringTypeOptions, activeRequest.goalType) }}</strong></li>
              <li><span>Создана</span><strong>{{ formatDateTime(activeRequest.createdAt) }}</strong></li>
            </ul>

            <div class="empty-state">{{ activeRequest.message }}</div>

            <label class="field">
              <span>Уточнение</span>
              <textarea v-model="notes.clarificationNote" />
            </label>
            <label class="field">
              <span>Причина отклонения</span>
              <textarea v-model="notes.reason" />
            </label>

            <div class="request-actions">
              <BaseButton variant="clear" size="m" label="Просмотрено" :disabled="isBusy" @click="requestsStore.markViewed" />
              <BaseButton size="m" label="Принять" :disabled="isBusy" @click="requestsStore.acceptRequest" />
              <BaseButton variant="secondary" size="m" label="Уточнить" :disabled="isBusy" @click="requestsStore.clarifyRequest" />
              <BaseButton variant="secondary" size="m" label="Завершить" :disabled="isBusy" @click="requestsStore.completeRequest" />
              <BaseButton variant="clear" size="m" label="Отменить" :disabled="isBusy" @click="requestsStore.cancelRequest" />
              <BaseButton variant="secondary" size="m" label="Отклонить" :disabled="isBusy" @click="requestsStore.rejectRequest" />
            </div>
          </div>

          <div v-else class="empty-state mt-4">
            Выберите заявку в списке.
          </div>

          <div v-if="error" class="error-state mt-6">
            {{ error.message }}
          </div>
          <div v-if="successMessage" class="success-state mt-6">
            {{ successMessage }}
          </div>
        </article>
      </div>
    </template>
  </section>
</template>
