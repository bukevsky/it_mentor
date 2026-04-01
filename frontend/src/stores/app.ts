import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useAppStore = defineStore("app", () => {
  const appName = ref("IT Mentor Frontend");
  const backendBaseUrl = ref("http://localhost:8080");
  const stack = ref([
    "TypeScript 5.3",
    "Vue 3",
    "Pinia",
    "Vite",
    "Node.js 20"
  ]);

  const title = computed(() => appName.value);

  return {
    appName,
    backendBaseUrl,
    stack,
    title
  };
});
