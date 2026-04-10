import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import AuthPage from "@/pages/AuthPage.vue";
import ChatPage from "@/pages/ChatPage.vue";
import FilesPage from "@/pages/FilesPage.vue";
import HomePage from "@/pages/HomePage.vue";
import MentorsPage from "@/pages/MentorsPage.vue";
import ProfilePage from "@/pages/ProfilePage.vue";
import RequestsPage from "@/pages/RequestsPage.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    name: "home",
    component: HomePage,
    meta: {
      title: "Главная",
      description: "Быстрые переходы по основным сценариям и персональные рекомендации по работе в системе."
    }
  },
  {
    path: "/auth",
    name: "auth",
    component: AuthPage,
    meta: {
      title: "Аккаунт",
      description: "Вход, регистрация и восстановление доступа."
    }
  },
  {
    path: "/profile",
    name: "profile",
    component: ProfilePage,
    meta: {
      title: "Мой профиль",
      description: "Личные данные, профиль студента и профиль ментора."
    }
  },
  {
    path: "/mentors",
    name: "mentors",
    component: MentorsPage,
    meta: {
      title: "Менторы",
      description: "Поиск, фильтры и отправка заявки выбранному ментору."
    }
  },
  {
    path: "/requests",
    name: "requests",
    component: RequestsPage,
    meta: {
      title: "Заявки",
      description: "Все заявки, их статусы и действия по обработке."
    }
  },
  {
    path: "/chat",
    name: "chat",
    component: ChatPage,
    meta: {
      title: "Чаты",
      description: "Переписка по заявкам и отправка сообщений."
    }
  },
  {
    path: "/files",
    name: "files",
    component: FilesPage,
    meta: {
      title: "Файлы",
      description: "Резюме, портфолио, аватар и вложения для чата."
    }
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});
