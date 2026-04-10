<script setup lang="ts">
import { computed, watch } from "vue";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, BaseSelect, Tabs } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useProfilesStore } from "@/features/profile/model/profiles-store";
import {
  employmentTypeOptions,
  languageLevelOptions,
  mentoringChannelOptions,
  mentoringDurationOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions,
  skillLevelOptions,
  workFormatOptions
} from "@/shared/lib/options";
import { formatList, fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const profilesStore = useProfilesStore();

const { isAuthenticated, user } = storeToRefs(authStore);
const { cities, error: dictionariesError, isLoaded, isLoading, languages, skills } = storeToRefs(dictionariesStore);
const {
  error,
  isBusy,
  mentorForm,
  mentorProfile,
  mode,
  preferredMode,
  studentForm,
  studentProfile,
  successMessage
} = storeToRefs(profilesStore);

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
    ? "Для текущего аккаунта доступен только профиль ментора."
    : "Для текущего аккаунта доступен только профиль студента."
});

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
      <article class="app-panel">
        <p class="section-kicker">Профиль</p>
        <h3 class="section-title">Личный кабинет</h3>
        <p class="section-copy">{{ activeRoleHint }}</p>
        <Tabs v-if="showModeTabs" v-model:active-tab="activeTab" class="base-tabs" :tabs="profileTabs" />
        <div class="base-actions mt-4">
          <BaseButton
            variant="secondary"
            size="l"
            :label="isBusy ? 'Загрузка...' : 'Обновить данные'"
            :loading="isBusy"
            @click="mode === 'student' ? profilesStore.loadStudentProfile() : profilesStore.loadMentorProfile()"
          />
        </div>

        <div v-if="dictionariesError" class="error-state mt-4">
          {{ dictionariesError.message }}
        </div>

        <div v-else-if="!isLoaded" class="empty-state">
          Загружаются словари для выбора города, навыков и языков...
        </div>
      </article>

      <div class="split-grid">
        <article v-if="mode === 'student'" class="app-panel">
          <p class="section-kicker">Студент</p>
          <h3 class="section-title">Профиль студента</h3>

          <div class="form-grid mt-6">
            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput v-model="studentForm.firstName" title="Имя" />
              <BaseInput v-model="studentForm.lastName" title="Фамилия" />
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput v-model="studentForm.middleName" title="Отчество" />
              <BaseInput v-model="studentForm.phone" title="Телефон" />
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
              <BaseInput v-model="studentForm.desiredPosition" title="Желаемая позиция" />
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseInput
                :model-value="studentForm.hoursPerWeek ? String(studentForm.hoursPerWeek) : ''"
                title="Часов в неделю"
                type="number"
                @update:model-value="studentForm.hoursPerWeek = $event ? Number($event) : null"
              />
              <div class="profile-input-shell profile-input-shell--date base-input base-input__default base-input__size-m">
                <label class="base-input__title">Доступен с</label>
                <div class="input">
                  <input v-model="studentForm.availableFrom" type="date" />
                </div>
              </div>
            </div>

            <div class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
              <label class="base-input__title">О себе</label>
              <div class="input">
                <textarea v-model="studentForm.about"></textarea>
              </div>
            </div>

            <BaseInput v-model="studentForm.max" title="Контакт" />

            <div class="field">
              <span>Формат занятости</span>
              <div class="choice-grid choice-grid--checkbox choice-button-grid">
                <BaseButton
                  v-for="option in employmentTypeOptions"
                  :key="option.value"
                  block
                  size="l"
                  variant="secondary"
                  :active="studentForm.employmentTypes.includes(option.value)"
                  :label="option.label"
                  @click="profilesStore.toggleItem(studentForm.employmentTypes, option.value)"
                />
              </div>
            </div>

            <div class="field">
              <span>Формат работы</span>
              <div class="choice-grid choice-grid--checkbox choice-button-grid">
                <BaseButton
                  v-for="option in workFormatOptions"
                  :key="option.value"
                  block
                  size="l"
                  variant="secondary"
                  :active="studentForm.workFormats.includes(option.value)"
                  :label="option.label"
                  @click="profilesStore.toggleItem(studentForm.workFormats, option.value)"
                />
              </div>
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="studentForm.skillId"
                size="m"
                title="Ключевой навык"
                placeholder="Не выбран"
                :state="skillSelectState"
                :options="skillOptions"
              />
              <BaseSelect v-model="studentForm.skillLevel" size="m" title="Уровень навыка" :options="skillLevelOptions" />
            </div>

            <div class="form-grid form-grid--two conductor-grid">
              <BaseSelect
                v-model="studentForm.languageId"
                size="m"
                title="Язык"
                placeholder="Не выбран"
                :state="languageSelectState"
                :options="languageOptions"
              />
              <BaseSelect v-model="studentForm.languageLevel" size="m" title="Уровень языка" :options="languageLevelOptions" />
            </div>

            <div class="base-actions">
              <BaseButton size="l" label="Сохранить" :disabled="isBusy" :loading="isBusy" @click="profilesStore.saveStudentProfile" />
              <BaseButton variant="secondary" size="l" label="Обновить" :disabled="isBusy" @click="profilesStore.loadStudentProfile" />
            </div>
          </div>
        </article>

        <article v-else class="app-panel">
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
              <div class="input">
                <textarea v-model="mentorForm.description"></textarea>
              </div>
            </div>

            <div class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
              <label class="base-input__title">Чем могу помочь</label>
              <div class="input">
                <textarea v-model="mentorForm.canHelpWith"></textarea>
              </div>
            </div>

            <div class="profile-input-shell profile-input-shell--textarea base-input base-input__default base-input__size-m">
              <label class="base-input__title">Ожидания</label>
              <div class="input">
                <textarea v-model="mentorForm.expectations"></textarea>
              </div>
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
              <BaseInput
                :model-value="mentorForm.menteeLimit ? String(mentorForm.menteeLimit) : ''"
                title="Лимит"
                type="number"
                @update:model-value="mentorForm.menteeLimit = $event ? Number($event) : null"
              />
              <BaseInput v-model="mentorForm.mentoringFrequency" title="Частота" />
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

            <BaseInput v-model="mentorForm.max" title="Контакт" />

            <div class="base-actions">
              <BaseButton size="l" label="Сохранить" :disabled="isBusy" :loading="isBusy" @click="profilesStore.saveMentorProfile" />
              <BaseButton variant="secondary" size="l" label="Обновить" :disabled="isBusy" @click="profilesStore.loadMentorProfile" />
            </div>
          </div>
        </article>

        <article class="profile-preview">
          <p class="section-kicker">Просмотр</p>
          <h3 class="profile-preview__title">Текущий {{ currentProfileTitle.toLowerCase() }}</h3>

          <div class="profile-preview__body">
            <div v-if="successMessage" class="success-state">
              {{ successMessage }}
            </div>

            <div v-if="error" class="error-state">
              {{ error.message }}
            </div>

            <template v-if="mode === 'student' && studentProfile">
              <ul class="clean-list mentor-meta">
                <li><span>ФИО</span><strong>{{ fullName(studentProfile) }}</strong></li>
                <li><span>Город</span><strong>{{ studentProfile.city?.name ?? "—" }}</strong></li>
                <li><span>Позиция</span><strong>{{ studentProfile.desiredPosition ?? "—" }}</strong></li>
                <li><span>Формат работы</span><strong>{{ formatList(studentProfile.workFormats) }}</strong></li>
                <li><span>Занятость</span><strong>{{ formatList(studentProfile.employmentTypes) }}</strong></li>
                <li><span>Навык</span><strong>{{ studentProfile.skills[0]?.skill.name ?? "—" }}</strong></li>
              </ul>
            </template>

            <template v-else-if="mode === 'mentor' && mentorProfile">
              <ul class="clean-list mentor-meta">
                <li><span>ФИО</span><strong>{{ fullName(mentorProfile) }}</strong></li>
                <li><span>Позиция</span><strong>{{ mentorProfile.position ?? "—" }}</strong></li>
                <li><span>Город</span><strong>{{ mentorProfile.city?.name ?? "—" }}</strong></li>
                <li>
                  <span>Формат</span>
                  <strong>
                    {{ getOptionLabel(mentoringTypeOptions, mentorProfile.mentoringType) }}
                    /
                    {{ getOptionLabel(mentoringChannelOptions, mentorProfile.mentoringChannel) }}
                  </strong>
                </li>
                <li><span>Набор</span><strong>{{ getOptionLabel(recruitmentStatusOptions, mentorProfile.recruitmentStatus) }}</strong></li>
                <li><span>Навык</span><strong>{{ mentorProfile.skills[0]?.skill.name ?? "—" }}</strong></li>
              </ul>
            </template>

            <div v-else class="empty-state">
              Данные профиля пока не загружены.
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>
