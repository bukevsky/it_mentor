<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterView, useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseIcon, useTheme, useBreakpoints } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useChatStore } from "@/features/chat/model/chat-store";
import { useChatSocket } from "@/features/chat/model/use-chat-socket";
import { useRequestsStore } from "@/features/mentoring/model/requests-store";
import { getRequestStatusMeta } from "@/features/mentoring/model/request-triage";
import type { ChatResponse, MentoringRequestResponse, MentoringRequestStatus } from "@/shared/api/contracts";
import type { AccessRole } from "@/shared/lib/access";
import { hasAnyRole } from "@/shared/lib/access";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();
const requestsStore = useRequestsStore();
const chatSocket = useChatSocket();
const { isAuthenticated, user } = storeToRefs(authStore);
const { theme, setTheme } = useTheme();
const { isLaptop } = useBreakpoints();
const isMobile = computed(() => !isLaptop.value);
const SIDEBAR_STORAGE_KEY = "it-mentor.sidebar-visible";
const getInitialSidebarState = () => {
  if (typeof window === "undefined") {
    return true;
  }

  return window.localStorage.getItem(SIDEBAR_STORAGE_KEY) !== "false";
};
const isSidebarVisible = ref(getInitialSidebarState());
const { chats } = storeToRefs(chatStore);
const { requests } = storeToRefs(requestsStore);

const isNotifOpen = ref(false);
const notifBtnRef = ref<HTMLElement | null>(null);
const notifPanelStyle = ref<Record<string, string>>({});

type NotificationItem = {
  id: string;
  type: "message" | "request";
  title: string;
  body: string;
  chatId?: number;
  requestId?: number;
};

const chatCounterpartName = (chat: ChatResponse): string => {
  if (!user.value) return "Чат";
  const isMentorSide = user.value.id === chat.mentorUserId;
  const name = isMentorSide ? chat.studentName : chat.mentorName;
  return name ?? (chat.mentoringRequestId ? `Заявка #${chat.mentoringRequestId}` : "Чат");
};

const unreadChats = computed(() => (chats.value?.content ?? []).filter((c) => c.unreadCount > 0));

const isRequestRecipient = (request: MentoringRequestResponse) => {
  const roles = user.value?.roles ?? [];
  return (
    (request.direction === "STUDENT_TO_MENTOR" && roles.includes("MENTOR")) ||
    (request.direction === "MENTOR_TO_STUDENT" && roles.includes("STUDENT"))
  );
};

const isRequestInitiator = (request: MentoringRequestResponse) => {
  const roles = user.value?.roles ?? [];
  return (
    (request.direction === "STUDENT_TO_MENTOR" && roles.includes("STUDENT")) ||
    (request.direction === "MENTOR_TO_STUDENT" && roles.includes("MENTOR"))
  );
};

const requestPersonName = (request: MentoringRequestResponse, side: "student" | "mentor") => {
  const profile = side === "student" ? request.studentProfile : request.mentorProfile;
  return `${profile.firstName} ${profile.lastName}`.trim();
};

const requestCounterpartName = (request: MentoringRequestResponse) => {
  if (isRequestRecipient(request)) {
    return request.direction === "STUDENT_TO_MENTOR"
      ? requestPersonName(request, "student")
      : requestPersonName(request, "mentor");
  }

  return request.direction === "STUDENT_TO_MENTOR"
    ? requestPersonName(request, "mentor")
    : requestPersonName(request, "student");
};

const requestStatusBody = (status: MentoringRequestStatus, counterpart: string) => {
  const statusLabel = getRequestStatusMeta(status).label.toLowerCase();
  return `${counterpart}: статус заявки теперь "${statusLabel}"`;
};

const shouldShowRequestNotification = (request: MentoringRequestResponse) => {
  const recipient = isRequestRecipient(request);
  const initiator = isRequestInitiator(request);

  if (recipient && ["SENT", "CANCELLED"].includes(request.status)) {
    return true;
  }

  if (initiator && ["REVIEWING", "NEEDS_CLARIFICATION", "ACCEPTED", "REJECTED", "COMPLETED"].includes(request.status)) {
    return true;
  }

  return false;
};

const messageNotifications = computed<NotificationItem[]>(() =>
  unreadChats.value.map((chat) => ({
    id: `message-${chat.id}`,
    type: "message" as const,
    title: chatCounterpartName(chat),
    body: `${chat.unreadCount} новых сообщени${chat.unreadCount === 1 ? "е" : "й"}`,
    chatId: chat.id
  }))
);

const requestNotifications = computed<NotificationItem[]>(() =>
  requests.value
    .filter(shouldShowRequestNotification)
    .slice(0, 6)
    .map((request) => {
      const counterpart = requestCounterpartName(request);
      const title = isRequestRecipient(request) && request.status === "SENT"
        ? `Новая заявка #${request.id}`
        : `Заявка #${request.id}`;

      return {
        id: `request-${request.id}`,
        type: "request" as const,
        title,
        body: requestStatusBody(request.status, counterpart),
        requestId: request.id
      };
    })
);

