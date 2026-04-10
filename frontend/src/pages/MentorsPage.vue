<script setup lang="ts">
import { computed, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, BaseSelect, Status, VCol, VRow } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useMentorDirectoryStore } from "@/features/mentor-profile/model/mentor-directory-store";
import {
  mentoringChannelOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions
} from "@/shared/lib/options";
import { fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const directoryStore = useMentorDirectoryStore();

const { isAuthenticated } = storeToRefs(authStore);
const { cities, skills } = storeToRefs(dictionariesStore);
const {
  error,
  isLoading,
  mentorsCount,
  requestForm,
  results,
  searchForm,
  selectedMentor,
  successMessage
} = storeToRefs(directoryStore);

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void directoryStore.searchMentors();
  },
  { immediate: true }
);

const cityOptions = computed(() => cities.value.map((city) => ({
  value: String(city.id),
  label: `${city.name}, ${city.region}`
})));

const skillOptions = computed(() => skills.value.map((skill) => ({
  value: String(skill.id),
  label: skill.name
})));
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для доступа к каталогу менторов нужно войти в систему.
    </div>

    <template v-else>
      <div class="split-grid">
        <article class="app-panel">
          <p class="section-kicker">Каталог</p>
          <h3 class="section-title">Поиск менторов</h3>
          <p class="section-copy">
            Настройте фильтры и выберите профиль, который подходит по навыкам, городу и формату взаимодействия.
          </p>

          <div class="form-grid mt-6">
            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput
                v-model="searchForm.q"
                title="Поиск"
                placeholder="Java, Spring, аналитика"
              />
              <BaseSelect
                v-model="searchForm.cityId"
                title="Город"
                placeholder="Любой город"
                :options="cityOptions"
              />
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="searchForm.skillId"
                title="Навык"
                placeholder="Любой навык"
                :options="skillOptions"
              />
              <BaseSelect
                v-model="searchForm.recruitmentStatus"
                title="Статус набора"
                placeholder="Любой статус"
                :options="recruitmentStatusOptions"
              />
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="searchForm.mentoringType"
                title="Тип"
                placeholder="Любой тип"
                :options="mentoringTypeOptions"
              />
              <BaseSelect
                v-model="searchForm.mentoringChannel"
                title="Канал"
                placeholder="Любой канал"
                :options="mentoringChannelOptions"
              />
            </div>

            <div class="base-actions base-actions--between">
              <BaseButton
                size="l"
                :label="isLoading ? 'Поиск...' : 'Применить фильтры'"
                :loading="isLoading"
                @click="directoryStore.searchMentors"
              />
              <Status type="secondary" size="s" :label="`Найдено менторов: ${mentorsCount}`" />
            </div>
          </div>
        </article>

        <article class="app-panel">
          <p class="section-kicker">Заявка</p>
          <h3 class="section-title">Отправить запрос ментору</h3>

          <div v-if="selectedMentor" class="detail-stack mt-4">
            <div class="summary-row">
              <span>Ментор</span>
              <strong>{{ fullName(selectedMentor) }}</strong>
            </div>
            <div class="summary-row">
              <span>Позиция</span>
              <strong>{{ selectedMentor.position ?? "—" }}</strong>
            </div>
            <div class="summary-row">
              <span>Формат</span>
              <strong>
                {{ getOptionLabel(mentoringTypeOptions, selectedMentor.mentoringType) }}
                /
                {{ getOptionLabel(mentoringChannelOptions, selectedMentor.mentoringChannel) }}
              </strong>
            </div>
          </div>

          <div v-else class="empty-state mt-4">
            Выберите карточку ментора, чтобы отправить заявку.
          </div>

          <div class="form-grid mt-6">
            <BaseSelect
              v-model="requestForm.goalType"
              title="Цель"
              :options="mentoringTypeOptions"
            />

            <label class="field">
              <span>Сообщение</span>
              <textarea v-model="requestForm.message" />
            </label>

            <BaseButton
              size="l"
              label="Отправить заявку"
              :disabled="isLoading || !selectedMentor"
              :loading="isLoading"
              @click="directoryStore.submitRequest"
            />
          </div>

          <div v-if="error" class="error-state mt-6">
            {{ error.message }}
          </div>
          <div v-if="successMessage" class="success-state mt-6">
            {{ successMessage }}
          </div>
        </article>
      </div>

      <VRow v-if="results?.content.length" :gutter="2">
        <VCol
          v-for="mentor in results.content"
          :key="mentor.id"
          :cols="1"
          :laptop="2"
          :hd="3"
        >
          <article class="mentor-card">
            <div class="detail-stack">
              <h4 class="mentor-card__title">{{ fullName(mentor) }}</h4>
              <p class="mentor-card__copy">
                {{ mentor.position ?? "Позиция не указана" }}
                <span v-if="mentor.department">· {{ mentor.department }}</span>
              </p>
            </div>

            <ul class="clean-list mentor-meta">
              <li><span>Город</span><strong>{{ mentor.city?.name ?? "—" }}</strong></li>
              <li><span>Тип</span><strong>{{ getOptionLabel(mentoringTypeOptions, mentor.mentoringType) }}</strong></li>
              <li><span>Канал</span><strong>{{ getOptionLabel(mentoringChannelOptions, mentor.mentoringChannel) }}</strong></li>
              <li><span>Набор</span><strong>{{ getOptionLabel(recruitmentStatusOptions, mentor.recruitmentStatus) }}</strong></li>
            </ul>

            <div class="button-row">
              <Status
                v-for="skill in mentor.skills.slice(0, 3)"
                :key="skill.id"
                type="secondary"
                size="s"
                :label="skill.skill.name"
              />
            </div>

            <BaseButton
              variant="secondary"
              size="m"
              label="Выбрать"
              @click="directoryStore.selectMentor(mentor)"
            />
          </article>
        </VCol>
      </VRow>

      <div v-else class="empty-state">
        По текущим фильтрам результатов нет.
      </div>
    </template>
  </section>
</template>
