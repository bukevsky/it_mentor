<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseIcon, BaseInput } from "conductor";
import type {
  ActivityItemResponse,
  ChatResponse,
  DashboardSummaryResponse,
  ErrorResponse,
  MentorCardResponse,
  MentorProfileResponse,
  MentorStatsResponse,
  MentoringRequestResponse,
  ProfileSummaryResponse,
  StudentProfileResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useChatStore } from "@/features/chat/model/chat-store";
import { dashboardApi } from "@/features/dashboard/api/dashboard-api";
import { mentorProfileApi } from "@/features/mentor-profile/api/mentor-profile-api";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { profileSummaryApi } from "@/features/profile-summary/api/profile-summary-api";
import { studentProfileApi } from "@/features/student-profile/api/student-profile-api";
import { getPrimaryWorkspaceRole, hasAnyRole } from "@/shared/lib/access";
import {
  mentoringRequestStatusOptions,
  mentoringTypeOptions,
  recruitmentStatusOptions,
  roleOptions
} from "@/shared/lib/options";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { formatDateTime, fullName, getOptionLabel } from "@/shared/lib/presenters";

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();
const { isAuthenticated, user } = storeToRefs(authStore);
const { chats } = storeToRefs(chatStore);

const profileSummary = ref<ProfileSummaryResponse | null>(null);
const dashboardSummary = ref<DashboardSummaryResponse | null>(null);
const mentorStats = ref<MentorStatsResponse | null>(null);
const ownMentorProfile = ref<MentorProfileResponse | null>(null);
const ownStudentProfile = ref<StudentProfileResponse | null>(null);
const latestRequests = ref<MentoringRequestResponse[]>([]);
const dashboardActivity = ref<ActivityItemResponse[]>([]);
const recommendedMentors = ref<MentorCardResponse[]>([]);
const recommendedStudents = ref<StudentProfileResponse[]>([]);
const availableMentorsCount = ref(0);
const availableStudentsCount = ref(0);
const summaryError = ref<ErrorResponse | null>(null);
const isSummaryLoading = ref(false);
const searchQuery = ref("");

type DashboardMetric = {
  label: string;
  value: string;
  hint: string;
  tone: "blue" | "green" | "amber" | "violet";
  icon: string;
};

type DirectoryCard = {
  id: number;
  name: string;
  subtitle: string;
  meta: string;
  chips: string[];
  routeName: "mentors" | "students";
};

const resetDashboardData = () => {
  profileSummary.value = null;
  dashboardSummary.value = null;
  mentorStats.value = null;
  ownMentorProfile.value = null;
  ownStudentProfile.value = null;
  latestRequests.value = [];
  dashboardActivity.value = [];
  recommendedMentors.value = [];
  recommendedStudents.value = [];
  availableMentorsCount.value = 0;
  availableStudentsCount.value = 0;
  searchQuery.value = "";
};

const mentorProfileCompletion = computed(() => {
  const profile = ownMentorProfile.value;

  if (!profile) {
    return 0;
  }

  const fields = [
    profile.firstName,
    profile.lastName,
    profile.position,
    profile.department,
    profile.city?.name,
    profile.description,
    profile.canHelpWith,
    profile.mentoringType,
    profile.mentoringChannel,
    profile.mentoringDuration,
    profile.recruitmentStatus,
    profile.skills.length ? "skills" : ""
  ];

  const filled = fields.filter((field) => Boolean(String(field ?? "").trim())).length;
  return Math.round((filled / fields.length) * 100);
});

const profileCompletion = computed(() => {
  if (!profileSummary.value?.profileExists) {
    return 0;
  }

  if (workspaceRole.value === "MENTOR") {
    return mentorProfileCompletion.value;
  }

  return dashboardSummary.value?.profileCompletion ?? 0;
});

const workspaceRole = computed(() => getPrimaryWorkspaceRole(user.value));
const isStudent = computed(() => workspaceRole.value === "STUDENT");
const isMentor = computed(() => workspaceRole.value === "MENTOR");

