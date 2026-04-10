<script setup lang="ts">
import { onMounted } from "vue";
import { ThemeProvider } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import MainLayout from "@/app/layouts/MainLayout.vue";

const authStore = useAuthStore();
const dictionariesStore = useDictionariesStore();

onMounted(() => {
  void authStore.initialize();
  void dictionariesStore.loadAll();
});
</script>

<template>
  <ThemeProvider class-name="app-theme">
    <MainLayout />
  </ThemeProvider>
</template>
