<script setup lang="ts">
import { ref } from "vue";
import type { ErrorResponse, StudentProfileRequest, StudentProfileResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { formatJson, parseJsonInput, parseOptionalNumber } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { studentProfileApi } from "../api/student-profile-api";

const requestDraft = ref(
  formatJson<StudentProfileRequest>({
    firstName: "Иван",
    lastName: "Петров",
    middleName: "Сергеевич",
    phone: "+7 999 123-45-67",
    cityId: 1,
    desiredPosition: "Backend-разработчик",
    hoursPerWeek: 30,
    availableFrom: "2026-04-15",
    about: "Студент 4 курса, ищу практику.",
    max: "t.me/ivan_petrov",
    employmentTypes: ["PRACTICE", "INTERNSHIP"],
    workFormats: ["REMOTE", "HYBRID"],
    educations: [
      {
        institution: "МГУ",
        specialty: "Прикладная информатика",
        degree: "BACHELOR",
        educationForm: "FULL_TIME",
        startYear: 2022,
        graduationYear: 2026
      }
    ],
    languages: [{ languageId: 1, level: "B2" }],
    skills: [{ skillId: 1, level: "INTERMEDIATE" }]
  })
);
const studentProfileId = ref("");
const response = ref<StudentProfileResponse>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);

const execute = async (action: () => Promise<StudentProfileResponse>, path: string) => {
  isLoading.value = true;
  error.value = null;

  try {
    response.value = await action();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, path);
  } finally {
    isLoading.value = false;
  }
};

const upsert = async () => {
  await execute(() => studentProfileApi.upsert(parseJsonInput<StudentProfileRequest>(requestDraft.value)), "/profile/student");
};

const loadMine = async () => {
  await execute(() => studentProfileApi.getMine(), "/profile/student/me");
};

const loadById = async () => {
  const id = parseOptionalNumber(studentProfileId.value);

  if (id === undefined) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: "Укажите корректный ID студенческого профиля",
      path: "/profiles/students/{id}"
    };
    return;
  }

  await execute(() => studentProfileApi.getById(id), `/profiles/students/${id}`);
};
</script>

<template>
  <SectionCard
    description="Полное покрытие student profile: upsert, мой профиль и профиль по ID. Request body редактируется как JSON 1:1 с DTO из guide."
    kicker="Student Profile"
    title="Профиль студента"
  >
    <div class="section-grid">
      <div class="stack">
        <label class="field">
          <span>StudentProfileRequest</span>
          <textarea v-model="requestDraft" rows="18" />
        </label>

        <div class="actions">
          <button class="primary-button" :disabled="isLoading" type="button" @click="upsert">
            PUT /profile/student
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadMine">
            GET /profile/student/me
          </button>
        </div>

        <label class="field">
          <span>ID профиля</span>
          <input v-model="studentProfileId" placeholder="42" type="text" />
        </label>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="loadById">
          GET /profiles/students/{id}
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>

      <JsonPreview :value="response" title="StudentProfileResponse" />
    </div>
  </SectionCard>
</template>