const loadSummary = async () => {
  if (!isAuthenticated.value) {
    resetDashboardData();
    return;
  }

  isSummaryLoading.value = true;
  summaryError.value = null;

  try {
    profileSummary.value = await profileSummaryApi.getMySummary();

    if (!hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
      return;
    }

    const [summary, activity, requests] = await Promise.all([
      dashboardApi.getSummary(),
      dashboardApi.getActivity({ limit: 6 }),
      mentoringApi.getList({ page: 0, size: 5 }),
      chatStore.loadChats()
    ]);

    dashboardSummary.value = summary;
    dashboardActivity.value = activity;
    latestRequests.value = requests.content;

    if (isStudent.value) {
      const [mentors, studentProfile] = await Promise.all([
        mentorProfileApi.search({ recruitmentStatus: "OPEN", page: 0, size: 4, sort: "createdAt,desc" }),
        profileSummary.value.profileExists ? studentProfileApi.getMine() : Promise.resolve(null)
      ]);

      recommendedMentors.value = mentors.content;
      availableMentorsCount.value = mentors.totalElements;
      ownStudentProfile.value = studentProfile;
    }

    if (isMentor.value) {
      const [students, mentorProfile] = await Promise.all([
        studentProfileApi.search({ page: 0, size: 4, sort: "createdAt,desc" }),
        profileSummary.value.profileExists ? mentorProfileApi.getMine() : Promise.resolve(null)
      ]);

      recommendedStudents.value = students.content;
      availableStudentsCount.value = students.totalElements;
      ownMentorProfile.value = mentorProfile;

      if (profileSummary.value.profileExists) {
        mentorStats.value = await dashboardApi.getMentorStats();
      }
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

const currentRole = computed(() => {
  if (!user.value?.roles.length) {
    return "Гость";
  }

  return user.value.roles.map((role) => getOptionLabel(roleOptions, role)).join(", ");
});

const heroTitle = computed(() => {
  if (!isAuthenticated.value) {
    return "IT Mentor Platform";
  }

  if (isMentor.value) {
    return "Рабочий стол ментора";
  }

  if (isStudent.value) {
    return "Найдите ментора под свою цель";
  }

  return "Рабочее пространство";
});

const heroSubtitle = computed(() => {
  if (!isAuthenticated.value) {
    return "Войдите, чтобы открыть профиль, заявки, чаты и каталог.";
  }

  if (isMentor.value) {
    return "Заявки, студенты, сессии и сообщения собраны на одном экране.";
  }

  if (isStudent.value) {
    return "Менторы, заявки, ближайшая сессия и диалоги доступны без лишних переходов.";
  }

  return `Роль: ${currentRole.value}`;
});

const primaryAction = computed(() => {
  if (!isAuthenticated.value) {
    return { label: "Войти", routeName: "auth" as const, icon: "lock-alt" };
  }

  if (!profileSummary.value?.profileExists && hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    return { label: "Заполнить профиль", routeName: "profile" as const, icon: "calendar-user" };
  }

  if (isMentor.value) {
    return { label: "Открыть заявки", routeName: "requests" as const, icon: "calendar-exclamation" };
  }

  if (isStudent.value) {
    return { label: "Найти ментора", routeName: "mentors" as const, icon: "file-bookmark-alt" };
  }

  return { label: "Открыть админку", routeName: "admin" as const, icon: "rules" };
});

const profileSkills = computed(() => {
  const skills = isMentor.value
    ? ownMentorProfile.value?.skills
    : ownStudentProfile.value?.skills;

  return (skills ?? []).map((item) => item.skill.name).slice(0, 7);
});

const statusLine = computed(() => {
  if (!isAuthenticated.value) {
    return "Аккаунт не подключён";
  }

  if (!profileSummary.value?.profileExists && hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    return "Профиль ещё не заполнен";
  }

  return `Профиль заполнен на ${profileCompletion.value}%`;
});

const metrics = computed<DashboardMetric[]>(() => {
  if (!isAuthenticated.value || !hasAnyRole(user.value, ["STUDENT", "MENTOR"])) {
    return [
      { label: "Статус", value: currentRole.value, hint: "Доступ зависит от роли", tone: "blue", icon: "user" },
      { label: "Профиль", value: `${profileCompletion.value}%`, hint: statusLine.value, tone: "green", icon: "circle-check" },
      { label: "Аккаунт", value: user.value?.status ?? "Гость", hint: "Текущий статус", tone: "violet", icon: "lock-alt" }
    ];
  }

  const summary = dashboardSummary.value;

  if (isMentor.value) {
    const stats = mentorStats.value;
    return [
      {
        label: "Новые заявки",
        value: String(summary?.sentRequests ?? 0),
        hint: "Ожидают ответа",
        tone: "blue",
        icon: "circle-information"
      },
      {
        label: "В работе",
        value: String((summary?.pendingRequests ?? 0) + (summary?.acceptedRequests ?? 0)),
        hint: "Уточнение и принятые",
        tone: "amber",
        icon: "calendar-day"
      },
      {
        label: "Завершено",
        value: String(stats?.completedRequests ?? 0),
        hint: "Всего сессий",
        tone: "green",
        icon: "circle-check"
      },
      {
        label: "Отклик",
        value: stats ? `${stats.responseRate}%` : "—",
        hint: "Скорость ответа",
        tone: "violet",
        icon: "chart-bar"
      },
      {
        label: "Рейтинг",
        value: stats?.reviewCount ? String(stats.averageRating) : "—",
        hint: `Отзывов: ${stats?.reviewCount ?? 0}`,
        tone: "amber",
        icon: "star"
      }
    ];
  }

  return [
    {
      label: "Отправлено",
      value: String(summary?.sentRequests ?? 0),
      hint: "Статус отправки",
      tone: "blue",
      icon: "circle-information"
    },
    {
      label: "В работе",
      value: String((summary?.pendingRequests ?? 0) + (summary?.acceptedRequests ?? 0)),
      hint: "Уточнение и принятые",
      tone: "amber",
      icon: "calendar-day"
    },
    {
      label: "Чаты",
      value: String(totalChats.value),
      hint: `Непрочитанных: ${totalUnreadChats.value}`,
      tone: "violet",
      icon: "message-square-exclamation"
    },
    {
      label: "Менторы",
      value: String(availableMentorsCount.value),
      hint: "Открыты для заявок",
      tone: "green",
      icon: "users"
    }
  ];
});

const activeRequests = computed(() => {
  return latestRequests.value.filter((request) =>
    ["SENT", "REVIEWING", "NEEDS_CLARIFICATION", "ACCEPTED"].includes(request.status)
  );
});

const nextSession = computed(() => dashboardSummary.value?.nextSession ?? null);

const nextSessionMeta = computed(() => {
  const session = nextSession.value;

  if (!session) {
    return "Ближайшая сессия не запланирована";
  }

  const duration = session.durationMinutes ? ` · ${session.durationMinutes} мин` : "";
  return `${formatDateTime(session.scheduledAt)}${duration}`;
});

const latestChats = computed(() => {
  if (!isAuthenticated.value) {
    return [];
  }

  return [...(chats.value?.content ?? [])]
    .sort((left, right) => {
      const leftDate = left.lastMessageAt ?? left.lastMessage?.createdAt ?? left.createdAt;
      const rightDate = right.lastMessageAt ?? right.lastMessage?.createdAt ?? right.createdAt;

      return new Date(rightDate).getTime() - new Date(leftDate).getTime();
    })
    .slice(0, 5);
});

const unreadChats = computed(() => latestChats.value.filter((chat) => chat.unreadCount > 0));

const totalChats = computed(() => chats.value?.totalElements ?? dashboardSummary.value?.totalChats ?? 0);
const totalUnreadChats = computed(() =>
  (chats.value?.content ?? []).reduce((total, chat) => total + chat.unreadCount, 0)
);

const directoryCards = computed<DirectoryCard[]>(() => {
  if (isMentor.value) {
    return recommendedStudents.value.map((student) => ({
      id: student.id,
      name: `${student.firstName} ${student.lastName}`,
      subtitle: student.desiredPosition ?? "Позиция не указана",
      meta: student.city?.name ?? "Город не указан",
      chips: student.skills.map((skill) => skill.skill.name).slice(0, 3),
      routeName: "students"
    }));
  }

  return recommendedMentors.value.map((mentor) => ({
    id: mentor.id,
    name: `${mentor.firstName} ${mentor.lastName}`,
    subtitle: mentor.position ?? "Позиция не указана",
    meta: [
      mentor.department,
      getOptionLabel(recruitmentStatusOptions, mentor.recruitmentStatus)
    ].filter(Boolean).join(" · "),
    chips: mentor.skills.map((skill) => skill.skill.name).slice(0, 3),
    routeName: "mentors"
  }));
});

const counterpartName = (request: MentoringRequestResponse) => {
  return isMentor.value ? fullName(request.studentProfile) : fullName(request.mentorProfile);
};

const requestMeta = (request: MentoringRequestResponse) => {
  return [
    getOptionLabel(mentoringTypeOptions, request.goalType),
    getOptionLabel(mentoringRequestStatusOptions, request.status),
    formatDateTime(request.createdAt)
  ].join(" · ");
};

const chatTitle = (chat: ChatResponse) => {
  if (isMentor.value) {
    return chat.studentName ?? `Заявка #${chat.mentoringRequestId}`;
  }

  return chat.mentorName ?? `Заявка #${chat.mentoringRequestId}`;
};

const chatPreview = (chat: ChatResponse) => {
  return chat.lastMessage?.body
    ?? chat.lastMessage?.attachment?.originalFilename
    ?? `Заявка #${chat.mentoringRequestId}`;
};

const openSearch = () => {
  void router.push({
    name: isMentor.value ? "students" : "mentors",
    query: searchQuery.value.trim() ? { q: searchQuery.value.trim() } : undefined
  });
};

const openRequestChat = (request: MentoringRequestResponse) => {
  if (request.status !== "ACCEPTED") {
    void router.push({ name: "requests", query: { requestId: String(request.id) } });
    return;
  }

  const chat = (chats.value?.content ?? []).find(
    (c) => c.mentoringRequestId === request.id
  );
  void router.push({
    name: "chat",
    query: chat ? { chatId: String(chat.id) } : undefined
  });
};
</script>

<template>
  <section class="home-page">
    <section class="home-hero">
      <div class="home-hero__content">
        <p class="home-eyebrow">{{ currentRole }}</p>
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroSubtitle }}</p>

        <div v-if="isStudent || isMentor" class="home-search">
          <BaseInput
            v-model="searchQuery"
            :placeholder="isMentor ? 'Поиск по имени, позиции или навыку студента' : 'Поиск по навыкам, стеку или имени ментора'"
            title=""
            start-icon="search"
            @keydown.enter="openSearch"
          />
          <BaseButton
            class="home-search-btn"
            :label="isMentor ? 'Найти студента' : 'Найти'"
            @click="openSearch"
          />
        </div>
      </div>

      <aside v-if="profileCompletion < 100" class="home-feature-card">
        <p>{{ statusLine }}</p>
        <strong>{{ profileCompletion }}%</strong>
        <span>Профиль</span>
        <BaseButton
          variant="secondary"
          size="m"
          label="Открыть профиль"
          @click="router.push({ name: 'profile' })"
        />
      </aside>
    </section>

    <section class="home-metrics" aria-label="Сводка">
      <article v-for="metric in metrics" :key="metric.label" class="home-metric" :class="`home-metric--${metric.tone}`">
        <span class="home-metric__icon">
          <BaseIcon :icon="metric.icon" :width="22" :height="22" />
        </span>
        <div class="home-metric__body">
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.label }}</p>
          <small>{{ metric.hint }}</small>
        </div>
      </article>
    </section>

    <section v-if="activeRequests.length" class="home-panel">
      <div class="home-panel__header">
        <h2>{{ isMentor ? "Заявки студентов" : "Продолжить обучение" }}</h2>
        <BaseButton variant="clear" size="m" label="Все заявки" @click="router.push({ name: 'requests' })" />
      </div>

      <div class="home-request-row">
        <article v-for="request in activeRequests.slice(0, 3)" :key="request.id" class="home-request-card">
          <div class="home-avatar">{{ counterpartName(request).slice(0, 1) }}</div>
          <div>
            <h3>{{ counterpartName(request) }}</h3>
            <p>{{ requestMeta(request) }}</p>
          </div>
          <BaseButton variant="secondary" size="m" label="Открыть чат" @click="openRequestChat(request)" />
        </article>
      </div>
    </section>

    <div v-if="isSummaryLoading" class="home-loading">Загружаем данные...</div>
    <article v-if="summaryError" class="error-state">{{ summaryError.message }}</article>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 20px;
  padding: 0 32px 40px;
}

