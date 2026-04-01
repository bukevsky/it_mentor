<script setup lang="ts">
import { reactive } from "vue";
import { storeToRefs } from "pinia";
import { env } from "@/shared/config/env";
import { useAuthStore } from "../model/auth-store";

const authStore = useAuthStore();
const {
  backendHealth,
  error,
  fieldErrors,
  hasSession,
  isAuthenticated,
  isInitializing,
  isSubmitting,
  user
} = storeToRefs(authStore);

const form = reactive({
  email: "",
  password: ""
});

const submit = async () => {
  await authStore.login({
    email: form.email,
    password: form.password
  });
};

const healthLabel = {
  idle: "не проверялось",
  checking: "проверка",
  up: "доступен",
  down: "недоступен"
} as const;
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <div>
        <p class="eyebrow">IT Mentor</p>
        <h1>Frontend подключен к backend через модульный API-слой</h1>
        <p class="lead">
          Точка входа для интеграции без изменений backend. В dev-режиме запросы
          идут через Vite proxy, а бизнес-вызовы изолированы в `shared/api` и
          `features/auth`.
        </p>
      </div>

      <dl class="meta-grid">
        <div>
          <dt>API base URL</dt>
          <dd>{{ env.apiBaseUrl }}</dd>
        </div>
        <div>
          <dt>Backend health</dt>
          <dd :data-state="backendHealth">{{ healthLabel[backendHealth] }}</dd>
        </div>
        <div>
          <dt>Сессия</dt>
          <dd>{{ hasSession ? "токен найден" : "токена нет" }}</dd>
        </div>
      </dl>
    </section>

    <section class="workspace-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-kicker">Auth</p>
            <h2>Вход в систему</h2>
          </div>
          <button class="ghost-button" type="button" @click="authStore.checkBackendHealth">
            Проверить backend
          </button>
        </div>

        <p v-if="isInitializing" class="hint">Инициализация клиентской сессии…</p>

        <form class="form-grid" @submit.prevent="submit">
          <label class="field">
            <span>Email</span>
            <input
              v-model.trim="form.email"
              autocomplete="username"
              name="email"
              placeholder="student@example.com"
              type="email"
            />
            <small v-if="fieldErrors.email">{{ fieldErrors.email }}</small>
          </label>

          <label class="field">
            <span>Пароль</span>
            <input
              v-model="form.password"
              autocomplete="current-password"
              name="password"
              placeholder="Введите пароль"
              type="password"
            />
            <small v-if="fieldErrors.password">{{ fieldErrors.password }}</small>
          </label>

          <button class="primary-button" :disabled="isSubmitting" type="submit">
            {{ isSubmitting ? "Выполняется вход..." : "Войти" }}
          </button>
        </form>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-kicker">Session</p>
            <h2>Текущий пользователь</h2>
          </div>
          <div class="actions">
            <button
              class="ghost-button"
              :disabled="!hasSession"
              type="button"
              @click="authStore.fetchCurrentUser"
            >
              Обновить /auth/me
            </button>
            <button
              class="ghost-button"
              :disabled="!isAuthenticated"
              type="button"
              @click="authStore.logout"
            >
              Выйти
            </button>
          </div>
        </div>

        <div v-if="user" class="user-card">
          <p><span>ID:</span> {{ user.id }}</p>
          <p><span>Email:</span> {{ user.email }}</p>
          <p><span>Статус:</span> {{ user.status }}</p>
          <p><span>Роли:</span> {{ user.roles.join(", ") }}</p>
        </div>

        <p v-else class="hint">
          После успешного логина frontend сохранит JWT и сможет читать `/auth/me`
          через общий HTTP-клиент.
        </p>
      </article>
    </section>
  </main>
</template>
