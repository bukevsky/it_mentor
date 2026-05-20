<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseIcon, BaseInput, BaseSelect } from "conductor";
import type { MentorCardResponse, ReviewResponse } from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useMentorDirectoryStore } from "@/features/mentor-profile/model/mentor-directory-store";
import ProfileSearchSelect from "@/features/profile/ui/ProfileSearchSelect.vue";
import { reviewsApi } from "@/features/reviews/api/reviews-api";
import ReviewItem from "@/features/reviews/ui/ReviewItem.vue";
import ReviewRating from "@/features/reviews/ui/ReviewRating.vue";
import CatalogSkillFilter from "@/shared/ui/CatalogSkillFilter.vue";
import {
  mentoringChannelOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions
} from "@/shared/lib/options";
import { fullName, getOptionLabel } from "@/shared/lib/presenters";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();
const directoryStore = useMentorDirectoryStore();
const route = useRoute();

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
const isReviewsModalOpen = ref(false);
const selectedReviewsMentor = ref<MentorCardResponse | null>(null);
let searchDebounceId: ReturnType<typeof setTimeout> | undefined;

type MentorReviewState = {
  reviews: ReviewResponse[];
  averageRating: number;
  totalReviews: number;
  isLoading: boolean;
  error: string | null;
};

const mentorReviews = ref<Record<number, MentorReviewState>>({});

const buildReviewState = (
  reviews: ReviewResponse[],
  isLoading = false,
  error: string | null = null
): MentorReviewState => {
  const totalReviews = reviews.length;
  const ratingSum = reviews.reduce((sum, review) => sum + review.rating, 0);

  return {
    reviews,
    averageRating: totalReviews ? Number((ratingSum / totalReviews).toFixed(1)) : 0,
    totalReviews,
    isLoading,
    error
  };
};

const setReviewState = (mentorId: number, state: MentorReviewState) => {
  mentorReviews.value = {
    ...mentorReviews.value,
    [mentorId]: state
  };
};

const getReviewState = (mentorId: number) =>
  mentorReviews.value[mentorId] ?? buildReviewState([]);

const selectedSkillIds = computed(() =>
  Array.isArray(searchForm.skillIds) ? searchForm.skillIds : []
);

const loadMentorReviews = async (mentorId: number, force = false) => {
  const currentState = mentorReviews.value[mentorId];

  if (!force && currentState && !currentState.isLoading) {
    return;
  }

  setReviewState(mentorId, buildReviewState(currentState?.reviews ?? [], true));

  try {
    const content: ReviewResponse[] = [];
    let page = 0;
    let last = false;

    while (!last) {
      const response = await reviewsApi.getMentorReviews(mentorId, { page, size: 100 });
      content.push(...response.content);
      last = response.last;
      page += 1;
    }

    setReviewState(mentorId, buildReviewState(content));
  } catch (rawError) {
    const message = rawError instanceof Error ? rawError.message : "Не удалось загрузить отзывы.";
    setReviewState(mentorId, buildReviewState(currentState?.reviews ?? [], false, message));
  }
};

const filterSearchKey = computed(() =>
  [
    searchForm.q.trim(),
    searchForm.cityId,
    selectedSkillIds.value.join(","),
    searchForm.recruitmentStatus,
    searchForm.mentoringType,
    searchForm.mentoringChannel
  ].join("|")
);

const scheduleMentorSearch = (delay = 280) => {
  if (searchDebounceId) {
    clearTimeout(searchDebounceId);
  }

  if (!isAuthenticated.value) {
    return;
  }

  searchDebounceId = setTimeout(() => {
    searchDebounceId = undefined;
    void directoryStore.searchMentors();
  }, delay);
};

watch(
  () => route.query.q,
  (query) => {
    const queryValue = typeof query === "string" ? query : "";

    if (searchForm.q !== queryValue) {
      searchForm.q = queryValue;
    }
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
    scheduleMentorSearch(previousKey === undefined ? 0 : 280);
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

const recruitmentFilterOptions = computed(() => [
  { value: "", label: "Любой статус" },
  ...recruitmentStatusOptions
]);

const mentoringTypeFilterOptions = computed(() => [
  { value: "", label: "Любая цель" },
  ...mentoringTypeOptions
]);

const mentoringChannelFilterOptions = computed(() => [
  { value: "", label: "Любой формат" },
  ...mentoringChannelOptions
]);

const mentorCards = computed(() => {
  return (results.value?.content ?? [])
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
      hasActiveRequest: requestedMentorIds.value.has(mentor.id),
      raw: mentor
    }))
    .sort((left, right) => Number(left.hasActiveRequest) - Number(right.hasActiveRequest));
});

const displayedMentorCards = computed(() => mentorCards.value);

const mentorsTotal = computed(() => results.value?.totalElements ?? displayedMentorCards.value.length);
const openMentorsCount = computed(() =>
  displayedMentorCards.value.filter((mentor) => mentor.recruitmentStatus === "OPEN").length
);

