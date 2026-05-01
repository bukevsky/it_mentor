import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/features/auth/model/auth-store";
import AuthPage from "@/pages/AuthPage.vue";
import ChatPage from "@/pages/ChatPage.vue";
import FilesPage from "@/pages/FilesPage.vue";
import HomePage from "@/pages/HomePage.vue";
import MentorsPage from "@/pages/MentorsPage.vue";
import ProfilePage from "@/pages/ProfilePage.vue";
import RequestsPage from "@/pages/RequestsPage.vue";
import StudentsPage from "@/pages/StudentsPage.vue";
import ReviewsPage from "@/pages/ReviewsPage.vue";
import AdminPage from "@/pages/AdminPage.vue";
import type { AccessRole } from "@/shared/lib/access";
import { hasAnyRole } from "@/shared/lib/access";

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
      requiresAuth: true,
      roles: ["STUDENT", "MENTOR"],
      title: "Мой профиль",
      description: "Личные данные, профиль студента и профиль ментора."
    }
  },
  {
    path: "/mentors",
    name: "mentors",
    component: MentorsPage,
    meta: {
      requiresAuth: true,
      roles: ["STUDENT"],
      title: "Менторы",
      description: "Поиск, фильтры и отправка заявки выбранному ментору."
    }
  },
  {
    path: "/students",
    name: "students",
    component: StudentsPage,
    meta: {
      requiresAuth: true,
      roles: ["MENTOR"],
      title: "Студенты",
      description: "Каталог студентов для приглашений от менторов."
    }
  },
  {
    path: "/requests",
    name: "requests",
    component: RequestsPage,
    meta: {
      requiresAuth: true,
      roles: ["STUDENT", "MENTOR"],
      title: "Заявки",
      description: "Все заявки, их статусы и действия по обработке."
    }
  },
  {
    path: "/chat",
    name: "chat",
    component: ChatPage,
    meta: {
      requiresAuth: true,
      roles: ["STUDENT", "MENTOR"],
      title: "Чаты",
      description: "Переписка по заявкам и отправка сообщений."
    }
  },
  {
    path: "/files",
    name: "files",
    component: FilesPage,
    meta: {
      requiresAuth: true,
      roles: ["STUDENT", "MENTOR"],
      title: "Файлы",
      description: "Резюме, портфолио, аватар и вложения для чата."
    }
  },
  {
    path: "/reviews",
    name: "reviews",
    component: ReviewsPage,
    meta: {
      requiresAuth: true,
      roles: ["STUDENT", "MENTOR", "ADMIN"],
      title: "Отзывы",
      description: "Рейтинг менторов, отзывы и модерация обратной связи."
    }
  },
  {
    path: "/admin",
    name: "admin",
    component: AdminPage,
    meta: {
      requiresAuth: true,
      roles: ["ADMIN"],
      title: "Администрирование",
      description: "Пользователи, справочники, отзывы, жалобы и аудит."
    }
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const roles = to.meta.roles as AccessRole[] | undefined;

  if ((requiresAuth || roles?.length) && authStore.hasSession && !authStore.user) {
    await authStore.fetchCurrentUser();
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      name: "auth",
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (roles?.length && !hasAnyRole(authStore.user, roles)) {
    return { name: "home" };
  }

  return true;
});