const notifications = computed<NotificationItem[]>(() => [
  ...messageNotifications.value,
  ...requestNotifications.value
]);

const notifCount = computed(() =>
  unreadChats.value.reduce((sum, c) => sum + c.unreadCount, 0) + requestNotifications.value.length
);

const toggleNotif = () => {
  isNotifOpen.value = !isNotifOpen.value;
  if (isNotifOpen.value && notifBtnRef.value) {
    const rect = notifBtnRef.value.getBoundingClientRect();
    notifPanelStyle.value = {
      top: `${rect.top}px`,
      left: `${rect.right + 8}px`,
    };
  }
};

const closeNotif = () => { isNotifOpen.value = false; };

const openNotification = (notification: NotificationItem) => {
  if (notification.type === "message" && notification.chatId) {
    chatStore.markChatRead(notification.chatId);
    void chatStore.loadMessages(notification.chatId);
    void router.push({ name: "chat", query: { chatId: String(notification.chatId) } });
    closeNotif();
    return;
  }

  if (notification.type === "request" && notification.requestId) {
    void router.push({ name: "requests", query: { requestId: String(notification.requestId) } });
    closeNotif();
  }
};

const onDocClick = (e: MouseEvent) => {
  if (!isNotifOpen.value) return;
  const target = e.target as Node;
  const panel = document.getElementById("notif-panel");
  if (notifBtnRef.value?.contains(target) || panel?.contains(target)) return;
  closeNotif();
};

onMounted(() => document.addEventListener("click", onDocClick, true));
onBeforeUnmount(() => document.removeEventListener("click", onDocClick, true));

type NavItem = {
  name: string;
  label: string;
  icon: string;
  requiresAuth?: boolean;
  roles?: readonly AccessRole[];
  guestOnly?: boolean;
};

const navItems = computed(() => {
  const items: NavItem[] = [
    { name: "home", label: "Dashboard", icon: "chart-tree-map" },
    { name: "auth", label: "Аккаунт", icon: "lock-alt", guestOnly: true },
    { name: "mentors", label: "Менторы", icon: "users", requiresAuth: true, roles: ["STUDENT"] },
    { name: "students", label: "Студенты", icon: "users", requiresAuth: true, roles: ["MENTOR"] },
    { name: "requests", label: "Заявки", icon: "list-check", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "chat", label: "Чат", icon: "message-square-exclamation", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "files", label: "Файлы", icon: "file-arrow-down", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "profile", label: "Профиль", icon: "user", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "reviews", label: "Отзывы", icon: "star", requiresAuth: true, roles: ["MENTOR", "ADMIN"] },
    { name: "admin", label: "Админка", icon: "rules", requiresAuth: true, roles: ["ADMIN"] }
  ];

  return items.filter((item) => {
    if (item.guestOnly) {
      return !isAuthenticated.value;
    }

    if (item.requiresAuth && !isAuthenticated.value) {
      return false;
    }

    return hasAnyRole(user.value, item.roles);
  });
});

const isChatRoute = computed(() => route.name === "chat");
const isDarkTheme = computed(() => theme.value === "default-dark");
const sidebarToggleLabel = computed(() =>
  isSidebarVisible.value ? "Скрыть меню" : "Показать меню"
);
const sidebarToggleIcon = computed(() =>
  isSidebarVisible.value ? "menu-hide" : "menu-show"
);
const canUseMentoringWorkspace = computed(() => hasAnyRole(user.value, ["STUDENT", "MENTOR"]));

const toggleTheme = (nextState?: boolean) => {
  const shouldUseDark = typeof nextState === "boolean" ? nextState : !isDarkTheme.value;
  setTheme(shouldUseDark ? "default-dark" : "default-light");
};

const toggleSidebar = () => {
  isSidebarVisible.value = !isSidebarVisible.value;
};

watch(
  () => isSidebarVisible.value,
  (nextValue) => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(SIDEBAR_STORAGE_KEY, String(nextValue));
    }
  }
);

watch(
  () => [isAuthenticated.value, canUseMentoringWorkspace.value] as const,
  ([isLoggedIn, canUseChat]) => {
    if (isLoggedIn && canUseChat) {
      void chatSocket.connect();
      void chatStore.loadChats();
      void requestsStore.loadRequests({ selectFirst: false });
      return;
    }

    chatSocket.disconnect();
  },
  { immediate: true }
);

