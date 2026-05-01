<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { BaseButton, BaseInput, Tabs } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { getPrimaryWorkspaceRole } from "@/shared/lib/access";

type AuthMode = "login" | "register" | "recover";

const router = useRouter();
const authStore = useAuthStore();
const {
  error,
  fieldErrors,
  isAuthenticated,
  isRecoveringPassword,
  isSubmitting,
  user
} = storeToRefs(authStore);

const mode = ref<AuthMode>("login");
const successMessage = ref("");
const authTabs = [{ name: "Вход" }, { name: "Регистрация" }, { name: "Сброс пароля" }];

const loginForm = reactive({
  email: "student.demo@example.com",
  password: "DemoPass123"
});

const registerForm = reactive({
  email: "new.student@example.com",
  password: "SecurePass123",
  firstName: "Ярослав",
  lastName: "Шилов"
});

const recoveryForm = reactive({
  email: "student.demo@example.com",
  code: "",
  newPassword: ""
});

const activeTab = computed({
  get: () => {
    if (mode.value === "register") {
      return "Регистрация";
    }

    if (mode.value === "recover") {
      return "Сброс пароля";
    }

    return "Вход";
  },
  set: (value: string) => {
    if (value === "Регистрация") {
      mode.value = "register";
      return;
    }

    if (value === "Сброс пароля") {
      mode.value = "recover";
      return;
    }

    mode.value = "login";
  }
});

const accessHighlights = [
  "Профиль",
  "Менторы",
  "Заявки и чаты"
];

const authenticatedRouteName = computed(() => {
  return getPrimaryWorkspaceRole(user.value) === "ADMIN" ? "admin" : "profile";
});

watch(
  () => isAuthenticated.value,
  (nextValue) => {
    if (nextValue) {
      void router.replace({ name: authenticatedRouteName.value });
    }
  },
  { immediate: true }
);

const submitLogin = async () => {
  successMessage.value = "";
  const result = await authStore.login({ ...loginForm });

  if (result) {
    successMessage.value = "Вход выполнен.";
    void router.push({ name: authenticatedRouteName.value });
  }
};

const submitRegister = async () => {
  successMessage.value = "";
  const result = await authStore.register({ ...registerForm });

  if (result) {
    successMessage.value = "Аккаунт создан. Теперь можно войти.";
    mode.value = "login";
    loginForm.email = registerForm.email;
    loginForm.password = registerForm.password;
  }
};

const sendResetCode = async () => {
  successMessage.value = "";
  const isSent = await authStore.forgotPassword({ email: recoveryForm.email });

  if (isSent) {
    successMessage.value = "Код для сброса пароля отправлен.";
  }
};

const submitReset = async () => {
  successMessage.value = "";
  const isReset = await authStore.resetPassword({
    email: recoveryForm.email,
    code: recoveryForm.code,
    newPassword: recoveryForm.newPassword
  });

  if (isReset) {
    successMessage.value = "Пароль обновлён.";
    mode.value = "login";
    loginForm.email = recoveryForm.email;
    loginForm.password = recoveryForm.newPassword;
  }
};
</script>