const activeFilterLabels = computed(() => {
  const filters = [];

  if (searchForm.q) {
    filters.push(searchForm.q);
  }

  if (searchForm.cityId) {
    filters.push(cityOptions.value.find((item) => item.value === searchForm.cityId)?.label ?? "Город");
  }

  if (selectedSkillIds.value.length) {
    filters.push(`Навыки: ${selectedSkillIds.value.length}`);
  }

  if (searchForm.recruitmentStatus) {
    filters.push(getOptionLabel(recruitmentStatusOptions, searchForm.recruitmentStatus));
  }

  if (searchForm.mentoringType) {
    filters.push(getOptionLabel(mentoringTypeOptions, searchForm.mentoringType));
  }

  if (searchForm.mentoringChannel) {
    filters.push(getOptionLabel(mentoringChannelOptions, searchForm.mentoringChannel));
  }

  return filters;
});

const selectedReviewState = computed(() =>
  selectedReviewsMentor.value ? getReviewState(selectedReviewsMentor.value.id) : buildReviewState([])
);

const selectedReviewsMentorName = computed(() =>
  selectedReviewsMentor.value ? fullName(selectedReviewsMentor.value) : "Ментор"
);

const visibleMentorIds = computed(() => displayedMentorCards.value.map((mentor) => mentor.id).join(","));

watch(
  visibleMentorIds,
  () => {
    displayedMentorCards.value.forEach((mentor) => {
      void loadMentorReviews(mentor.id);
    });
  },
  { immediate: true }
);

const openRequestModal = (mentor: (typeof mentorCards.value)[number]["raw"]) => {
  directoryStore.selectMentor(mentor);
  isRequestModalOpen.value = true;
};

const closeRequestModal = () => {
  isRequestModalOpen.value = false;
  directoryStore.clearSelectedMentor();
};

const openReviewsModal = (mentor: (typeof mentorCards.value)[number]["raw"]) => {
  selectedReviewsMentor.value = mentor;
  isReviewsModalOpen.value = true;
  void loadMentorReviews(mentor.id, true);
};

const closeReviewsModal = () => {
  isReviewsModalOpen.value = false;
  selectedReviewsMentor.value = null;
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
  searchForm.skillIds = [];
  searchForm.recruitmentStatus = "";
  searchForm.mentoringType = "";
  searchForm.mentoringChannel = "";

  scheduleMentorSearch(0);
};

</script>

