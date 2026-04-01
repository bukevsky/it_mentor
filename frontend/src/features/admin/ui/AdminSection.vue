<script setup lang="ts">
import { ref } from "vue";
import type { AdminRoleRequest, ErrorResponse } from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { parseOptionalNumber } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { adminApi } from "../api/admin-api";

const userId = ref("");
const role = ref<AdminRoleRequest["role"]>("MENTOR");
const response = ref<unknown>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);

const assignRole = async () => {
  const parsedUserId = parseOptionalNumber(userId.value);

  if (parsedUserId === undefined) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: "Укажите корректный userId",
      path: "/admin/users/{userId}/role"
    };
    return;
  }

  isLoading.value = true;
  error.value = null;

  try {
    await adminApi.assignRole(parsedUserId, { role: role.value });
    response.value = {
      status: "OK",
      userId: parsedUserId,
      role: role.value,
      note: "Роль назначена. Для нового JWT нужен повторный логин."
    };
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, `/admin/users/${parsedUserId}/role`);
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <SectionCard
    description="Админский endpoint для смены роли пользователя между STUDENT и MENTOR. ADMIN назначать этим endpoint нельзя."
    kicker="Admin"
    title="Администрирование"
  >
    <div class="section-grid">
      <div class="stack">
        <label class="field">
          <span>userId</span>
          <input v-model.trim="userId" placeholder="1" type="text" />
        </label>

        <label class="field">
          <span>role</span>
          <select v-model="role">
            <option value="STUDENT">STUDENT</option>
            <option value="MENTOR">MENTOR</option>
          </select>
        </label>

        <button class="primary-button" :disabled="isLoading" type="button" @click="assignRole">
          PUT /admin/users/{userId}/role
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>

      <JsonPreview :value="response" title="Admin result" />
    </div>
  </SectionCard>
</template>
