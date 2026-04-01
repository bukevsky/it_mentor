<script setup lang="ts">
import { onMounted } from "vue";
import ApiWorkbench from "@/widgets/api-workbench/ui/ApiWorkbench.vue";
import { useAuthStore } from "@/features/auth/model/auth-store";

const authStore = useAuthStore();

onMounted(() => {
  void authStore.initialize();
});
</script>

<template>
  <ApiWorkbench />
</template>
