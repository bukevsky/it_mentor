<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { RouterView, useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, Toggle, useTheme } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import type { AccessRole } from "@/shared/lib/access";
import { hasAnyRole } from "@/shared/lib/access";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { isAuthenticated, user } = storeToRefs(authStore);
const { theme, setTheme } = useTheme();
const SIDEBAR_STORAGE_KEY = "it-mentor.sidebar-visible";
const getInitialSidebarState = () => {
  if (typeof window === "undefined") {
    return true;
  }

  return window.localStorage.getItem(SIDEBAR_STORAGE_KEY) !== "false";
};
const isSidebarVisible = ref(getInitialSidebarState());

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
    { name: "home", label: "Dashboard", icon: "diagram-cells" },
    { name: "auth", label: "Аккаунт", icon: "lock-alt", guestOnly: true },
    { name: "mentors", label: "Менторы", icon: "file-bookmark-alt", requiresAuth: true, roles: ["STUDENT"] },
    { name: "students", label: "Студенты", icon: "users", requiresAuth: true, roles: ["MENTOR"] },
    { name: "requests", label: "Заявки", icon: "calendar-exclamation", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "chat", label: "Чат", icon: "message-square-exclamation", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "files", label: "Файлы", icon: "file-arrow-down", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "profile", label: "Профиль", icon: "calendar-user", requiresAuth: true, roles: ["STUDENT", "MENTOR"] },
    { name: "reviews", label: "Отзывы", icon: "star", requiresAuth: true, roles: ["STUDENT", "MENTOR", "ADMIN"] },
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

const pageTitle = computed(() => {
  return typeof route.meta.title === "string" ? route.meta.title : "IT Mentor";
});

const isChatRoute = computed(() => route.name === "chat");
const isDarkTheme = computed(() => theme.value === "default-dark");
const sidebarToggleLabel = computed(() =>
  isSidebarVisible.value ? "Скрыть меню" : "Показать меню"
);
const sidebarToggleIcon = computed(() =>
  isSidebarVisible.value ? "menu-hide" : "menu-show"
);
const searchQuery = ref("");
const canUseMentoringWorkspace = computed(() => hasAnyRole(user.value, ["STUDENT", "MENTOR"]));
const sidebarPrimaryAction = computed(() => {
  return isAuthenticated.value
    ? { label: "Открыть заявки", icon: "calendar-exclamation", routeName: "requests" }
    : { label: "Войти", icon: "lock-alt", routeName: "auth" };
});

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

const logout = () => {
  authStore.logout();
  void router.push({ name: "auth" });
};
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--dark': isDarkTheme, 'app-shell--chat': isChatRoute }">
    <div
      class="app-shell__layout"
      :class="{ 'app-shell__layout--sidebar-hidden': !isSidebarVisible }"
    >
      <aside class="app-sidebar" :class="{ 'app-sidebar--hidden': !isSidebarVisible }">
        <div class="app-sidebar__brand">
          <p class="brand-kicker">Enterprise IT</p>
          <h1 class="brand-title">Mentor Portal</h1>
        </div>

        <nav class="sidebar-nav" aria-label="Основная навигация">
          <BaseButton
            v-for="item in navItems"
            :key="item.name"
            block
            variant="secondary"
            size="l"
            :start-icon="item.icon"
            icon-color="typo-default"
            content-align="start"
            :active="route.name === item.name"
            :label="item.label"
            @click="router.push({ name: item.name })"
          >
            {{ item.label }}
          </BaseButton>
        </nav>

        <div class="sidebar-toolbar">
          <BaseButton
            v-if="!isAuthenticated || canUseMentoringWorkspace"
            block
            size="l"
            :start-icon="sidebarPrimaryAction.icon"
            :label="sidebarPrimaryAction.label"
            @click="router.push({ name: sidebarPrimaryAction.routeName })"
          />
          <div class="sidebar-toggle">
            <Toggle
              label="Тёмная тема"
              :is-checked="isDarkTheme"
              @change_toggle="toggleTheme"
            />
          </div>
          <BaseButton
            v-if="isAuthenticated"
            block
            variant="secondary"
            size="m"
            label="Сменить аккаунт"
            @click="logout"
          />
        </div>
      </aside>

      <main class="app-main" :class="{ 'app-main--chat': isChatRoute }">
        <header v-if="!isChatRoute" class="app-topbar">
          <div class="app-topbar__brand">IT Mentor Platform</div>
          <BaseInput
            v-model="searchQuery"
            class="app-topbar__search"
            placeholder="Поиск по имени, заявке или навыку..."
            title=""
            start-icon="search"
          />
          <div class="app-topbar__actions">
            <span class="topbar-icon" aria-label="Уведомления">•</span>
            <BaseButton
              variant="secondary"
              size="m"
              :start-icon="sidebarToggleIcon"
              :label="sidebarToggleLabel"
              @click="toggleSidebar"
            />
          </div>
        </header>

        <header v-if="!isChatRoute" class="app-main__header">
          <div class="app-main__heading">
            <h2 class="app-main__title">{{ pageTitle }}</h2>
          </div>
        </header>

        <RouterView
          :sidebar-toggle-label="sidebarToggleLabel"
          :sidebar-toggle-icon="sidebarToggleIcon"
          :is-sidebar-visible="isSidebarVisible"
          @toggle-sidebar="toggleSidebar"
        />
      </main>
    </div>
  </div>
</template>
