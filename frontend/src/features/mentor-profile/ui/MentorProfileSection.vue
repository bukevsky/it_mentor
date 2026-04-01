<script setup lang="ts">
import { reactive, ref } from "vue";
import type {
  ErrorResponse,
  MentorProfileRequest,
  MentorProfileResponse,
  MentorSearchParams,
  PagedResponse,
  MentorCardResponse
} from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { formatJson, parseJsonInput, parseNumberList, parseOptionalNumber } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { mentorProfileApi } from "../api/mentor-profile-api";

const requestDraft = ref(
  formatJson<MentorProfileRequest>({
    firstName: "Алексей",
    lastName: "Смирнов",
    middleName: "Дмитриевич",
    position: "Senior Backend Developer",
    department: "Platform Engineering",
    cityId: 1,
    phone: "+7 999 987-65-43",
    max: "t.me/alexey_smirnov",
    description: "Помогаю войти в Java/Spring backend.",
    expectations: "База по Java и 10 часов в неделю.",
    canHelpWith: "Backend, code review, system design",
    mentoringType: "INTERNSHIP",
    mentoringChannel: "MIXED",
    mentoringFrequency: "2 раза в неделю",
    mentoringDuration: "THREE_MONTHS",
    menteeLimit: 5,
    recruitmentStatus: "OPEN",
    skills: [{ skillId: 1, level: "CONFIDENT" }]
  })
);

const mentorProfileId = ref("");
const response = ref<MentorProfileResponse>();
const searchResponse = ref<PagedResponse<MentorCardResponse>>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);
const searchForm = reactive({
  q: "",
  skillIds: "",
  cityId: "",
  recruitmentStatus: "",
  mentoringType: "",
  mentoringChannel: "",
  page: "0",
  size: "20",
  sort: "createdAt,desc"
});

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

const upsert = async () => {
  await execute(
    () => mentorProfileApi.upsert(parseJsonInput<MentorProfileRequest>(requestDraft.value)),
    "/profile/mentor",
    response
  );
};

const loadMine = async () => {
  await execute(() => mentorProfileApi.getMine(), "/profile/mentor/me", response);
};

const loadById = async () => {
  const id = parseOptionalNumber(mentorProfileId.value);

  if (id === undefined) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: "Укажите корректный ID профиля ментора",
      path: "/profiles/mentors/{id}"
    };
    return;
  }

  await execute(() => mentorProfileApi.getById(id), `/profiles/mentors/${id}`, response);
};

const search = async () => {
  const params: MentorSearchParams = {
    q: searchForm.q || undefined,
    skillIds: parseNumberList(searchForm.skillIds),
    cityId: parseOptionalNumber(searchForm.cityId),
    recruitmentStatus: (searchForm.recruitmentStatus || undefined) as MentorSearchParams["recruitmentStatus"],
    mentoringType: (searchForm.mentoringType || undefined) as MentorSearchParams["mentoringType"],
    mentoringChannel: (searchForm.mentoringChannel || undefined) as MentorSearchParams["mentoringChannel"],
    page: parseOptionalNumber(searchForm.page) ?? 0,
    size: parseOptionalNumber(searchForm.size) ?? 20,
    sort: searchForm.sort || undefined
  };

  await execute(() => mentorProfileApi.search(params), "/profiles/mentors", searchResponse);
};
</script>

<template>
  <SectionCard
    description="Модуль ментора покрывает upsert, чтение своего/чужого профиля и поиск по карточкам mentor search."
    kicker="Mentor Profile"
    title="Профиль ментора и поиск"
  >
    <div class="section-grid section-grid--wide">
      <div class="stack">
        <label class="field">
          <span>MentorProfileRequest</span>
          <textarea v-model="requestDraft" rows="18" />
        </label>

        <div class="actions">
          <button class="primary-button" :disabled="isLoading" type="button" @click="upsert">
            PUT /profile/mentor
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadMine">
            GET /profile/mentor/me
          </button>
        </div>

        <label class="field">
          <span>ID профиля ментора</span>
          <input v-model="mentorProfileId" placeholder="10" type="text" />
        </label>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="loadById">
          GET /profiles/mentors/{id}
        </button>
      </div>

      <div class="stack">
        <div class="form-grid form-grid--columns">
          <label class="field">
            <span>q</span>
            <input v-model.trim="searchForm.q" placeholder="Смирнов" type="text" />
          </label>
          <label class="field">
            <span>skillIds</span>
            <input v-model.trim="searchForm.skillIds" placeholder="1,3" type="text" />
          </label>
          <label class="field">
            <span>cityId</span>
            <input v-model.trim="searchForm.cityId" placeholder="1" type="text" />
          </label>
          <label class="field">
            <span>recruitmentStatus</span>
            <input v-model.trim="searchForm.recruitmentStatus" placeholder="OPEN" type="text" />
          </label>
          <label class="field">
            <span>mentoringType</span>
            <input v-model.trim="searchForm.mentoringType" placeholder="INTERNSHIP" type="text" />
          </label>
          <label class="field">
            <span>mentoringChannel</span>
            <input v-model.trim="searchForm.mentoringChannel" placeholder="MIXED" type="text" />
          </label>
          <label class="field">
            <span>page</span>
            <input v-model.trim="searchForm.page" type="text" />
          </label>
          <label class="field">
            <span>size</span>
            <input v-model.trim="searchForm.size" type="text" />
          </label>
          <label class="field">
            <span>sort</span>
            <input v-model.trim="searchForm.sort" placeholder="createdAt,desc" type="text" />
          </label>
        </div>

        <button class="ghost-button" :disabled="isLoading" type="button" @click="search">
          GET /profiles/mentors
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>
    </div>

    <div class="section-grid">
      <JsonPreview :value="response" title="MentorProfileResponse" />
      <JsonPreview :value="searchResponse" title="PagedResponse<MentorCardResponse>" />
    </div>
  </SectionCard>
</template>
