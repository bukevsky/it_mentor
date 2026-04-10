<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton } from "conductor";
import type { ErrorResponse, ProfileSummaryResponse } from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { profileSummaryApi } from "@/features/profile-summary/api/profile-summary-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { roleOptions } from "@/shared/lib/options";
import { getOptionLabel } from "@/shared/lib/presenters";

const router = useRouter();
const authStore = useAuthStore();
const { isAuthenticated, user } = storeToRefs(authStore);

const profileSummary = ref<ProfileSummaryResponse | null>(null);
const summaryError = ref<ErrorResponse | null>(null);
const isSummaryLoading = ref(false);

const loadSummary = async () => {
  if (!isAuthenticated.value) {
    profileSummary.value = null;
    return;
  }

  isSummaryLoading.value = true;
  summaryError.value = null;

  try {
    profileSummary.value = await profileSummaryApi.getMySummary();
  } catch (rawError) {
    summaryError.value = normalizeErrorResponse(rawError, "/profile/me");
  } finally {
    isSummaryLoading.value = false;
  }
};

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (nextValue) {
      void loadSummary();
      return;
    }

    profileSummary.value = null;
    summaryError.value = null;
  },
  { immediate: true }
);

const actionCards = computed(() => {
  return [
    {
      title: isAuthenticated.value ? "Мой профиль" : "Вход и регистрация",
      copy: isAuthenticated.value
        ? "Заполните данные студента или ментора и держите профиль в актуальном состоянии."
        : "Войдите в аккаунт, чтобы открыть профиль, каталог менторов и заявки.",
      actionLabel: isAuthenticated.value ? "Открыть профиль" : "Перейти ко входу",
      routeName: isAuthenticated.value ? "profile" : "auth"
    },
    {
      title: "Каталог менторов",
      copy: "Подберите наставника по навыкам, формату общения и текущему набору.",
      actionLabel: "Смотреть каталог",
      routeName: "mentors"
    },
    {
      title: "Заявки и диалоги",
      copy: "Следите за запросами, принимайте решения и продолжайте общение в чатах.",
      actionLabel: "Открыть заявки",
      routeName: "requests"
    },
    {
      title: "Файлы",
      copy: "Загружайте резюме, портфолио, аватар и вложения для общения с ментором.",
      actionLabel: "Перейти к файлам",
      routeName: "files"
    }
  ];
});

const nextStep = computed(() => {
  if (!isAuthenticated.value) {
    return "Войдите в систему или зарегистрируйте новый аккаунт.";
  }

  if (!profileSummary.value?.profileExists) {
    return "Заполните профиль, чтобы открыть полный рабочий сценарий.";
  }

  return "Перейдите в каталог менторов и начните работу по заявкам.";
});

const currentRole = computed(() => {
  if (!user.value?.roles.length) {
    return "Гость";
  }

  return user.value.roles.map((role) => getOptionLabel(roleOptions, role)).join(", ");
});

const summaryRows = computed(() => {
  return [
    {
      label: "Статус",
      value: isAuthenticated.value ? "Аккаунт активен" : "Нужен вход"
    },
    {
      label: "Роль",
      value: currentRole.value
    },
    {
      label: "Профиль",
      value: profileSummary.value?.profileExists ? "Заполнен" : "Не заполнен"
    },
    {
      label: "Следующий шаг",
      value: nextStep.value
    }
  ];
});
</script>

<template>
  <section class="app-section">
    <div class="hero-grid">
      <article class="app-panel app-panel--hero home-hero-card">
        <p class="section-kicker">Обзор</p>
        <h3 class="section-title">Все основные сценарии собраны в одном рабочем интерфейсе</h3>
        <div class="hero-actions">
          <BaseButton
            size="l"
            :label="isAuthenticated ? 'Открыть профиль' : 'Войти в аккаунт'"
            @click="router.push({ name: isAuthenticated ? 'profile' : 'auth' })"
          />
          <BaseButton
            variant="secondary"
            size="l"
            label="Каталог менторов"
            @click="router.push({ name: 'mentors' })"
          />
          <BaseButton
            variant="secondary"
            size="l"
            label="Заявки"
            @click="router.push({ name: 'requests' })"
          />
        </div>
      </article>

      <article class="app-panel dashboard-rail">
        <p class="section-kicker">Состояние</p>
        <h3 class="section-title">Текущая сводка</h3>
        <ul class="clean-list profile-summary-list">
          <li v-for="row in summaryRows" :key="row.label">
            <span>{{ row.label }}</span>
            <strong>{{ row.value }}</strong>
          </li>
        </ul>
      </article>
    </div>

    <div class="quick-links-grid">
      <article v-for="card in actionCards" :key="card.title" class="stat-card quick-link-card">
        <div class="quick-link-card__content">
          <p class="stat-card__label">{{ card.title }}</p>
          <p class="stat-card__hint">{{ card.copy }}</p>
        </div>
        <BaseButton
          block
          variant="secondary"
          size="l"
          :label="card.actionLabel"
          @click="router.push({ name: card.routeName })"
        />
      </article>
    </div>
  </section>
</template>
