<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton } from "conductor";
import type {
  ChatMessageResponse,
  ChatResponse,
  ErrorResponse,
  MentoringRequestResponse,
  ProfileSummaryResponse,
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { chatApi } from "@/features/chat/api/chat-api";
import { mentorProfileApi } from "@/features/mentor-profile/api/mentor-profile-api";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { profileSummaryApi } from "@/features/profile-summary/api/profile-summary-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { getPrimaryWorkspaceRole, hasAnyRole } from "@/shared/lib/access";
import { mentoringRequestStatusOptions, mentoringTypeOptions, roleOptions } from "@/shared/lib/options";
import { formatDateTime, fullName, getOptionLabel } from "@/shared/lib/presenters";

const router = useRouter();
const authStore = useAuthStore();
const { isAuthenticated, user } = storeToRefs(authStore);

const profileSummary = ref<ProfileSummaryResponse | null>(null);
const totalRequests = ref(0);
const activeRequests = ref(0);
const pendingRequests = ref(0);
const completedRequests = ref(0);
const chatsCount = ref(0);
const availableMentorsCount = ref(0);
const latestRequests = ref<MentoringRequestResponse[]>([]);
const latestChats = ref<Array<{ chat: ChatResponse; message: ChatMessageResponse | null }>>([]);
const summaryError = ref<ErrorResponse | null>(null);
const isSummaryLoading = ref(false);

type ActivityItem = {
  title: string;
  meta: string;
  time: string;
  sortAt: string;
};

type DashboardMetric = {
  label: string;
  value: string;
  icon: string;
  isDemo?: boolean;
};

type DashboardStat = {
  label: string;
  value: string;
  hint: string;
  isDemo?: boolean;
};

const resetDashboardData = () => {
  profileSummary.value = null;
  totalRequests.value = 0;
  activeRequests.value = 0;
  pendingRequests.value = 0;
  completedRequests.value = 0;
  chatsCount.value = 0;
  availableMentorsCount.value = 0;
  latestRequests.value = [];
  latestChats.value = [];
};

const loadLatestChats = async (chats: ChatResponse[]) => {
  latestChats.value = await Promise.all(
    chats.slice(0, 3).map(async (chat) => {
      try {
        const messages = await chatApi.getMessages(chat.id, { page: 0, size: 1 });
        return {
          chat,
          message: messages.content[0] ?? null
        };
      } catch {
        return {
          chat,
          message: null
        };
      }
    })
  );
};

const loadSummary = async () => {
  if (!isAuthenticated.value) {
    resetDashboardData();
    return;
  }

  isSummaryLoading.value = true;
  summaryError.value = null;

  try {
    profileSummary.value = await profileSummaryApi.getMySummary();

    const workspaceRole = getPrimaryWorkspaceRole(user.value);
    const canUseMentoringData = workspaceRole === "STUDENT" || workspaceRole === "MENTOR";

    if (canUseMentoringData) {
      const [
        allRequests,
        accepted,
        sent,
        reviewing,
        clarification,
        completed,
        chats
      ] = await Promise.all([
        mentoringApi.getList({ page: 0, size: 5 }),
        mentoringApi.getList({ status: "ACCEPTED", page: 0, size: 1 }),
        mentoringApi.getList({ status: "SENT", page: 0, size: 1 }),
        mentoringApi.getList({ status: "REVIEWING", page: 0, size: 1 }),
        mentoringApi.getList({ status: "NEEDS_CLARIFICATION", page: 0, size: 1 }),
        mentoringApi.getList({ status: "COMPLETED", page: 0, size: 1 }),
        chatApi.getChats({ page: 0, size: 3 })
      ]);

      latestRequests.value = allRequests.content;
      totalRequests.value = allRequests.totalElements;
      activeRequests.value = accepted.totalElements;
      pendingRequests.value = sent.totalElements + reviewing.totalElements + clarification.totalElements;
      completedRequests.value = completed.totalElements;
      chatsCount.value = chats.totalElements;

      await loadLatestChats(chats.content);
    }

    if (workspaceRole === "STUDENT") {
      const mentors = await mentorProfileApi.search({ page: 0, size: 1, sort: "createdAt,desc" });
      availableMentorsCount.value = mentors.totalElements;
    }
  } catch (rawError) {
    summaryError.value = normalizeErrorResponse(rawError, "/dashboard");
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

    resetDashboardData();
    summaryError.value = null;
  },
  { immediate: true }
);

const actionCards = computed(() => {
  if (!isAuthenticated.value) {
    return [
      {
        title: "Вход и регистрация",
        copy: "Войдите в аккаунт, чтобы открыть профиль, менторов и заявки.",
        actionLabel: "Перейти ко входу",
        routeName: "auth"
      }
    ];
  }

  const cards = [];

  if (hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    cards.push({
      title: isAuthenticated.value ? "Мой профиль" : "Вход и регистрация",
      copy: "Заполните данные студента или ментора и держите профиль в актуальном состоянии.",
      actionLabel: "Открыть профиль",
      routeName: "profile"
    });
  }

  if (hasAnyRole(user.value, ["STUDENT"])) {
    cards.push({
      title: "Менторы",
      copy: "Подберите наставника по навыкам, формату общения и текущему набору.",
      actionLabel: "Смотреть менторов",
      routeName: "mentors"
    });
  }

  if (hasAnyRole(user.value, ["MENTOR"])) {
    cards.push({
      title: "Студенты",
      copy: "Найдите подходящего кандидата и отправьте приглашение на менторство.",
      actionLabel: "Смотреть студентов",
      routeName: "students"
    });
  }

  if (hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    cards.push({
      title: "Заявки и диалоги",
      copy: "Следите за запросами, принимайте решения и продолжайте общение в чатах.",
      actionLabel: "Открыть заявки",
      routeName: "requests"
    });

    cards.push({
      title: "Файлы",
      copy: "Загружайте резюме, портфолио, аватар и вложения для общения с ментором.",
      actionLabel: "Перейти к файлам",
      routeName: "files"
    });
  }

  if (hasAnyRole(user.value, ["ADMIN"])) {
    cards.push({
      title: "Администрирование",
      copy: "Управляйте пользователями, ролями, жалобами и справочниками.",
      actionLabel: "Открыть админку",
      routeName: "admin"
    });

    cards.push({
      title: "Отзывы",
      copy: "Проверяйте обратную связь и модерационные сценарии.",
      actionLabel: "Открыть отзывы",
      routeName: "reviews"
    });
  }

  return cards;
});

const nextStep = computed(() => {
  if (!isAuthenticated.value) {
    return "Войдите в систему или зарегистрируйте новый аккаунт.";
  }

  const workspaceRole = getPrimaryWorkspaceRole(user.value);

  if (workspaceRole === "ADMIN") {
    return "Откройте админку, чтобы управлять пользователями и модерацией.";
  }

  if (!profileSummary.value?.profileExists) {
    return "Заполните профиль, чтобы открыть полный рабочий сценарий.";
  }

  if (workspaceRole === "MENTOR") {
    return "Перейдите во вкладку Студенты и начните работу по приглашениям.";
  }

  return "Перейдите во вкладку Менторы и начните работу по заявкам.";
});

const currentRole = computed(() => {
  if (!user.value?.roles.length) {
    return "Гость";
  }

  return user.value.roles.map((role) => getOptionLabel(roleOptions, role)).join(", ");
});

const dashboardSubtitle = computed(() => {
  if (!isAuthenticated.value) {
    return nextStep.value;
  }

  if (hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    return `Добро пожаловать, ${user.value?.email}. Чатов: ${chatsCount.value}, заявок: ${totalRequests.value}.`;
  }

  return `Добро пожаловать, ${user.value?.email}. Роль: ${currentRole.value}.`;
});

const primaryAction = computed(() => {
  if (!isAuthenticated.value) {
    return {
      label: "Войти",
      routeName: "auth",
      icon: "lock-alt"
    };
  }

  if (!profileSummary.value?.profileExists && hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    return {
      label: "Заполнить профиль",
      routeName: "profile",
      icon: "calendar-user"
    };
  }

  if (hasAnyRole(user.value, ["STUDENT"])) {
    return {
      label: "Найти ментора",
      routeName: "mentors",
      icon: "file-bookmark-alt"
    };
  }

  if (hasAnyRole(user.value, ["MENTOR"])) {
    return {
      label: "Открыть заявки",
      routeName: "requests",
      icon: "calendar-exclamation"
    };
  }

  return {
    label: "Открыть админку",
    routeName: "admin",
    icon: "rules"
  };
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

const metrics = computed<DashboardMetric[]>(() => {
  if (!isAuthenticated.value) {
    return [
      { label: "Статус", value: "Гость", icon: "●" },
      { label: "Профиль", value: "0%", icon: "!" },
      { label: "Заявки", value: "0", icon: "◆" }
    ];
  }

  if (hasAnyRole(user.value, ["ADMIN"])) {
    return [
      { label: "Статус", value: user.value?.status ?? "—", icon: "●" },
      { label: "Роли", value: String(user.value?.roles.length ?? 0), icon: "!" },
      { label: "Админка", value: "Доступ", icon: "◆" }
    ];
  }

  return [
    { label: "Активные заявки", value: String(activeRequests.value), icon: "●" },
    { label: "В ожидании", value: String(pendingRequests.value), icon: "!" },
    { label: "Чаты", value: String(chatsCount.value), icon: "◆" }
  ];
});

const dashboardInfoKicker = computed(() => {
  if (hasAnyRole(user.value, ["ADMIN"])) {
    return "Администрирование";
  }

  if (!profileSummary.value?.profileExists) {
    return "Профиль";
  }

  return hasAnyRole(user.value, ["MENTOR"]) ? "Заявки" : "Менторы";
});

const dashboardInfoTitle = computed(() => {
  if (hasAnyRole(user.value, ["ADMIN"])) {
    return "Управление платформой";
  }

  if (!profileSummary.value?.profileExists) {
    return "Заполните профиль";
  }

  if (hasAnyRole(user.value, ["MENTOR"])) {
    return `${activeRequests.value} активных заявок`;
  }

  return `${availableMentorsCount.value} доступных менторов`;
});

const dashboardInfoCopy = computed(() => {
  if (hasAnyRole(user.value, ["ADMIN"])) {
    return "Доступны модерация и управление ролями пользователей.";
  }

  if (!profileSummary.value?.profileExists) {
    return "Заполните профиль, чтобы система могла использовать подбор и заявки.";
  }

  if (hasAnyRole(user.value, ["MENTOR"])) {
    return `Активные заявки: ${activeRequests.value}. Чаты: ${chatsCount.value}.`;
  }

  return `Доступные менторы: ${availableMentorsCount.value}. Заявки в работе: ${activeRequests.value}.`;
});

const buildRequestActivity = (request: MentoringRequestResponse): ActivityItem => {
  const workspaceRole = getPrimaryWorkspaceRole(user.value);
  const counterpart = workspaceRole === "MENTOR"
    ? fullName(request.studentProfile)
    : fullName(request.mentorProfile);

  return {
    title: `${getOptionLabel(mentoringRequestStatusOptions, request.status)}: ${counterpart}`,
    meta: `${getOptionLabel(mentoringTypeOptions, request.goalType)} · заявка #${request.id}`,
    time: formatDateTime(request.createdAt),
    sortAt: request.createdAt
  };
};

const buildChatActivity = (chat: ChatResponse, message: ChatMessageResponse | null): ActivityItem => {
  const messagePreview = message?.body
    ?? message?.attachment?.originalFilename
    ?? "Сообщений пока нет";

  return {
    title: `Чат по заявке #${chat.mentoringRequestId}`,
    meta: messagePreview.length > 96 ? `${messagePreview.slice(0, 96)}...` : messagePreview,
    time: formatDateTime(message?.createdAt ?? chat.createdAt),
    sortAt: message?.createdAt ?? chat.createdAt
  };
};

const activityItems = computed<ActivityItem[]>(() => {
  const requestActivities = latestRequests.value.map(buildRequestActivity);
  const chatActivities = latestChats.value.map(({ chat, message }) => buildChatActivity(chat, message));

  return [...requestActivities, ...chatActivities]
    .sort((left, right) => Date.parse(right.sortAt) - Date.parse(left.sortAt))
    .slice(0, 5);
});

const secondaryStats = computed<DashboardStat[]>(() => {
  const stats: DashboardStat[] = [
    {
      label: "Всего заявок",
      value: String(totalRequests.value),
      hint: "Все ваши заявки"
    },
    {
      label: "Завершено",
      value: String(completedRequests.value),
      hint: "Закрытые менторские запросы"
    },
    {
      label: "Профиль",
      value: profileSummary.value?.profileExists ? "Готов" : "Не заполнен",
      hint: "Влияет на подбор и заявки"
    }
  ];

  if (hasAnyRole(user.value, ["STUDENT"])) {
    stats.push({
      label: "Доступные менторы",
      value: String(availableMentorsCount.value),
      hint: "Открыты для заявок"
    });
  }

  stats.push(
    { label: "Рейтинг", value: "4.9", hint: "Появится после отзывов", isDemo: true },
    { label: "Отклики", value: "92%", hint: "Появится после диалогов", isDemo: true }
  );

  return stats;
});
</script>

<template>
  <section class="app-section">
    <div class="workspace-header">
      <div>
        <h1 class="workspace-title">Рабочее пространство</h1>
        <p class="workspace-subtitle">
          {{ dashboardSubtitle }}
        </p>
      </div>
      <BaseButton
        size="l"
        :start-icon="primaryAction.icon"
        :label="primaryAction.label"
        @click="router.push({ name: primaryAction.routeName })"
      />
    </div>

    <div class="dashboard-metrics">
      <article v-for="metric in metrics" :key="metric.label" class="stat-card metric-card">
        <div class="metric-card__icon">{{ metric.icon }}</div>
        <div>
          <p class="metric-card__label title-with-badge">
            {{ metric.label }}
            <span v-if="metric.isDemo" class="demo-badge">Демо</span>
          </p>
          <p class="metric-card__value">{{ metric.value }}</p>
        </div>
      </article>
    </div>

    <div class="workspace-grid">
      <div class="workspace-stack">
        <article class="workspace-card">
          <p class="section-kicker">Quick actions</p>
          <div class="workspace-stack mt-4">
            <button
              v-for="card in actionCards.slice(0, 3)"
              :key="card.title"
              class="quick-action"
              type="button"
              @click="router.push({ name: card.routeName })"
            >
              <span class="metric-card__icon">{{ card.title.slice(0, 1) }}</span>
              {{ card.actionLabel }}
            </button>
          </div>
        </article>

        <article class="workspace-card auth-card--blue">
          <p class="section-kicker">{{ dashboardInfoKicker }}</p>
          <h3 class="section-title">{{ dashboardInfoTitle }}</h3>
          <p class="section-copy mt-4">
            {{ dashboardInfoCopy }}
          </p>
        </article>
      </div>

      <article class="table-card">
        <div class="table-card__header">
          <h3 class="section-title title-with-badge">
            Последняя активность
          </h3>
          <BaseButton variant="clear" size="m" label="Показать все" />
        </div>
        <div v-if="isSummaryLoading" class="empty-state">
          Загружаем активность...
        </div>
        <div v-else-if="activityItems.length" class="activity-list">
          <div v-for="item in activityItems" :key="item.title" class="activity-item">
            <div class="workspace-header">
              <strong>{{ item.title }}</strong>
              <span class="helper-text">{{ item.time }}</span>
            </div>
            <span class="helper-text">{{ item.meta }}</span>
          </div>
        </div>
        <div v-else class="empty-state">
          Активности пока нет.
        </div>
      </article>
    </div>

    <div class="quick-links-grid">
      <article v-for="stat in secondaryStats" :key="stat.label" class="stat-card">
        <p class="stat-card__label title-with-badge">
          {{ stat.label }}
          <span v-if="stat.isDemo" class="demo-badge">Демо</span>
        </p>
        <p class="stat-card__value">{{ stat.value }}</p>
        <p class="stat-card__hint">{{ stat.hint }}</p>
      </article>
    </div>

    <article v-if="summaryError" class="error-state">
      {{ summaryError.message }}
    </article>
  </section>
</template>
