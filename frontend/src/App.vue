<script setup lang="ts">
import { storeToRefs } from "pinia";
import { useAppStore } from "@/stores/app";

const appStore = useAppStore();
const { title, stack, backendBaseUrl } = storeToRefs(appStore);
</script>

<template>
  <main class="app-shell">
    <section class="hero">
      <p class="eyebrow">IT Mentor</p>
      <h1>{{ title }}</h1>
      <p class="lead">
        Базовый фронтенд-каркас для монорепозитория. Дальше сюда можно
        добавлять роутинг, API-клиент и предметные модули.
      </p>
    </section>

    <section class="card-grid">
      <article class="card">
        <h2>Стек</h2>
        <ul>
          <li v-for="item in stack" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="card">
        <h2>Backend API</h2>
        <p>Базовый адрес для интеграции:</p>
        <code>{{ backendBaseUrl }}</code>
      </article>
    </section>
  </main>
</template>