.home-hero,
.home-panel,
.home-side-card,
.home-directory-card,
.home-request-card {
  border: 1px solid rgba(91, 124, 220, 0.16);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 50px rgba(34, 64, 128, 0.08);
}

.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 250px;
  gap: 24px;
  min-height: 250px;
  padding: 32px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 76% 24%, rgba(50, 205, 184, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(244, 248, 255, 0.98), rgba(239, 244, 255, 0.94));
  overflow: hidden;
}

.home-eyebrow,
.home-muted,
.home-empty,
.home-loading {
  color: #66769a;
}

.home-eyebrow {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.home-hero h1 {
  max-width: 640px;
  margin: 0;
  color: #0d1b3e;
  font-size: 42px;
  line-height: 1.12;
}

.home-hero p {
  max-width: 720px;
  margin: 14px 0 0;
  color: #66769a;
  font-size: 16px;
  line-height: 1.5;
}

.home-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 12px;
  max-width: 680px;
  margin-top: 24px;
}

.home-chips,
.home-mini-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.home-chips {
  margin-top: 18px;
}

.home-chip,
.home-mini-chips span {
  border: 1px solid rgba(45, 98, 255, 0.18);
  border-radius: 999px;
  background: #eef4ff;
  color: #2356d8;
  font-size: 12px;
  font-weight: 600;
}

