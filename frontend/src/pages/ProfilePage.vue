<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { BaseButton, BaseInput, BaseSelect, Tabs } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useProfilesStore } from "@/features/profile/model/profiles-store";
import { useProfile, type MentorProfileSectionId, type StudentProfileSectionId } from "@/features/profile/model/use-profile";
import ProfileAbout from "@/features/profile/ui/ProfileAbout.vue";
import ProfileAvatarUploader from "@/features/profile/ui/ProfileAvatarUploader.vue";
import ProfileLanguages from "@/features/profile/ui/ProfileLanguages.vue";
import ProfilePreview from "@/features/profile/ui/ProfilePreview.vue";
import ProfileSearchSelect from "@/features/profile/ui/ProfileSearchSelect.vue";
import ProfileSection from "@/features/profile/ui/ProfileSection.vue";
import ProfileSkills from "@/features/profile/ui/ProfileSkills.vue";
import ProfileWorkPreferences from "@/features/profile/ui/ProfileWorkPreferences.vue";
import {
  mentoringChannelOptions,
  mentoringDurationOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions
} from "@/shared/lib/options";

const router = useRouter();
const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const profilesStore = useProfilesStore();

const { isAuthenticated, user } = storeToRefs(authStore);
const { cities, error: dictionariesError, isLoaded, isLoading, languages, skills } = storeToRefs(dictionariesStore);
const {
  avatarPreviewUrl,
  error,
  isBusy,
  isStudentDirty,
  isUploadingAvatar,
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
  mentorCompletionPercent,
  mentorCompletionTasks,
  mentorNextTasks,
  mentorPreview,
  mentorProfileSections,
  nextTasks,
  preview,
  studentProfileSections
} = useProfile();

const activeStudentSection = ref<StudentProfileSectionId>("main");
const activeMentorSection = ref<MentorProfileSectionId>("main");

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

const isMentorSectionComplete = (sectionId: MentorProfileSectionId) => {
  return mentorCompletionTasks.value.find((task) => task.id === sectionId)?.done;
};

const studentFullName = computed(() => `${studentForm.value.firstName} ${studentForm.value.lastName}`.trim() || "Имя студента");
const mentorFullName = computed(() => `${mentorForm.value.firstName} ${mentorForm.value.lastName}`.trim() || "Имя ментора");

const saveStudentProfile = async () => {
  await profilesStore.saveStudentProfile();
};

const refreshProfile = async () => {
  if (mode.value === "student") {
    await profilesStore.loadStudentProfile();
  } else {
    await profilesStore.loadMentorProfile();
  }

  await profilesStore.loadAvatar();
};