<template>
  <section class="app-section auth-page">
    <div class="content-grid auth-page__grid">
      <article class="auth-card auth-card--primary">
        <p class="section-kicker">Аккаунт</p>
        <h3 class="section-title">Вход и управление доступом</h3>

        <Tabs v-model:active-tab="activeTab" class="base-tabs" :tabs="authTabs" />

        <form v-if="mode === 'login'" class="form-grid mt-6" @submit.prevent="submitLogin">
          <div class="base-form-grid">
            <BaseInput
              v-model="loginForm.email"
              title="Email"
              type="text"
              input-mode="email"
              placeholder="name@example.com"
              :state="fieldErrors.email ? 'error' : 'default'"
            />
            <small v-if="fieldErrors.email" class="helper-text">{{ fieldErrors.email }}</small>
          </div>

          <div class="base-form-grid">
            <BaseInput
              v-model="loginForm.password"
              title="Пароль"
              type="password"
              placeholder="Введите пароль"
              :state="fieldErrors.password ? 'error' : 'default'"
            />
            <small v-if="fieldErrors.password" class="helper-text">{{ fieldErrors.password }}</small>
          </div>

          <div class="base-actions auth-card__actions">
            <BaseButton
              size="l"
              type="submit"
              block
              :label="isSubmitting ? 'Вход...' : 'Войти'"
              :loading="isSubmitting"
            />
            <BaseButton
              variant="secondary"
              size="l"
              block
              label="Не помню пароль"
              @click="mode = 'recover'"
            />
          </div>
        </form>

        <form v-else-if="mode === 'register'" class="form-grid mt-6" @submit.prevent="submitRegister">
          <div class="form-grid form-grid--two conductor-grid">
            <BaseInput v-model="registerForm.firstName" title="Имя" placeholder="Введите имя" />
            <BaseInput v-model="registerForm.lastName" title="Фамилия" placeholder="Введите фамилию" />
          </div>

          <BaseInput v-model="registerForm.email" title="Email" type="text" input-mode="email" placeholder="name@example.com" />

          <BaseInput v-model="registerForm.password" title="Пароль" type="password" placeholder="Создайте пароль" />

          <div class="base-actions auth-card__actions">
            <BaseButton
              size="l"
              type="submit"
              block
              :label="isSubmitting ? 'Создание...' : 'Создать аккаунт'"
              :loading="isSubmitting"
            />
            <BaseButton
              variant="secondary"
              size="l"
              block
              label="У меня уже есть аккаунт"
              @click="mode = 'login'"
            />
          </div>
        </form>

        <div v-else class="form-grid mt-6">
          <BaseInput v-model="recoveryForm.email" title="Email" type="text" input-mode="email" placeholder="name@example.com" />

          <div class="base-actions auth-card__actions auth-card__actions--single">
            <BaseButton
              variant="secondary"
              size="l"
              block
              :label="isRecoveringPassword ? 'Отправка...' : 'Запросить код'"
              :loading="isRecoveringPassword"
              @click="sendResetCode"
            />
          </div>

          <div class="form-grid form-grid--two conductor-grid">
            <BaseInput v-model="recoveryForm.code" title="Код" placeholder="123456" />
            <BaseInput v-model="recoveryForm.newPassword" title="Новый пароль" type="password" placeholder="Введите новый пароль" />
          </div>

          <div class="base-actions auth-card__actions">
            <BaseButton
              size="l"
              block
              label="Сбросить пароль"
              :loading="isRecoveringPassword"
              @click="submitReset"
            />
            <BaseButton
              variant="secondary"
              size="l"
              block
              label="Вернуться ко входу"
              @click="mode = 'login'"
            />
          </div>
        </div>

        <div v-if="error" class="error-state mt-6">
          {{ error.message }}
        </div>

        <div v-if="successMessage" class="success-state mt-6">
          {{ successMessage }}
        </div>
      </article>

      <article class="auth-card auth-card--blue">
        <p class="section-kicker">Mentor Portal</p>
        <h3 class="workspace-title">{{ isAuthenticated ? "Аккаунт готов" : "Ваш инструмент для эффективного менторства" }}</h3>
        <p class="section-copy">
          Единая рабочая среда для профилей, заявок, чатов и материалов по практикам и стажировкам.
        </p>

        <div class="auth-feature-list">
          <div v-for="item in accessHighlights" :key="item" class="quick-action">
            <span class="metric-card__icon">{{ item.slice(0, 1) }}</span>
            <div>
              <strong>{{ item }}</strong>
              <p class="section-copy">Рабочий сценарий откроется после авторизации.</p>
            </div>
          </div>
        </div>

        <div v-if="isAuthenticated" class="success-state mt-6">
          Авторизация выполнена для {{ user?.email ?? "текущего пользователя" }}.
        </div>

        <div v-if="isAuthenticated" class="base-actions mt-6">
          <BaseButton label="Перейти в профиль" @click="router.push({ name: 'profile' })" />
          <BaseButton variant="secondary" label="Открыть менторов" @click="router.push({ name: 'mentors' })" />
        </div>
      </article>
    </div>
  </section>
</template>
