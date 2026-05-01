<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { BaseButton, BaseInput, BaseSelect, Tabs } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useProfilesStore } from "@/features/profile/model/profiles-store";
import { useProfile, type StudentProfileSectionId } from "@/features/profile/model/use-profile";
import ProfileAbout from "@/features/profile/ui/ProfileAbout.vue";
import ProfileLanguages from "@/features/profile/ui/ProfileLanguages.vue";
import ProfilePreview from "@/features/profile/ui/ProfilePreview.vue";
import ProfileSection from "@/features/profile/ui/ProfileSection.vue";
import ProfileSkills from "@/features/profile/ui/ProfileSkills.vue";
import ProfileWorkPreferences from "@/features/profile/ui/ProfileWorkPreferences.vue";
import {
  mentoringChannelOptions,
  mentoringDurationOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions,
  skillLevelOptions
} from "@/shared/lib/options";

const router = useRouter();
const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const profilesStore = useProfilesStore();

const { isAuthenticated, user } = storeToRefs(authStore);
const { cities, error: dictionariesError, isLoaded, isLoading, languages, skills } = storeToRefs(dictionariesStore);
const {
  error,
  isBusy,
  isStudentDirty,
  mentorForm,
  mode,
  preferredMode,
  studentForm,
  successMessage
} = storeToRefs(profilesStore);
const {
  completionPercent,
  completionTasks,
  hasUnsavedStudentChanges,
  nextTasks,
  preview,
  studentProfileSections
} = useProfile();

const activeStudentSection = ref<StudentProfileSectionId>("main");

const availableModes = computed(() => {
  return preferredMode.value === "mentor" ? ["Ментор"] : ["Студент"];
});

const profileTabs = computed(() => availableModes.value.map((name) => ({ name })));
const showModeTabs = computed(() => profileTabs.value.length > 1);

const activeTab = computed({
  get: () => (mode.value === "mentor" ? "Ментор" : "Студент"),
  set: (value: string) => {
    profilesStore.setMode(value === "Ментор" ? "mentor" : "student");
  }
});

const cityOptions = computed(() => {
  return cities.value.length
    ? cities.value.map((city) => ({
        value: String(city.id),
        label: `${city.name}, ${city.region}`
      }))
    : [{ value: "", label: isLoading.value ? "Загрузка городов..." : "Справочник недоступен" }];
});

const skillOptions = computed(() => {
  return skills.value.length
    ? skills.value.map((skill) => ({
        value: String(skill.id),
        label: skill.name
      }))
    : [{ value: "", label: isLoading.value ? "Загрузка навыков..." : "Справочник недоступен" }];
});

const languageOptions = computed(() => {
  return languages.value.length
    ? languages.value.map((language) => ({
        value: String(language.id),
        label: language.name
      }))
    : [{ value: "", label: isLoading.value ? "Загрузка языков..." : "Справочник недоступен" }];
});

const citySelectState = computed(() => (cities.value.length ? "default" : "disabled"));
const skillSelectState = computed(() => (skills.value.length ? "default" : "disabled"));
const languageSelectState = computed(() => (languages.value.length ? "default" : "disabled"));

const currentProfileTitle = computed(() => {
  return mode.value === "mentor" ? "Профиль ментора" : "Профиль студента";
});

const activeRoleHint = computed(() => {
  if (!user.value) {
    return "Профиль станет доступен после входа.";
  }

  return user.value.roles.includes("MENTOR")
    ? "Для текущего аккаунта доступен профиль ментора."
    : "Заполните профиль по шагам, чтобы менторам было проще оценить ваш запрос.";
});

const isSectionComplete = (sectionId: StudentProfileSectionId) => {
  if (sectionId === "main") {
    return completionTasks.value.find((task) => task.id === "main")?.done;
  }

  return completionTasks.value.find((task) => task.id === sectionId)?.done;
};

