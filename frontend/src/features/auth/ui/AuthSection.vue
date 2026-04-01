<script setup lang="ts">
import { reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import type { RegisterRequest, ResetPasswordRequest } from "@/shared/api/contracts";
import { formatJson } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { useAuthStore } from "../model/auth-store";

const authStore = useAuthStore();
const {
  backendHealth,
  error,
  fieldErrors,
  hasSession,
  isAuthenticated,
  isInitializing,
  isRecoveringPassword,
  isSubmitting,
  user
} = storeToRefs(authStore);

const loginForm = reactive({
  email: "test.frontend@example.com",
  password: "TestPass123!"
});

const registerDraft = ref(
  formatJson<RegisterRequest>({
    email: "new.student@example.com",
    password: "SecurePass123",
    firstName: "Иван",
    lastName: "Петров"
  })
);

const forgotEmail = ref("test.frontend@example.com");
const resetDraft = ref(
  formatJson<ResetPasswordRequest>({
    email: "test.frontend@example.com",
    code: "123456",
    newPassword: "NewSecurePass123"
  })
);

const registerResult = ref<unknown>();
const forgotResult = ref<unknown>();
const resetResult = ref<unknown>();

const submitLogin = async () => {
  await authStore.login({ ...loginForm });
};

const submitRegister = async () => {
  registerResult.value = await authStore.register(JSON.parse(registerDraft.value) as RegisterRequest);
};

const submitForgot = async () => {
  const success = await authStore.forgotPassword({ email: forgotEmail.value });
  forgotResult.value = success ? { status: "OK", message: "Запрос на сброс отправлен" } : undefined;
};

const submitReset = async () => {
  const success = await authStore.resetPassword(JSON.parse(resetDraft.value) as ResetPasswordRequest);
  resetResult.value = success ? { status: "OK", message: "Пароль обновлен" } : undefined;
};

const healthLabel = {
  idle: "не проверялось",
  checking: "проверка",
  up: "доступен",
  down: "недоступен"
} as const;
</script>

<template>
  <SectionCard
    description="Полный auth flow из guide: register, login, current user, forgot/reset password и проверка health."
    kicker="Auth"
    title="Аутентификация и сессия"
  >
    <div class="section-grid">
      <div class="stack">
        <div class="meta-grid meta-grid--compact">
          <div>
            <dt>Backend health</dt>
            <dd :data-state="backendHealth">{{ healthLabel[backendHealth] }}</dd>
          </div>
          <div>
            <dt>Сессия</dt>
            <dd>{{ hasSession ? "Bearer токен найден" : "токен не найден" }}</dd>
          </div>
          <div>
            <dt>Инициализация</dt>
            <dd>{{ isInitializing ? "выполняется" : "завершена" }}</dd>
          </div>
        </div>

        <div class="actions">
          <button class="ghost-button" type="button" @click="authStore.checkBackendHealth">
            Проверить health
          </button>
          <button class="ghost-button" :disabled="!hasSession" type="button" @click="authStore.fetchCurrentUser">
            Обновить /auth/me
          </button>
          <button class="ghost-button" :disabled="!isAuthenticated" type="button" @click="authStore.logout">
            Выйти
          </button>
        </div>

        <form class="form-grid" @submit.prevent="submitLogin">
          <label class="field">
            <span>Email</span>
            <input v-model.trim="loginForm.email" autocomplete="username" type="email" />
            <small v-if="fieldErrors.email">{{ fieldErrors.email }}</small>
          </label>

          <label class="field">
            <span>Пароль</span>
            <input v-model="loginForm.password" autocomplete="current-password" type="password" />
            <small v-if="fieldErrors.password">{{ fieldErrors.password }}</small>
          </label>

          <button class="primary-button" :disabled="isSubmitting" type="submit">
            {{ isSubmitting ? "Вход..." : "POST /auth/login" }}
          </button>
        </form>

        <label class="field">
          <span>Register payload</span>
          <textarea v-model="registerDraft" rows="8" />
        </label>
        <button class="ghost-button" :disabled="isSubmitting" type="button" @click="submitRegister">
          POST /auth/register
        </button>

        <label class="field">
          <span>Email для forgot password</span>
          <input v-model.trim="forgotEmail" type="email" />
        </label>
        <button class="ghost-button" :disabled="isRecoveringPassword" type="button" @click="submitForgot">
          POST /auth/password/forgot
        </button>

        <label class="field">
          <span>Reset payload</span>
          <textarea v-model="resetDraft" rows="8" />
        </label>
        <button class="ghost-button" :disabled="isRecoveringPassword" type="button" @click="submitReset">
          POST /auth/password/reset
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>

      <div class="stack">
        <JsonPreview :value="user ?? undefined" title="GET /auth/me" />
        <JsonPreview :value="registerResult" title="POST /auth/register" />
        <JsonPreview :value="forgotResult" title="POST /auth/password/forgot" />
        <JsonPreview :value="resetResult" title="POST /auth/password/reset" />
      </div>
    </div>
  </SectionCard>
</template>
