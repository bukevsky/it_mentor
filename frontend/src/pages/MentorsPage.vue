<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, BaseSelect } from "conductor";
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
  requestForm,
  requestedMentorIds,
  results,
  searchForm,
  selectedMentor,
  successMessage
} = storeToRefs(directoryStore);

const isFilterCollapsed = ref(false);
const isRequestModalOpen = ref(false);

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

const cityOptions = computed(() => {
  const options = cities.value.map((city) => ({
    value: String(city.id),
    label: `${city.name}, ${city.region}`
  }));

  return options.length ? options : [{ value: "", label: "Список загружается" }];
});

const skillOptions = computed(() => {
  const options = skills.value.map((skill) => ({
    value: String(skill.id),
    label: skill.name
  }));

  return options.length ? options : [{ value: "", label: "Список загружается" }];
});

const mentorCards = computed(() => {
  return (results.value?.content ?? [])
    .filter((mentor) => !requestedMentorIds.value.has(mentor.id))
    .map((mentor) => ({
      id: mentor.id,
      firstName: mentor.firstName,
      lastName: mentor.lastName,
      position: mentor.position,
      department: mentor.department,
      city: mentor.city,
      recruitmentStatus: mentor.recruitmentStatus,
      mentoringType: mentor.mentoringType,
      mentoringChannel: mentor.mentoringChannel,
      skills: mentor.skills.map((skill) => skill.skill.name),
      raw: mentor
    }));
});

const hiddenMentorsCount = computed(() => {
  return (results.value?.content ?? []).filter((mentor) => requestedMentorIds.value.has(mentor.id)).length;
});

const openRequestModal = (mentor: (typeof mentorCards.value)[number]["raw"]) => {
  directoryStore.selectMentor(mentor);
  isRequestModalOpen.value = true;
};

const closeRequestModal = () => {
  isRequestModalOpen.value = false;
  directoryStore.clearSelectedMentor();
};

const submitRequest = async () => {
  const isSubmitted = await directoryStore.submitRequest();

  if (isSubmitted) {
    isRequestModalOpen.value = false;
  }
};

