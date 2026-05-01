<script setup lang="ts">
import { computed, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, BaseSelect } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useStudentDirectoryStore } from "@/features/student-profile/model/student-directory-store";
import {
  employmentTypeOptions,
  mentoringTypeOptions,
  workFormatOptions
} from "@/shared/lib/options";
import { formatList, fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const directoryStore = useStudentDirectoryStore();

const { isAuthenticated } = storeToRefs(authStore);
const { cities, skills } = storeToRefs(dictionariesStore);
const {
  error,
  isLoading,
  requestForm,
  results,
  searchForm,
  selectedStudent,
  studentsCount,
  successMessage
} = storeToRefs(directoryStore);

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void directoryStore.searchStudents();
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
    raw: student
  }));
});

const resetFilters = () => {
  searchForm.value.q = "";
  searchForm.value.cityId = "";
  searchForm.value.skillId = "";
  searchForm.value.employmentType = "";
  searchForm.value.workFormat = "";

  void directoryStore.searchStudents();
};

const initials = (firstName: string, lastName: string) => `${firstName.slice(0, 1)}${lastName.slice(0, 1)}`;

const availabilityText = (hoursPerWeek: number | null) => {
  return hoursPerWeek ? `${hoursPerWeek} ч/неделю` : "Загрузка не указана";
};
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для доступа к студентам нужно войти в систему.
    </div>

    <template v-else>
      <div class="mentors-workspace">
        <div class="workspace-header">
          <div>
            <p class="section-kicker">Главная > Студенты</p>
            <h1 class="workspace-title">Директория студентов</h1>
          </div>
        </div>

        <article class="app-panel catalog-filter-panel catalog-filter-panel--horizontal">
          <div class="catalog-filter-panel__head">
            <p class="section-kicker">Фильтры</p>
            <BaseButton
              class="catalog-filter-panel__reset"
              variant="clear"
              size="s"
              label="Сбросить"
              @click="resetFilters"
            />
          </div>

          <div class="form-grid catalog-filter-panel__form">
            <BaseInput
              v-model="searchForm.q"
              class="catalog-filter-panel__search"
              size="s"
              title="Поиск"
              placeholder="Имя, позиция или навык"
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
              v-model="searchForm.employmentType"
              class="catalog-filter-panel__select"
              size="s"
              title="Тип занятости"
              placeholder="Любой тип"
              :options="employmentTypeOptions"
            />
            <BaseSelect
              v-model="searchForm.workFormat"
              class="catalog-filter-panel__select"
              size="s"
              title="Формат работы"
              placeholder="Любой формат"
              :options="workFormatOptions"
            />

            <div class="base-actions">
              <BaseButton
                class="catalog-filter-panel__submit"
                size="m"
                :label="isLoading ? 'Поиск...' : 'Применить фильтры'"
                :loading="isLoading"
                @click="directoryStore.searchStudents"
              />
            </div>
            <div class="catalog-filter-panel__summary">
              <span>Найдено студентов: {{ studentsCount }}</span>
            </div>
          </div>
        </article>

        <div>
          <div v-if="isLoading && !results" class="empty-state mt-6">
            Загружаем студентов...
          </div>

          <div v-else-if="error" class="error-state mt-6">
            {{ error.message }}
          </div>

          <div v-else-if="studentCards.length" class="student-directory-grid mt-6">
            <article
              v-for="student in studentCards"
              :key="student.id"
              class="mentor-card student-card"
            >
              <div class="student-card__top">
                <div class="avatar">{{ initials(student.firstName, student.lastName) }}</div>
                <span class="status-pill status-pill--info">
                  {{ availabilityText(student.hoursPerWeek) }}
                </span>
              </div>

              <div>
                <h4 class="mentor-card__title">{{ student.firstName }} {{ student.lastName }}</h4>
                <p class="mentor-card__copy">
                  {{ student.desiredPosition ?? "Желаемая позиция не указана" }}
                </p>
                <p class="helper-text">
                  {{ student.city?.name ?? "Город не указан" }}
                  ·
                  {{ formatList(student.employmentTypes.map((type) => getOptionLabel(employmentTypeOptions, type))) }}
                </p>
                <p class="helper-text">
                  {{ formatList(student.workFormats.map((format) => getOptionLabel(workFormatOptions, format))) }}
                </p>
              </div>

              <div class="chip-list">
                <span v-for="skill in student.skills.slice(0, 4)" :key="skill" class="chip">
                  {{ skill }}
                </span>
                <span v-if="!student.skills.length" class="chip">Навыки не указаны</span>
              </div>

              <div class="mentor-card__actions mentor-card__actions--single">
                <BaseButton
                  size="m"
                  label="Выбрать для приглашения"
                  @click="directoryStore.selectStudent(student.raw)"
                />
              </div>
            </article>
          </div>

          <div v-else class="empty-state mt-6">
            По текущим фильтрам студенты не найдены.
          </div>
        </div>
      </div>

      <article class="app-panel request-draft-panel">
        <div class="request-draft-panel__header">
          <div>
            <p class="section-kicker">Заявка</p>
            <h3 class="section-title">Приглашение студенту</h3>
          </div>
          <span
            :class="[
              'request-draft-panel__status',
              selectedStudent ? 'request-draft-panel__status--ready' : ''
            ]"
          >
            {{ selectedStudent ? "Студент выбран" : "Студент не выбран" }}
          </span>
        </div>

        <div class="request-draft-panel__body">
          <div class="request-draft-panel__selected">
            <template v-if="selectedStudent">
              <p class="section-kicker">Кому отправится</p>
              <h4 class="request-draft-panel__mentor">{{ fullName(selectedStudent) }}</h4>
              <div class="request-draft-panel__details">
                <span>{{ selectedStudent.desiredPosition ?? "Желаемая позиция не указана" }}</span>
                <span>{{ selectedStudent.city?.name ?? "Город не указан" }}</span>
                <span>{{ availabilityText(selectedStudent.hoursPerWeek) }}</span>
              </div>
            </template>

            <template v-else>
              <p class="section-kicker">Кому отправится</p>
              <h4 class="request-draft-panel__mentor">Выберите студента</h4>
              <p class="section-copy">
                Нажмите “Выбрать для приглашения” в карточке студента. После этого здесь появятся его данные.
              </p>
            </template>
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
                @click="directoryStore.submitRequest"
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
        <div v-if="successMessage" class="success-state">
          {{ successMessage }}
        </div>
      </article>
    </template>
  </section>
</template>
