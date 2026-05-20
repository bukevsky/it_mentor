<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, BaseSelect } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import ProfileSearchSelect from "@/features/profile/ui/ProfileSearchSelect.vue";
import { useStudentDirectoryStore } from "@/features/student-profile/model/student-directory-store";
import CatalogSkillFilter from "@/shared/ui/CatalogSkillFilter.vue";
import {
  employmentTypeOptions,
  mentoringTypeOptions,
  workFormatOptions
} from "@/shared/lib/options";
import { formatList, fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const directoryStore = useStudentDirectoryStore();
const route = useRoute();

const { isAuthenticated } = storeToRefs(authStore);
const { cities, skills } = storeToRefs(dictionariesStore);
const {
  error,
  isLoading,
  requestForm,
  requestedStudentIds,
  results,
  searchForm,
  selectedStudent,
  studentsCount,
  successMessage
} = storeToRefs(directoryStore);

const isFilterCollapsed = ref(false);
const isRequestModalOpen = ref(false);
let searchDebounceId: ReturnType<typeof setTimeout> | undefined;

const selectedSkillIds = computed(() =>
  Array.isArray(searchForm.value.skillIds) ? searchForm.value.skillIds : []
);

const filterSearchKey = computed(() =>
  [
    searchForm.value.q.trim(),
    searchForm.value.cityId,
    selectedSkillIds.value.join(","),
    searchForm.value.employmentType,
    searchForm.value.workFormat
  ].join("|")
);

const scheduleStudentSearch = (delay = 280) => {
  if (searchDebounceId) {
    clearTimeout(searchDebounceId);
  }

  if (!isAuthenticated.value) {
    return;
  }

  searchDebounceId = setTimeout(() => {
    searchDebounceId = undefined;
    void directoryStore.searchStudents();
  }, delay);
};

watch(
  () => route.query.q,
  (query) => {
    searchForm.value.q = typeof query === "string" ? query : "";
  },
  { immediate: true }
);

watch(
  [() => isAuthenticated.value, filterSearchKey],
  ([nextValue], previousValue) => {
    if (!nextValue) {
      return;
    }

    const previousKey = previousValue?.[1];
    scheduleStudentSearch(previousKey === undefined ? 0 : 280);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  if (searchDebounceId) {
    clearTimeout(searchDebounceId);
  }
});

const cityOptions = computed(() => {
  const options = cities.value.map((city) => ({
    value: String(city.id),
    label: `${city.name}, ${city.region}`
  }));

  return [{ value: "", label: "Все города" }, ...options];
});

const skillOptions = computed(() => {
  const options = skills.value.map((skill) => ({
    value: String(skill.id),
    label: skill.name
  }));

  return options;
});

const employmentTypeFilterOptions = computed(() => [
  { value: "", label: "Любой тип" },
  ...employmentTypeOptions
]);

const workFormatFilterOptions = computed(() => [
  { value: "", label: "Любой формат" },
  ...workFormatOptions
]);

const studentCards = computed(() => {
  return (results.value?.content ?? []).map((student) => ({
    id: student.id,
    firstName: student.firstName,
    lastName: student.lastName,
    desiredPosition: student.desiredPosition,
    city: student.city,
    hoursPerWeek: student.hoursPerWeek,
    employmentTypes: student.employmentTypes,
    workFormats: student.workFormats,
    skills: student.skills.map((skill) => skill.skill.name),
    hasActiveRequest: requestedStudentIds.value.has(student.id),
    raw: student
  })).sort((left, right) => Number(left.hasActiveRequest) - Number(right.hasActiveRequest));
});

const displayedStudentCards = computed(() => studentCards.value);

const activeFilterLabels = computed(() => {
  const filters = [];

  if (searchForm.value.q) {
    filters.push(searchForm.value.q);
  }

  if (searchForm.value.cityId) {
    filters.push(cityOptions.value.find((item) => item.value === searchForm.value.cityId)?.label ?? "Город");
  }

  if (selectedSkillIds.value.length) {
    filters.push(`Навыки: ${selectedSkillIds.value.length}`);
  }

  if (searchForm.value.employmentType) {
    filters.push(getOptionLabel(employmentTypeOptions, searchForm.value.employmentType));
  }

  if (searchForm.value.workFormat) {
    filters.push(getOptionLabel(workFormatOptions, searchForm.value.workFormat));
  }

  return filters;
});

const resetFilters = () => {
  searchForm.value.q = "";
  searchForm.value.cityId = "";
  searchForm.value.skillIds = [];
  searchForm.value.employmentType = "";
  searchForm.value.workFormat = "";

  scheduleStudentSearch(0);
};

const initials = (firstName: string, lastName: string) => `${firstName.slice(0, 1)}${lastName.slice(0, 1)}`;

const availabilityText = (hoursPerWeek: number | null) => {
  return hoursPerWeek ? `${hoursPerWeek} ч/неделю` : "Загрузка не указана";
};

const openRequestModal = (student: (typeof studentCards.value)[number]["raw"]) => {
  directoryStore.selectStudent(student);
  isRequestModalOpen.value = true;
};

const closeRequestModal = () => {
  isRequestModalOpen.value = false;
  directoryStore.clearSelectedStudent();
};

const submitRequest = async () => {
  const isSubmitted = await directoryStore.submitRequest();

  if (isSubmitted) {
    isRequestModalOpen.value = false;
  }
};
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для доступа к студентам нужно войти в систему.
    </div>

    <template v-else>
      <div class="mentors-workspace">
        <section class="catalog-hero catalog-hero--students">
          <div class="catalog-hero__content">
            <p class="section-kicker">Главная > Студенты</p>
            <h1 class="workspace-title">Найдите студентов для менторства</h1>
            <p class="workspace-subtitle">
              Каталог строится из профилей студентов: позиция, город, навыки, формат работы и занятость.
            </p>

            <div class="catalog-hero__search">
              <BaseInput
                v-model="searchForm.q"
                title=""
                start-icon="search"
                placeholder="Поиск по имени, навыкам, цели или ключевым словам..."
              />
            </div>

          </div>

          <aside class="catalog-hero__visual">
            <div class="catalog-hero__stat">
              <strong>{{ studentsCount }}</strong>
              <span>студентов найдено</span>
            </div>
          </aside>
        </section>

        <article
          class="app-panel catalog-filter-panel catalog-filter-panel--horizontal"
          :class="{ 'catalog-filter-panel--collapsed': isFilterCollapsed }"
        >
          <div class="catalog-filter-panel__head">
            <div>
              <p class="section-kicker">Фильтры</p>
              <p v-if="!isFilterCollapsed" class="catalog-filter-panel__hint">Фильтры применяются сразу после выбора.</p>
              <p v-if="isFilterCollapsed && activeFilterLabels.length" class="catalog-filter-panel__collapsed-summary">
                {{ activeFilterLabels.join(' · ') }}
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
                class="catalog-filter-panel__toggle"
                variant="secondary"
                size="s"
                :label="isFilterCollapsed ? 'Фильтры' : 'Свернуть'"
                @click="isFilterCollapsed = !isFilterCollapsed"
              />
            </div>
          </div>

          <div v-if="!isFilterCollapsed" class="form-grid catalog-filter-panel__form">
            <ProfileSearchSelect
              v-model="searchForm.cityId"
              class="catalog-filter-panel__select"
              title="Город"
              placeholder="Все города"
              select-state="primary"
              :options="cityOptions"
            />
            <CatalogSkillFilter
              v-model="searchForm.skillIds"
              :options="skillOptions"
              class="catalog-filter-panel__select"
              title="Навык"
              placeholder="Любой навык"
            />
            <BaseSelect
              v-model="searchForm.employmentType"
              class="catalog-filter-panel__select"
              size="s"
              title="Тип занятости"
              placeholder="Любой тип"
              :options="employmentTypeFilterOptions"
            />
            <BaseSelect
              v-model="searchForm.workFormat"
              class="catalog-filter-panel__select"
              size="s"
              title="Формат работы"
              placeholder="Любой формат"
              :options="workFormatFilterOptions"
            />

            <div class="catalog-filter-panel__summary">
              <span>{{ isLoading ? "Обновляем выдачу..." : `Найдено студентов: ${studentsCount}` }}</span>
              <strong class="catalog-filter-panel__auto">Авто</strong>
            </div>
          </div>

          <div v-if="activeFilterLabels.length" class="catalog-active-filters">
            <span class="catalog-active-filters__label">Активные фильтры:</span>
            <span v-for="filter in activeFilterLabels" :key="filter" class="chip">{{ filter }}</span>
          </div>
        </article>

        <div class="catalog-layout">
          <main class="catalog-results">
            <div v-if="isLoading && !results" class="empty-state mt-6">
              Загружаем студентов...
            </div>

            <div v-else-if="error" class="error-state mt-6">
              {{ error.message }}
            </div>

            <div v-else-if="displayedStudentCards.length" class="student-directory-grid student-directory-grid--rich mt-6">
              <article
                v-for="student in displayedStudentCards"
                :key="student.id"
                class="student-catalog-card"
              >
                <div class="student-catalog-card__main">
                  <div class="student-catalog-card__head">
                    <div class="avatar catalog-avatar">{{ initials(student.firstName, student.lastName) }}</div>
                    <div>
                      <h3>{{ student.firstName }} {{ student.lastName }}</h3>
                      <p>{{ student.desiredPosition ?? "Желаемая позиция не указана" }}</p>
                      <span
                        :class="[
                          'status-pill',
                          student.hasActiveRequest ? 'status-pill--info' : 'status-pill--success'
                        ]"
                      >
                        {{ student.hasActiveRequest ? "Приглашение отправлено" : availabilityText(student.hoursPerWeek) }}
                      </span>
                    </div>
                  </div>

                  <p class="student-catalog-card__copy">
                    {{ student.raw.about ?? student.raw.max ?? "Описание профиля не заполнено." }}
                  </p>

                  <div class="chip-list">
                    <span v-for="skill in student.skills.slice(0, 5)" :key="skill" class="chip">
                      {{ skill }}
                    </span>
                    <span v-if="!student.skills.length" class="chip">Навыки не указаны</span>
                  </div>
                </div>

                <aside class="student-catalog-card__aside">
                  <p>{{ student.city?.name ?? "Город не указан" }}</p>
                  <span>{{ formatList(student.employmentTypes.map((type) => getOptionLabel(employmentTypeOptions, type))) }}</span>
                  <span>{{ formatList(student.workFormats.map((format) => getOptionLabel(workFormatOptions, format))) }}</span>
                  <BaseButton
                    size="m"
                    :label="student.hasActiveRequest ? 'Уже приглашён' : 'Связаться'"
                    :disabled="student.hasActiveRequest"
                    @click="openRequestModal(student.raw)"
                  />
                </aside>
              </article>
            </div>

            <div v-else class="empty-state mt-6">
              По текущим фильтрам студенты не найдены.
            </div>
          </main>

        </div>
      </div>

      <div v-if="isRequestModalOpen" class="request-modal" @click.self="closeRequestModal">
        <article class="app-panel request-draft-panel request-draft-panel--modal">
          <div class="request-draft-panel__header">
            <div>
              <p class="section-kicker">Заявка</p>
              <h3 class="section-title">Приглашение студенту</h3>
            </div>
            <BaseButton variant="clear" size="s" label="Закрыть" @click="closeRequestModal" />
          </div>

          <div class="request-draft-panel__body">
            <div v-if="selectedStudent" class="request-draft-panel__selected">
              <p class="section-kicker">Кому отправится</p>
              <h4 class="request-draft-panel__mentor">{{ fullName(selectedStudent) }}</h4>
              <div class="request-draft-panel__details">
                <span>{{ selectedStudent.desiredPosition ?? "Желаемая позиция не указана" }}</span>
                <span>{{ selectedStudent.city?.name ?? "Город не указан" }}</span>
                <span>{{ availabilityText(selectedStudent.hoursPerWeek) }}</span>
              </div>
            </div>

            <div class="request-draft-panel__form">
              <BaseSelect
                v-model="requestForm.goalType"
                title="Цель приглашения"
                :options="mentoringTypeOptions"
              />

              <label class="field request-draft-panel__textarea">
                <span>Сообщение студенту</span>
                <textarea
                  v-model="requestForm.message"
                  placeholder="Коротко опишите, чем можете помочь и какой формат менторства предлагаете."
                />
              </label>

              <div class="request-draft-panel__actions">
                <BaseButton
                  size="l"
                  label="Отправить приглашение"
                  :disabled="isLoading || !selectedStudent"
                  :loading="isLoading"
                  @click="submitRequest"
                />
                <p class="helper-text">
                  После отправки приглашение появится в разделе “Заявки”.
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