const resetFilters = () => {
  searchForm.q = "";
  searchForm.cityId = "";
  searchForm.skillId = "";
  searchForm.recruitmentStatus = "";
  searchForm.mentoringType = "";
  searchForm.mentoringChannel = "";

  void directoryStore.searchMentors();
};
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для доступа к менторам нужно войти в систему.
    </div>

    <template v-else>
      <div class="mentors-workspace">
        <div class="workspace-header">
          <div>
            <p class="section-kicker">Главная > Менторы</p>
            <h1 class="workspace-title">Директория менторов</h1>
          </div>
        </div>

        <article
          class="app-panel catalog-filter-panel catalog-filter-panel--horizontal"
          :class="{ 'catalog-filter-panel--collapsed': isFilterCollapsed }"
        >
          <div class="catalog-filter-panel__head">
            <div>
              <p class="section-kicker">Фильтры</p>
              <p v-if="isFilterCollapsed" class="catalog-filter-panel__collapsed-summary">
                Найдено: {{ mentorCards.length }} · скрыто выбранных: {{ hiddenMentorsCount }}
              </p>
            </div>
            <div class="catalog-filter-panel__controls">
              <BaseButton
                class="catalog-filter-panel__reset"
                variant="clear"
                size="s"
                label="Сбросить"
                @click="resetFilters"
              />
              <BaseButton
                class="catalog-filter-panel__reset"
                variant="secondary"
                size="s"
                :label="isFilterCollapsed ? 'Показать фильтр' : 'Свернуть'"
                @click="isFilterCollapsed = !isFilterCollapsed"
              />
            </div>
          </div>

          <div v-show="!isFilterCollapsed" class="form-grid catalog-filter-panel__form">
            <BaseInput
              v-model="searchForm.q"
              class="catalog-filter-panel__search"
              size="s"
              title="Поиск"
              placeholder="Java, Spring, аналитика"
            />

            <BaseSelect
              v-model="searchForm.cityId"
              class="catalog-filter-panel__select"
              size="s"
              title="Город"
              placeholder="Все города"
              :options="cityOptions"
            />
            <BaseSelect
              v-model="searchForm.skillId"
              class="catalog-filter-panel__select"
              size="s"
              title="Навык"
              placeholder="Любой навык"
              :options="skillOptions"
            />
            <BaseSelect
              v-model="searchForm.recruitmentStatus"
              class="catalog-filter-panel__select"
              size="s"
              title="Статус набора"
              placeholder="Любой статус"
              :options="recruitmentStatusOptions"
            />
            <BaseSelect
              v-model="searchForm.mentoringType"
              class="catalog-filter-panel__select"
              size="s"
              title="Цель менторства"
              placeholder="Любая цель"
              :options="mentoringTypeOptions"
            />
            <BaseSelect
              v-model="searchForm.mentoringChannel"
              class="catalog-filter-panel__select"
              size="s"
              title="Формат связи"
              placeholder="Любой формат"
              :options="mentoringChannelOptions"
            />

            <div class="base-actions">
              <BaseButton
                class="catalog-filter-panel__submit"
                size="m"
                :label="isLoading ? 'Поиск...' : 'Применить фильтры'"
                :loading="isLoading"
                @click="directoryStore.searchMentors"
              />
            </div>
            <div class="catalog-filter-panel__summary">
              <span>Найдено: {{ mentorCards.length }}</span>
              <span v-if="hiddenMentorsCount">Скрыто выбранных: {{ hiddenMentorsCount }}</span>
            </div>
          </div>
        </article>

        <div>
          <div v-if="isLoading && !results" class="empty-state mt-6">
            Загружаем менторов...
          </div>

          <div v-else-if="error" class="error-state mt-6">
            {{ error.message }}
          </div>

          <div v-else-if="mentorCards.length" class="mentor-directory-grid mt-6">
            <article
              v-for="mentor in mentorCards"
              :key="mentor.id"
              class="mentor-card"
            >
              <div class="mentor-card__top">
                <div class="avatar">{{ mentor.firstName.slice(0, 1) }}{{ mentor.lastName.slice(0, 1) }}</div>
                <span
                  :class="[
                    'status-pill',
                    mentor.recruitmentStatus === 'OPEN' ? 'status-pill--success' : 'status-pill--warning'
                  ]"
                >
                  {{ getOptionLabel(recruitmentStatusOptions, mentor.recruitmentStatus) }}
                </span>
              </div>

              <div>
                <h4 class="mentor-card__title">{{ mentor.firstName }} {{ mentor.lastName }}</h4>
                <p class="mentor-card__copy">
                  {{ mentor.position ?? "Позиция не указана" }}
                </p>
                <p class="helper-text">
                  {{ mentor.city?.name ?? "—" }} · {{ mentor.department ?? "Направление не указано" }}
                </p>
              </div>

              <div class="chip-list">
                <span v-for="skill in mentor.skills.slice(0, 4)" :key="skill" class="chip">
                  {{ skill }}
                </span>
              </div>

              <div class="mentor-card__actions mentor-card__actions--single">
                <BaseButton
                  size="m"
                  label="Выбрать для заявки"
                  :disabled="mentor.recruitmentStatus !== 'OPEN'"
                  @click="openRequestModal(mentor.raw)"
                />
              </div>
            </article>
          </div>

          <div v-else class="empty-state mt-6">
            По текущим фильтрам менторы не найдены.
          </div>
        </div>
      </div>

      <div v-if="isRequestModalOpen" class="request-modal" @click.self="closeRequestModal">
        <article class="app-panel request-draft-panel request-draft-panel--modal">
          <div class="request-draft-panel__header">
            <div>
              <p class="section-kicker">Заявка</p>
              <h3 class="section-title">Запрос ментору</h3>
            </div>
            <BaseButton variant="clear" size="s" label="Закрыть" @click="closeRequestModal" />
          </div>

          <div class="request-draft-panel__body">
            <div v-if="selectedMentor" class="request-draft-panel__selected">
              <p class="section-kicker">Кому отправится</p>
              <h4 class="request-draft-panel__mentor">{{ fullName(selectedMentor) }}</h4>
              <div class="request-draft-panel__details">
                <span>{{ selectedMentor.position ?? "Позиция не указана" }}</span>
                <span>{{ selectedMentor.city?.name ?? "Город не указан" }}</span>
                <span>
                  {{ getOptionLabel(mentoringTypeOptions, selectedMentor.mentoringType) }}
                  ·
                  {{ getOptionLabel(mentoringChannelOptions, selectedMentor.mentoringChannel) }}
                </span>
              </div>
            </div>

            <div class="request-draft-panel__form">
              <BaseSelect
                v-model="requestForm.goalType"
                title="Цель запроса"
                :options="mentoringTypeOptions"
              />

              <label class="field request-draft-panel__textarea">
                <span>Сообщение ментору</span>
                <textarea
                  v-model="requestForm.message"
                  placeholder="Коротко опишите цель, текущий уровень и удобный формат общения."
                />
              </label>

              <div class="request-draft-panel__actions">
                <BaseButton
                  size="l"
                  label="Отправить заявку"
                  :disabled="isLoading || !selectedMentor"
                  :loading="isLoading"
                  @click="submitRequest"
                />
                <p class="helper-text">
                  После отправки заявка появится в разделе “Заявки”, а ментор исчезнет из каталога.
                </p>
              </div>
            </div>
          </div>

          <div v-if="error" class="error-state">
            {{ error.message }}
          </div>
        </article>
      </div>

      <div v-if="successMessage" class="success-state">
        {{ successMessage }}
      </div>
    </template>
  </section>
</template>
