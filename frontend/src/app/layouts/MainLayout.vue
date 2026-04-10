<script setup lang="ts">
import { computed, ref } from "vue";
import { RouterView, useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, Toggle, useTheme } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { isAuthenticated } = storeToRefs(authStore);
const { theme, setTheme } = useTheme();
const isSidebarVisible = ref(true);

const navItems = computed(() => {
  const items = [
    { name: "home", label: "Обзор", icon: "house-smile" },
    { name: "auth", label: "Аккаунт", icon: "lock-alt" },
    { name: "profile", label: "Профиль", icon: "calendar-user" },
    { name: "mentors", label: "Менторы", icon: "users" },
    { name: "requests", label: "Заявки", icon: "table-list" },
    { name: "chat", label: "Чаты", icon: "message-square-exclamation" },
    { name: "files", label: "Файлы", icon: "file-bookmark-alt" }
  ] as const;

  return isAuthenticated.value
    ? items.filter((item) => item.name !== "auth")
    : items;
});

const pageTitle = computed(() => {
  return typeof route.meta.title === "string" ? route.meta.title : "IT Mentor";
});

const isDarkTheme = computed(() => theme.value === "default-dark");
const sidebarToggleLabel = computed(() =>
  isSidebarVisible.value ? "Скрыть меню" : "Показать меню"
);
const sidebarToggleIcon = computed(() =>
  isSidebarVisible.value ? "menu-hide" : "menu-show"
);

const toggleTheme = (nextState?: boolean) => {
  const shouldUseDark = typeof nextState === "boolean" ? nextState : !isDarkTheme.value;
  setTheme(shouldUseDark ? "default-dark" : "default-light");
};

const toggleSidebar = () => {
  isSidebarVisible.value = !isSidebarVisible.value;
};

const logout = () => {
  authStore.logout();
  void router.push({ name: "auth" });
};
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--dark': isDarkTheme }">
    <div
      class="app-shell__layout"
      :class="{ 'app-shell__layout--sidebar-hidden': !isSidebarVisible }"
    >
      <aside class="app-sidebar" :class="{ 'app-sidebar--hidden': !isSidebarVisible }">
        <p class="brand-kicker">IT Mentor</p>
        <h1 class="brand-title">Рабочее пространство наставничества</h1>

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

      <main class="app-main">
        <header class="app-main__header">
          <div class="app-main__heading">
            <h2 class="app-main__title">{{ pageTitle }}</h2>
            <BaseButton
              variant="secondary"
              size="m"
              :start-icon="sidebarToggleIcon"
              :label="sidebarToggleLabel"
              @click="toggleSidebar"
            />
          </div>
        </header>

        <RouterView />
      </main>
    </div>
  </div>
</template>