const logout = () => {
  authStore.logout();
  void router.push({ name: "auth" });
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
                class="btn-logout"
                label="Выйти"
                @click="logout"
              />
              <BaseButton
                variant="secondary"
                size="m"
                :label="isBusy ? 'Загрузка...' : 'Обновить'"
                :loading="isBusy"
                @click="refreshProfile"
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
              <ProfileAvatarUploader
                :full-name="studentFullName"
                :avatar-src="avatarPreviewUrl"
                :is-uploading="isUploadingAvatar"
                @upload="profilesStore.uploadAvatar"
              />
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
                    <ProfileSearchSelect
                      v-model="studentForm.cityId"
                      title="Город"
                      placeholder="Не выбран"
                      :options="cityOptions"
                      :select-state="citySelectState"
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

          <ProfilePreview :preview="preview" :avatar-src="avatarPreviewUrl" />
        </div>
      </template>

      <template v-else>
        <div class="student-profile-layout">
          <main class="student-profile-editor">
            <article class="app-panel profile-completion-panel">
              <ProfileAvatarUploader
                :full-name="mentorFullName"
                :avatar-src="avatarPreviewUrl"
                :is-uploading="isUploadingAvatar"
                @upload="profilesStore.uploadAvatar"
              />
              <div class="workspace-header">
                <div>
                  <p class="section-kicker">Заполненность</p>
                  <h3 class="section-title">{{ mentorCompletionPercent }}%</h3>
                </div>
                <span class="profile-save-pill">Профиль ментора</span>
              </div>
              <div class="progress-card__bar progress-card__bar--contrast">
                <div class="progress-card__fill" :style="{ width: `${mentorCompletionPercent}%` }"></div>
              </div>
              <ul class="profile-checklist">
                <li
                  v-for="task in mentorCompletionTasks"
                  :key="task.id"
                  :class="{ 'profile-checklist__item--done': task.done }"
                >
                  <span></span>
                  {{ task.label }}
                </li>
              </ul>
              <p v-if="mentorNextTasks.length" class="section-copy">
                Ближайший шаг: {{ mentorNextTasks[0].label }}.
              </p>
            </article>

            <div class="profile-section-stack">
              <ProfileSection
                v-for="section in mentorProfileSections"
                :key="section.id"
                :title="section.title"
                :summary="section.summary"
                :is-open="activeMentorSection === section.id"
                :is-complete="isMentorSectionComplete(section.id)"
                @open="activeMentorSection = section.id"
              >
                <div v-if="section.id === 'main'" class="student-section-grid">
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseInput v-model="mentorForm.firstName" title="Имя" />
                    <BaseInput v-model="mentorForm.lastName" title="Фамилия" />
                  </div>
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseInput v-model="mentorForm.position" title="Позиция" />
                    <BaseInput v-model="mentorForm.department" title="Департамент" />
                  </div>
                  <div class="form-grid form-grid--two conductor-grid">
                    <ProfileSearchSelect
                      v-model="mentorForm.cityId"
                      title="Город"
                      placeholder="Не выбран"
                      :options="cityOptions"
                      :select-state="citySelectState"
                    />
                    <BaseInput v-model="mentorForm.phone" title="Телефон" />
                  </div>
                </div>

                <div v-else-if="section.id === 'about'" class="student-section-grid">
                  <label class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
                    <span class="base-input__title">Описание</span>
                    <span class="input">
                      <textarea v-model="mentorForm.description" placeholder="Расскажите о своём опыте и стиле менторства."></textarea>
                    </span>
                  </label>
                  <label class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
                    <span class="base-input__title">С чем можете помочь</span>
                    <span class="input">
                      <textarea v-model="mentorForm.canHelpWith" placeholder="Например: ревью резюме, подготовка к собеседованиям, архитектура frontend-приложений."></textarea>
                    </span>
                  </label>
                  <label class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
                    <span class="base-input__title">Ожидания от менти</span>
                    <span class="input">
                      <textarea v-model="mentorForm.expectations" placeholder="Что важно подготовить до первой встречи."></textarea>
                    </span>
                  </label>
                  <label class="profile-input-shell base-input base-input__default base-input__size-m">
                    <span class="base-input__title">Контакт для связи</span>
                    <span class="input"><input v-model="mentorForm.max" placeholder="@username или ссылка" /></span>
                  </label>
                </div>

                <div v-else-if="section.id === 'format'" class="student-section-grid">
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseSelect v-model="mentorForm.mentoringType" size="m" title="Тип" :options="mentoringTypeOptions" />
                    <BaseSelect v-model="mentorForm.mentoringChannel" size="m" title="Канал" :options="mentoringChannelOptions" />
                  </div>
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseSelect v-model="mentorForm.mentoringDuration" size="m" title="Длительность" :options="mentoringDurationOptions" />
                    <label class="profile-input-shell base-input base-input__default base-input__size-m">
                      <span class="base-input__title">Частота встреч</span>
                      <span class="input"><input v-model="mentorForm.mentoringFrequency" placeholder="2 раза в неделю" /></span>
                    </label>
                  </div>
                </div>

                <ProfileSkills
                  v-else-if="section.id === 'skills'"
                  :skills="mentorForm.skills"
                  :skill-options="skillOptions"
                  :select-state="skillSelectState"
                  picker-title="Навыки"
                  picker-placeholder="Выберите навыки"
                  empty-text="Выберите навыки, по которым вы готовы менторить."
                  default-level="CONFIDENT"
                  @add="profilesStore.addMentorSkill"
                  @remove="profilesStore.removeMentorSkill"
                  @update-level="profilesStore.updateMentorSkillLevel"
                />

                <div v-else class="student-section-grid">
                  <div class="form-grid form-grid--two conductor-grid">
                    <BaseSelect v-model="mentorForm.recruitmentStatus" size="m" title="Статус набора" :options="recruitmentStatusOptions" />
                    <label class="profile-input-shell base-input base-input__default base-input__size-m">
                      <span class="base-input__title">Лимит менти</span>
                      <span class="input">
                        <input v-model.number="mentorForm.menteeLimit" type="number" min="1" max="20" />
                      </span>
                    </label>
                  </div>
                </div>
              </ProfileSection>
            </div>
          </main>

          <ProfilePreview :preview="mentorPreview" :avatar-src="avatarPreviewUrl" />
        </div>
      </template>

      <div v-if="successMessage" class="success-state mt-4">
        {{ successMessage }}
      </div>

      <div v-if="error" class="error-state mt-4">
        {{ error.message }}
      </div>
    </template>
  </section>
</template>
