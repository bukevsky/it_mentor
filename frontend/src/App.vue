<script setup lang="ts">
import { onMounted } from "vue";
import AuthWorkspace from "@/features/auth/ui/AuthWorkspace.vue";
import { useAuthStore } from "@/features/auth/model/auth-store";

const authStore = useAuthStore();

onMounted(() => {
  void authStore.initialize();
});
</script>

<template>
  <AuthWorkspace />
</template>