const saveStudentProfile = async () => {
  await profilesStore.saveStudentProfile();
};

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (!nextValue) {
      return;
    }

    void profilesStore.initialize();
  },
  { immediate: true }
);
</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для работы с профилем нужно войти в систему.
    </div>

    <template v-else>
      <article class="app-panel profile-workspace-header">
        <div class="workspace-header">
          <div>
            <h1 class="workspace-title">{{ currentProfileTitle }}</h1>
            <p class="workspace-subtitle">{{ activeRoleHint }}</p>
          </div>
          <div class="profile-save-state">
            <span v-if="mode === 'student'">
              {{ hasUnsavedStudentChanges ? "Есть изменения" : "Сохранено" }}
            </span>
            <div class="base-actions">
              <BaseButton
                variant="secondary"
                size="m"
                :label="isBusy ? 'Загрузка...' : 'Обновить'"
                :loading="isBusy"
                @click="mode === 'student' ? profilesStore.loadStudentProfile() : profilesStore.loadMentorProfile()"
              />
              <BaseButton
                size="m"
                :label="mode === 'student' && isStudentDirty ? 'Сохранить изменения' : 'Сохранить'"
                :disabled="isBusy"
                :loading="isBusy"
                @click="mode === 'student' ? saveStudentProfile() : profilesStore.saveMentorProfile()"
              />
            </div>
          </div>
        </div>

        <Tabs v-if="showModeTabs" v-model:active-tab="activeTab" class="base-tabs" :tabs="profileTabs" />

        <div v-if="dictionariesError" class="error-state mt-4">
          {{ dictionariesError.message }}
        </div>

        <div v-else-if="!isLoaded" class="empty-state">
          Загружаются словари для выбора города, навыков и языков...
        </div>
      </article>

      <template v-if="mode === 'student'">
        <div class="student-profile-layout">
          <main class="student-profile-editor">
            <article class="app-panel profile-completion-panel">
              <div class="workspace-header">
                <div>
                  <p class="section-kicker">Заполненность</p>
                  <h3 class="section-title">{{ completionPercent }}%</h3>
                </div>
                <span class="profile-save-pill">{{ hasUnsavedStudentChanges ? "Черновик" : "Сохранено" }}</span>
              </div>
              <div class="progress-card__bar progress-card__bar--contrast">
                <div class="progress-card__fill" :style="{ width: `${completionPercent}%` }"></div>
              </div>
              <ul class="profile-checklist">
                <li
                  v-for="task in completionTasks"
                  :key="task.id"
                  :class="{ 'profile-checklist__item--done': task.done }"
                >
                  <span></span>
                  {{ task.label }}
                </li>
              </ul>
              <p v-if="nextTasks.length" class="section-copy">
                Ближайший шаг: {{ nextTasks[0].label }}.
              </p>
            </article>

            <div class="profile-section-stack">
              <ProfileSection
                v-for="section in studentProfileSections"
                :key="section.id"
                :title="section.title"
                :summary="section.summary"
                :is-open="activeStudentSection === section.id"
                :is-complete="isSectionComplete(section.id)"
                @open="activeStudentSection = section.id"
              >
                <div v-if="section.id === 'main'" class="student-section-grid">
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseInput v-model="studentForm.firstName" title="Имя" placeholder="Имя" />
                    <BaseInput v-model="studentForm.lastName" title="Фамилия" placeholder="Фамилия" />
                  </div>
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseSelect
                      v-model="studentForm.cityId"
                      size="m"
                      title="Город"
                      placeholder="Не выбран"
                      :state="citySelectState"
                      :options="cityOptions"
                    />
                    <BaseInput
                      v-model="studentForm.desiredPosition"
                      title="Желаемая позиция"
                      placeholder="Frontend Developer"
                    />
                  </div>
                </div>

                <ProfileAbout
                  v-else-if="section.id === 'about'"
                  v-model:about="studentForm.about"
                  v-model:max="studentForm.max"
                />

                <ProfileSkills
                  v-else-if="section.id === 'skills'"
                  :skills="studentForm.skills"
                  :skill-options="skillOptions"
                  :select-state="skillSelectState"
                  @add="profilesStore.addStudentSkill"
                  @remove="profilesStore.removeStudentSkill"
                  @update-level="profilesStore.updateStudentSkillLevel"
                />

                <ProfileWorkPreferences
                  v-else-if="section.id === 'work'"
                  v-model:hours-per-week="studentForm.hoursPerWeek"
                  v-model:available-from="studentForm.availableFrom"
                  :employment-types="studentForm.employmentTypes"
                  :work-formats="studentForm.workFormats"
                  @toggle-employment="profilesStore.toggleItem(studentForm.employmentTypes, $event)"
                  @toggle-work-format="profilesStore.toggleItem(studentForm.workFormats, $event)"
                />

                <ProfileLanguages
                  v-else-if="section.id === 'languages'"
                  :languages="studentForm.languages"
                  :language-options="languageOptions"
                  :select-state="languageSelectState"
                  @add="profilesStore.addStudentLanguage"
                  @remove="profilesStore.removeStudentLanguage"
                  @update-level="profilesStore.updateStudentLanguageLevel"
                />

                <div v-else class="profile-files-section">
                  <div>
                    <h3 class="section-title">Резюме и материалы</h3>
                    <p class="section-copy">
                      Файлы управляются в отдельном менеджере: резюме, портфолио, аватар и вложения.
                    </p>
                  </div>
                  <BaseButton size="m" label="Открыть файлы" @click="router.push({ name: 'files' })" />
                </div>
              </ProfileSection>
            </div>
          </main>

          <ProfilePreview :preview="preview" />
        </div>
      </template>

      <div v-else class="profile-form-grid">
        <article class="app-panel">
          <p class="section-kicker">Ментор</p>
          <h3 class="section-title">Профиль ментора</h3>

          <div class="form-grid mt-6">
            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput v-model="mentorForm.firstName" title="Имя" />
              <BaseInput v-model="mentorForm.lastName" title="Фамилия" />
            </div>
            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput v-model="mentorForm.position" title="Позиция" />
              <BaseInput v-model="mentorForm.department" title="Департамент" />
            </div>
            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="mentorForm.cityId"
                size="m"
                title="Город"
                placeholder="Не выбран"
                :state="citySelectState"
                :options="cityOptions"
              />
              <BaseInput v-model="mentorForm.phone" title="Телефон" />
            </div>
            <div class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
              <label class="base-input__title">Описание</label>
              <div class="input"><textarea v-model="mentorForm.description"></textarea></div>
            </div>
            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect v-model="mentorForm.mentoringType" size="m" title="Тип" :options="mentoringTypeOptions" />
              <BaseSelect v-model="mentorForm.mentoringChannel" size="m" title="Канал" :options="mentoringChannelOptions" />
            </div>
            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect v-model="mentorForm.mentoringDuration" size="m" title="Длительность" :options="mentoringDurationOptions" />
              <BaseSelect v-model="mentorForm.recruitmentStatus" size="m" title="Статус набора" :options="recruitmentStatusOptions" />
            </div>
            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="mentorForm.skillId"
                size="m"
                title="Навык"
                placeholder="Не выбран"
                :state="skillSelectState"
                :options="skillOptions"
              />
              <BaseSelect v-model="mentorForm.skillLevel" size="m" title="Уровень" :options="skillLevelOptions" />
            </div>
          </div>
        </article>
      </div>

      <div v-if="successMessage" class="success-state mt-4">
        {{ successMessage }}
      </div>

      <div v-if="error" class="error-state mt-4">
        {{ error.message }}
      </div>
    </template>
  </section>
</template>