const logout = () => {
  chatSocket.disconnect();
  chatStore.reset();
  authStore.logout();
  void router.push({ name: "auth" });
};
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--dark': isDarkTheme, 'app-shell--chat': isChatRoute }">
    <div
      class="app-shell__layout"
      :class="{
        'app-shell__layout--sidebar-hidden': !isSidebarVisible || isMobile,
        'app-shell__layout--mobile': isMobile
      }"
    >
      <aside
        v-if="!isMobile"
        class="app-sidebar"
        :class="{ 'app-sidebar--hidden': !isSidebarVisible }"
      >
        <div class="app-sidebar__brand">
          <h1 class="brand-title">IT Mentor</h1>
          <div class="brand-actions">
            <button
              type="button"
              class="theme-btn"
              :class="{ 'theme-btn--active': isDarkTheme }"
              :aria-label="isDarkTheme ? 'Светлая тема' : 'Тёмная тема'"
              @click="toggleTheme()"
            >
              <BaseIcon :icon="isDarkTheme ? 'moon' : 'sun'" :width="20" :height="20" />
            </button>
            <button
              ref="notifBtnRef"
              type="button"
              class="notif-btn"
              :class="{ 'notif-btn--active': isNotifOpen }"
              :aria-label="`Уведомления${notifCount ? `: ${notifCount}` : ''}`"
              @click="toggleNotif"
            >
              <BaseIcon icon="bell" :width="20" :height="20" />
              <span v-if="notifCount" class="notif-badge">{{ notifCount > 9 ? "9+" : notifCount }}</span>
            </button>
          </div>
        </div>

        <nav class="sidebar-nav" aria-label="Основная навигация">
          <BaseButton
            v-for="item in navItems"
            :key="item.name"
            block
            variant="secondary"
            size="l"
            :start-icon="item.icon"
            content-align="start"
            :active="route.name === item.name"
            :label="item.label"
            @click="router.push({ name: item.name })"
          >
            {{ item.label }}
          </BaseButton>
        </nav>

      </aside>

      <main class="app-main" :class="{ 'app-main--chat': isChatRoute }">
        <RouterView
          :sidebar-toggle-label="sidebarToggleLabel"
          :sidebar-toggle-icon="sidebarToggleIcon"
          :is-sidebar-visible="isSidebarVisible"
          :is-mobile="isMobile"
          @toggle-sidebar="toggleSidebar"
        />
      </main>
    </div>

    <!-- Notification panel -->
    <Teleport to="body">
      <div
        v-if="isNotifOpen"
        id="notif-panel"
        class="notif-panel"
        :class="{ 'notif-panel--dark': isDarkTheme }"
        :style="notifPanelStyle"
      >
        <div class="notif-panel__head">
          <strong>Уведомления</strong>
          <span v-if="notifCount" class="notif-panel__count">{{ notifCount }}</span>
        </div>
        <div v-if="notifications.length" class="notif-panel__list">
          <button
            v-for="n in notifications"
            :key="n.id"
            type="button"
            class="notif-item"
            @click="openNotification(n)"
          >
            <span class="notif-item__icon">
              <svg v-if="n.type === 'message'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
            </span>
            <span class="notif-item__text">
              <strong>{{ n.title }}</strong>
              <span>{{ n.body }}</span>
            </span>
          </button>
        </div>
        <p v-else class="notif-panel__empty">Нет новых уведомлений</p>
      </div>
    </Teleport>

    <!-- Mobile bottom navigation -->
    <nav v-if="isMobile" class="app-bottom-nav" aria-label="Мобильная навигация">
      <button
        v-for="item in navItems"
        :key="item.name"
        class="app-bottom-nav__item"
        :class="{ 'app-bottom-nav__item--active': route.name === item.name }"
        @click="router.push({ name: item.name })"
      >
        <span class="app-bottom-nav__icon">
          <svg v-if="item.icon === 'diagram-cells'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          <svg v-else-if="item.icon === 'lock-alt'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>
          <svg v-else-if="item.icon === 'file-bookmark-alt'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="9" y1="13" x2="15" y2="13"/><line x1="9" y1="17" x2="11" y2="17"/></svg>
          <svg v-else-if="item.icon === 'users'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <svg v-else-if="item.icon === 'calendar-exclamation'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="12" y1="14" x2="12" y2="16"/><circle cx="12" cy="18" r="0.5" fill="currentColor"/></svg>
          <svg v-else-if="item.icon === 'message-square-exclamation'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="12" y1="8" x2="12" y2="12"/><circle cx="12" cy="15" r="0.5" fill="currentColor"/></svg>
          <svg v-else-if="item.icon === 'file-arrow-down'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><polyline points="12 12 12 18 9 15"/><polyline points="12 18 15 15"/></svg>
          <svg v-else-if="item.icon === 'calendar-user'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><circle cx="12" cy="16" r="3"/></svg>
          <svg v-else-if="item.icon === 'star'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          <svg v-else-if="item.icon === 'rules'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>
        </span>
        <span class="app-bottom-nav__label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>