.home-chip {
  padding: 7px 12px;
}

.home-mini-chips span {
  padding: 5px 9px;
}

.home-feature-card {
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
}

.home-feature-card p,
.home-feature-card span {
  margin: 0;
  color: #66769a;
}

.home-feature-card strong {
  color: #245dff;
  font-size: 52px;
  line-height: 1;
}

.home-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.home-metric {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 16px;
  border: 0;
  color: #fff;
}

.home-metric--blue   { background: linear-gradient(135deg, #245dff, #4a80ff); }
.home-metric--amber  { background: linear-gradient(135deg, #f59e0b, #f97316); }
.home-metric--green  { background: linear-gradient(135deg, #00c07f, #04b800); }
.home-metric--violet { background: linear-gradient(135deg, #7c3aed, #a855f7); }

.home-metric__icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.home-metric__body {
  min-width: 0;
}

.home-metric p,
.home-metric small {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
}

.home-metric p {
  font-size: 13px;
  font-weight: 600;
}

.home-metric small {
  font-size: 11px;
}

.home-metric strong {
  display: block;
  margin: 0 0 2px;
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}

.home-panel,
.home-side-card {
  border-radius: 18px;
  padding: 20px;
}

.home-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.home-panel__header h2,
.home-side-card h2 {
  margin: 0;
  color: #0d1b3e;
  font-size: 17px;
}

.home-request-row,
.home-directory {
  display: grid;
  gap: 12px;
}

.home-request-row {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.home-directory {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.home-request-card,
.home-directory-card {
  border-radius: 14px;
  padding: 16px;
}

.home-request-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.home-request-card h3,
.home-directory-card h3 {
  margin: 0;
  color: #0d1b3e;
  font-size: 15px;
}

.home-request-card p,
.home-directory-card p,
.home-message p,
.home-session p {
  margin: 4px 0 0;
  color: #66769a;
  font-size: 13px;
  line-height: 1.35;
}

.home-request-card .btn {
  grid-column: 1 / -1;
}

.home-directory-card {
  display: grid;
  gap: 13px;
}

.home-directory-card__top {
  display: flex;
  gap: 12px;
  align-items: center;
}

.home-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #245dff, #31c8b7);
  color: #fff;
  font-weight: 800;
}

.home-avatar--small {
  width: 36px;
  height: 36px;
  font-size: 13px;
}

.home-activity {
  display: grid;
}

.home-activity__item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid rgba(91, 124, 220, 0.12);
}

.home-activity__item span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #245dff;
}

.home-activity__item p,
.home-activity__item time {
  margin: 0;
  color: #66769a;
  font-size: 13px;
}

.home-primary-action {
  min-height: 48px;
}

.home-session {
  display: grid;
  gap: 8px;
}

.home-session strong {
  color: #0d1b3e;
}

.home-message-list,
.home-stats-list {
  display: grid;
  gap: 12px;
}

.home-message {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.home-message strong {
  display: block;
  color: #0d1b3e;
  font-size: 14px;
}

.home-message span {
  display: grid;
  place-items: center;
  min-width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #245dff;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.home-stats-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(91, 124, 220, 0.12);
}

.home-stats-list span {
  color: #66769a;
}

.home-stats-list strong {
  color: #0d1b3e;
}

.home-loading {
  margin-top: 18px;
  text-align: center;
}

/* mobile-only messages: hidden on desktop */
.home-messages-mobile {
  display: none;
}

@media (max-width: 1180px) {
  .home-grid,
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-metrics,
  .home-directory {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 999px) {
  .home-hero {
    padding: 16px !important;
    min-height: 0 !important;
    grid-template-columns: 1fr !important;
  }

  .home-hero h1 {
    font-size: 22px !important;
    line-height: 1.2 !important;
  }

  .home-hero p {
    font-size: 13px !important;
    margin-top: 6px !important;
  }

  .home-eyebrow {
    font-size: 11px !important;
    margin-bottom: 6px !important;
  }

  .home-feature-card {
    display: none !important;
  }

  .home-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
    gap: 10px !important;
  }

  .home-metric {
    padding: 12px 14px !important;
    gap: 12px !important;
  }

  .home-metric__icon {
    width: 36px !important;
    height: 36px !important;
  }

  .home-metric strong {
    font-size: 20px !important;
  }

  .home-page {
    padding: 0 0 28px;
  }
}

@media (max-width: 760px) {
  .home-page {
    padding: 0 0 28px;
  }

  .home-search,
  .home-request-row {
    grid-template-columns: 1fr;
  }

  .home-panel__header {
    align-items: flex-start;
  }
}

</style>