<template>
  <section class="app-section">
    <div v-if="!isAuthenticated" class="empty-state">
      Для доступа к менторам нужно войти в систему.
    </div>

    <template v-else>
      <div class="mentors-workspace">
        <section class="catalog-hero catalog-hero--mentors">
          <div class="catalog-hero__content">
            <p class="section-kicker">Главная > Менторы</p>
            <h1 class="workspace-title">Найдите ментора под свою цель</h1>
            <p class="workspace-subtitle">
              Эксперты с открытым набором, понятными форматами общения и навыками из профилей.
            </p>

            <div class="catalog-hero__search">
              <BaseInput
                v-model="searchForm.q"
                title=""
                start-icon="search"
                placeholder="Поиск по навыкам, стеку, компании или имени ментора..."
              />
            </div>

          </div>

          <aside class="catalog-hero__visual">
            <div class="catalog-hero__stat">
              <strong>{{ mentorsTotal }}</strong>
              <span>менторов найдено</span>
            </div>
            <div class="catalog-hero__stat catalog-hero__stat--accent">
              <strong>{{ openMentorsCount }}</strong>
              <span>открыты к заявкам</span>
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
              <p v-if="isFilterCollapsed" class="catalog-filter-panel__collapsed-summary">
                Найдено: {{ mentorsTotal }} · открыты: {{ openMentorsCount }}
              </p>
              <p v-else class="catalog-filter-panel__hint">
                Фильтры применяются сразу после выбора.
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
              v-model="searchForm.recruitmentStatus"
              class="catalog-filter-panel__select"
              size="s"
              title="Статус набора"
              placeholder="Любой статус"
              :options="recruitmentFilterOptions"
            />
            <BaseSelect
              v-model="searchForm.mentoringType"
              class="catalog-filter-panel__select"
              size="s"
              title="Цель менторства"
              placeholder="Любая цель"
              :options="mentoringTypeFilterOptions"
            />
            <BaseSelect
              v-model="searchForm.mentoringChannel"
              class="catalog-filter-panel__select"
              size="s"
              title="Формат связи"
              placeholder="Любой формат"
              :options="mentoringChannelFilterOptions"
            />

            <div class="catalog-filter-panel__summary">
              <span>{{ isLoading ? "Обновляем выдачу..." : `Найдено: ${mentorsTotal}` }}</span>
              <span>Открыты: {{ openMentorsCount }}</span>
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
              Загружаем менторов...
            </div>

            <div v-else-if="error" class="error-state mt-6">
              {{ error.message }}
            </div>

            <div v-else-if="displayedMentorCards.length" class="student-directory-grid student-directory-grid--rich mt-6">
              <article
                v-for="mentor in displayedMentorCards"
                :key="mentor.id"
                class="student-catalog-card"
              >
                <div class="student-catalog-card__main">
                  <div class="student-catalog-card__head">
                    <div class="avatar catalog-avatar">{{ mentor.firstName.slice(0, 1) }}{{ mentor.lastName.slice(0, 1) }}</div>
                    <div>
                      <h3>{{ mentor.firstName }} {{ mentor.lastName }}</h3>
                      <p>{{ mentor.position ?? "Позиция не указана" }}</p>
                      <span
                        :class="[
                          'status-pill',
                          mentor.recruitmentStatus === 'OPEN' ? 'status-pill--success' : 'status-pill--warning'
                        ]"
                      >
                        {{ getOptionLabel(recruitmentStatusOptions, mentor.recruitmentStatus) }}
                      </span>
                    </div>
                  </div>

                  <button
                    class="catalog-rating-row"
                    type="button"
                    @click="openReviewsModal(mentor.raw)"
                  >
                    <template v-if="getReviewState(mentor.id).isLoading">
                      <span class="catalog-rating-row__muted">Отзывы загружаются...</span>
                    </template>
                    <template v-else-if="getReviewState(mentor.id).totalReviews">
                      <span class="catalog-rating-row__stars" aria-hidden="true">
                        <span
                          v-for="star in 5"
                          :key="star"
                          class="catalog-rating-row__star"
                          :class="{ 'catalog-rating-row__star--active': star <= Math.round(getReviewState(mentor.id).averageRating) }"
                        >
                          <BaseIcon icon="star" :width="14" :height="14" />
                        </span>
                      </span>
                      <span>{{ getReviewState(mentor.id).averageRating.toFixed(1) }} · {{ getReviewState(mentor.id).totalReviews }} отзывов</span>
                    </template>
                    <template v-else>
                      <span class="catalog-rating-row__muted">Нет отзывов</span>
                    </template>
                  </button>

                  <p class="student-catalog-card__copy">
                    {{ mentor.raw.description ?? "Описание профиля не заполнено." }}
                  </p>

                  <div class="chip-list">
                    <span v-for="skill in mentor.skills.slice(0, 5)" :key="skill" class="chip">{{ skill }}</span>
                    <span v-if="!mentor.skills.length" class="chip">Навыки не указаны</span>
                  </div>
                </div>

                <aside class="student-catalog-card__aside">
                  <p>{{ mentor.city?.name ?? "Город не указан" }}{{ mentor.department ? " · " + mentor.department : "" }}</p>
                  <span>{{ getOptionLabel(mentoringTypeOptions, mentor.mentoringType) }}</span>
                  <span>{{ getOptionLabel(mentoringChannelOptions, mentor.mentoringChannel) }}</span>
                  <BaseButton
                    size="m"
                    :label="mentor.hasActiveRequest ? 'Заявка уже отправлена' : 'Связаться'"
                    :disabled="mentor.recruitmentStatus !== 'OPEN' || mentor.hasActiveRequest"
                    @click="openRequestModal(mentor.raw)"
                  />
                </aside>
              </article>
            </div>

            <div v-else class="empty-state mt-6">
              По текущим фильтрам менторы не найдены.
            </div>
          </main>
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

      <div v-if="isReviewsModalOpen" class="request-modal" @click.self="closeReviewsModal">
        <article class="app-panel request-draft-panel request-draft-panel--modal mentor-reviews-modal">
          <div class="request-draft-panel__header">
            <div>
              <p class="section-kicker">Отзывы</p>
              <h3 class="section-title">{{ selectedReviewsMentorName }}</h3>
            </div>
            <BaseButton variant="clear" size="s" label="Закрыть" @click="closeReviewsModal" />
          </div>

          <div class="mentor-reviews-modal__summary">
            <template v-if="selectedReviewState.totalReviews">
              <ReviewRating
                :value="selectedReviewState.averageRating"
                readonly
              />
              <span>{{ selectedReviewState.totalReviews }} отзывов</span>
            </template>
            <span v-else-if="selectedReviewState.isLoading" class="helper-text">Загружаем отзывы...</span>
            <span v-else class="helper-text">У этого ментора пока нет отзывов.</span>
          </div>

          <div v-if="selectedReviewState.error" class="error-state">
            {{ selectedReviewState.error }}
          </div>

          <div v-if="selectedReviewState.reviews.length" class="mentor-reviews-modal__list">
            <ReviewItem
              v-for="review in selectedReviewState.reviews"
              :key="review.id"
              :review="review"
              :mentor-name="selectedReviewsMentorName"
              :current-user-id="authStore.user?.id"
            />
          </div>
        </article>
      </div>

      <div v-if="successMessage" class="success-state">
        {{ successMessage }}
      </div>
    </template>
  </section>
</template>
