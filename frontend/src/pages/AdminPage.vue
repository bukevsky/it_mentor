<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { BaseButton, BaseInput } from "conductor";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { adminApi } from "@/features/admin/api/admin-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import type {
  AdminUserResponse,
  AdminUsersStatsResponse,
  AuditLogResponse,
  ComplaintResponse,
  DictionaryType,
  ErrorResponse,
  OutboxEntryResponse,
  PagedResponse,
  RoleCode,
  UserStatus
} from "@/shared/api/contracts";

const roleLabels: Record<RoleCode, string> = {
  STUDENT: "Студент",
  MENTOR: "Ментор",
  ADMIN: "Администратор"
};

const statusLabels: Record<UserStatus, string> = {
  ACTIVE: "Активен",
  EMAIL_NOT_CONFIRMED: "Email не подтверждён",
  BLOCKED: "Заблокирован",
  DELETED: "Удалён"
};

const dictionaryLabels: Record<DictionaryType, string> = {
  cities: "Города",
  skills: "Навыки",
  languages: "Языки",
  "interaction-types": "Типы взаимодействия"
};

const filters = reactive<{
  q: string;
  role: "" | RoleCode;
  status: "" | UserStatus;
}>({
  q: "",
  role: "",
  status: ""
});

const users = ref<PagedResponse<AdminUserResponse> | null>(null);
const stats = ref<AdminUsersStatsResponse | null>(null);
const complaints = ref<PagedResponse<ComplaintResponse> | null>(null);
const audit = ref<PagedResponse<AuditLogResponse> | null>(null);
const outbox = ref<PagedResponse<OutboxEntryResponse> | null>(null);
const dictionaryCounts = ref<Record<DictionaryType, { total: number; active: number }>>({
  cities: { total: 0, active: 0 },
  skills: { total: 0, active: 0 },
  languages: { total: 0, active: 0 },
  "interaction-types": { total: 0, active: 0 }
});
const router = useRouter();
const authStore = useAuthStore();

const logout = () => {
  authStore.logout();
  void router.push({ name: "auth" });
};

const selectedUser = ref<AdminUserResponse | null>(null);
const isLoading = ref(false);
const isMutating = ref(false);
const error = ref<ErrorResponse | null>(null);
const successMessage = ref("");

const totalUsers = computed(() => stats.value?.totalUsers ?? users.value?.totalElements ?? 0);
const mentorsCount = computed(() => stats.value?.byRole?.MENTOR ?? 0);
const studentsCount = computed(() => stats.value?.byRole?.STUDENT ?? 0);
const blockedCount = computed(() => stats.value?.byStatus?.BLOCKED ?? 0);
const openComplaints = computed(() => complaints.value?.content.filter((item) => item.status === "OPEN").length ?? 0);

const formatDate = (value: string | null) => {
  if (!value) {
    return "Не указано";
  }

  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
};

const getUserName = (user: AdminUserResponse) => {
  const name = [user.firstName, user.lastName].filter(Boolean).join(" ");
  return name || "Профиль не заполнен";
};

const getUserRole = (user: AdminUserResponse) => {
  const priority: RoleCode[] = ["ADMIN", "MENTOR", "STUDENT"];
  return priority.find((role) => user.roles.includes(role)) ?? user.roles[0] ?? "STUDENT";
};

const loadUsers = async () => {
  users.value = await adminApi.getUsers({
    q: filters.q || undefined,
    role: filters.role || undefined,
    status: filters.status || undefined,
    page: 0,
    size: 20
  });
};

const loadDictionaries = async () => {
  const entries = await Promise.all(
    (Object.keys(dictionaryLabels) as DictionaryType[]).map(async (type) => {
      const items = await adminApi.getDictionary(type);
      return [
        type,
        {
          total: items.length,
          active: items.filter((item) => item.active).length
        }
      ] as const;
    })
  );

  dictionaryCounts.value = Object.fromEntries(entries) as Record<DictionaryType, { total: number; active: number }>;
};

const loadAdminData = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    const [statsResponse, complaintsResponse, auditResponse, outboxResponse] = await Promise.all([
      adminApi.getUsersStats(),
      adminApi.getComplaints({ page: 0, size: 5 }),
      adminApi.getAudit({ page: 0, size: 5 }),
      adminApi.getOutbox({ page: 0, size: 5 }),
      loadUsers(),
      loadDictionaries()
    ]);
    stats.value = statsResponse;
    complaints.value = complaintsResponse;
    audit.value = auditResponse;
    outbox.value = outboxResponse;
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, "/admin");
  } finally {
    isLoading.value = false;
  }
};

const refreshUsers = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    await loadUsers();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, "/admin/users");
  } finally {
    isLoading.value = false;
  }
};

const changeRole = async (user: AdminUserResponse, role: "STUDENT" | "MENTOR") => {
  if (getUserRole(user) === role) {
    return;
  }

  isMutating.value = true;
  error.value = null;
  successMessage.value = "";

  try {
    await adminApi.assignRole(user.id, { role });
    successMessage.value = "Роль обновлена. Пользователю нужно войти заново.";
    await loadAdminData();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, `/admin/users/${user.id}/role`);
  } finally {
    isMutating.value = false;
  }
};

const changeStatus = async (user: AdminUserResponse, status: Extract<UserStatus, "ACTIVE" | "BLOCKED" | "DELETED">) => {
  if (user.status === status) {
    return;
  }

  isMutating.value = true;
  error.value = null;
  successMessage.value = "";

  try {
    await adminApi.changeUserStatus(user.id, { status });
    successMessage.value = "Статус обновлён. Старые JWT пользователя инвалидированы.";
    await loadAdminData();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, `/admin/users/${user.id}/status`);
  } finally {
    isMutating.value = false;
  }
};

const resolveComplaint = async (complaint: ComplaintResponse, status: "RESOLVED" | "REJECTED") => {
  isMutating.value = true;
  error.value = null;
  successMessage.value = "";

  try {
    await adminApi.resolveComplaint(complaint.id, {
      status,
      resolution: status === "RESOLVED" ? "Обработано администратором" : "Жалоба отклонена администратором"
    });
    successMessage.value = "Жалоба обновлена.";
    await loadAdminData();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, `/admin/complaints/${complaint.id}/resolve`);
  } finally {
    isMutating.value = false;
  }
};

onMounted(() => {
  void loadAdminData();
});
</script>

<template>
  <section class="app-section">
    <div class="workspace-header">
      <div>
        <h1 class="workspace-title">Панель управления</h1>
        <p class="workspace-subtitle">Управление доступом, статусами, жалобами и справочниками платформы.</p>
      </div>
      <div class="base-actions">
        <BaseButton variant="secondary" size="m" class="btn-logout" label="Выйти" @click="logout" />
        <BaseButton size="l" label="Обновить" :loading="isLoading" @click="loadAdminData" />
      </div>
    </div>

    <div v-if="error" class="alert alert--error">
      <strong>{{ error.error }}</strong>
      <span>{{ error.message }}</span>
    </div>
    <div v-if="successMessage" class="alert alert--success">
      {{ successMessage }}
    </div>

    <div class="admin-grid">
      <div class="workspace-stack">
        <div class="admin-metrics">
          <article class="stat-card">
            <p class="stat-card__label">Всего пользователей</p>
            <p class="stat-card__value">{{ totalUsers }}</p>
            <p class="stat-card__hint">Аккаунты в системе</p>
          </article>
          <article class="stat-card">
            <p class="stat-card__label">Менторы / студенты</p>
            <p class="stat-card__value">{{ mentorsCount }} / {{ studentsCount }}</p>
            <p class="stat-card__hint">Активные рабочие роли</p>
          </article>
          <article class="stat-card">
            <p class="stat-card__label">Жалобы / блокировки</p>
            <p class="stat-card__value">{{ openComplaints }} / {{ blockedCount }}</p>
            <p class="stat-card__hint">Очередь модерации</p>
          </article>
        </div>

        <article class="table-card">
          <div class="table-card__header">
            <h3 class="section-title">Пользователи</h3>
            <div class="base-actions admin-filters">
              <BaseInput v-model="filters.q" title="" placeholder="Поиск по имени или email" />
              <select v-model="filters.role" class="admin-select" aria-label="Роль">
                <option value="">Все роли</option>
                <option value="STUDENT">Студенты</option>
                <option value="MENTOR">Менторы</option>
                <option value="ADMIN">Администраторы</option>
              </select>
              <select v-model="filters.status" class="admin-select" aria-label="Статус">
                <option value="">Все статусы</option>
                <option value="ACTIVE">Активные</option>
                <option value="BLOCKED">Заблокированные</option>
                <option value="DELETED">Удалённые</option>
              </select>
              <BaseButton size="m" label="Найти" :loading="isLoading" @click="refreshUsers" />
            </div>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>ID / имя</th>
                <th>Роли</th>
                <th>Статус</th>
                <th>Дата регистрации</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users?.content ?? []" :key="user.id">
                <td>
                  <strong>#{{ user.id }} · {{ getUserName(user) }}</strong><br />
                  <span class="helper-text">{{ user.email }}</span>
                </td>
                <td>{{ user.roles.map((role) => roleLabels[role] ?? role).join(", ") }}</td>
                <td>
                  <span
                    class="status-pill"
                    :class="{
                      'status-pill--success': user.status === 'ACTIVE',
                      'status-pill--danger': user.status === 'BLOCKED' || user.status === 'DELETED',
                      'status-pill--warning': user.status === 'EMAIL_NOT_CONFIRMED'
                    }"
                  >
                    {{ statusLabels[user.status] ?? user.status }}
                  </span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>
                  <div class="admin-row-actions">
                    <BaseButton variant="clear" size="s" label="Открыть" @click="selectedUser = user" />
                    <BaseButton
                      v-if="!user.roles.includes('ADMIN')"
                      variant="secondary"
                      size="s"
                      :label="getUserRole(user) === 'MENTOR' ? 'В студенты' : 'В менторы'"
                      :disabled="isMutating"
                      @click="changeRole(user, getUserRole(user) === 'MENTOR' ? 'STUDENT' : 'MENTOR')"
                    />
                    <BaseButton
                      v-if="user.status !== 'BLOCKED'"
                      variant="secondary"
                      size="s"
                      label="Блок"
                      :disabled="isMutating"
                      @click="changeStatus(user, 'BLOCKED')"
                    />
                    <BaseButton
                      v-else
                      variant="secondary"
                      size="s"
                      label="Актив"
                      :disabled="isMutating"
                      @click="changeStatus(user, 'ACTIVE')"
                    />
                  </div>
                </td>
              </tr>
              <tr v-if="!isLoading && !(users?.content.length)">
                <td colspan="5">Пользователи не найдены.</td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>

      <aside class="workspace-stack">
        <article v-if="selectedUser" class="workspace-card">
          <div class="workspace-header">
            <h3 class="section-title">Карточка пользователя</h3>
            <BaseButton variant="clear" size="s" label="Закрыть" @click="selectedUser = null" />
          </div>
          <ul class="clean-list upload-meta mt-4">
            <li><span>ID</span><strong>{{ selectedUser.id }}</strong></li>
            <li><span>Email</span><strong>{{ selectedUser.email }}</strong></li>
            <li><span>Роли</span><strong>{{ selectedUser.roles.join(", ") }}</strong></li>
            <li><span>Статус</span><strong>{{ statusLabels[selectedUser.status] }}</strong></li>
          </ul>
        </article>

        <article class="workspace-card">
          <h3 class="section-title">Справочники</h3>
          <ul class="clean-list upload-meta mt-4">
            <li v-for="(value, key) in dictionaryCounts" :key="key">
              <span>{{ dictionaryLabels[key] }}</span>
              <strong>{{ value.active }} / {{ value.total }}</strong>
            </li>
          </ul>
        </article>

        <article class="workspace-card">
          <div class="workspace-header">
            <h3 class="section-title">Жалобы</h3>
            <span class="status-pill status-pill--danger">{{ complaints?.totalElements ?? 0 }}</span>
          </div>
          <div v-if="complaints?.content.length" class="workspace-stack mt-4">
            <div v-for="complaint in complaints.content" :key="complaint.id" class="detail-box">
              <strong>#{{ complaint.id }} · {{ complaint.targetType }} {{ complaint.targetId }}</strong>
              <p>{{ complaint.reason }}</p>
              <small>{{ complaint.status }} · {{ formatDate(complaint.createdAt) }}</small>
              <div v-if="complaint.status === 'OPEN'" class="base-actions mt-3">
                <BaseButton size="s" label="Решено" :disabled="isMutating" @click="resolveComplaint(complaint, 'RESOLVED')" />
                <BaseButton
                  variant="secondary"
                  size="s"
                  label="Отклонить"
                  :disabled="isMutating"
                  @click="resolveComplaint(complaint, 'REJECTED')"
                />
              </div>
            </div>
          </div>
          <p v-else class="helper-text mt-4">Жалоб нет.</p>
        </article>

        <article class="workspace-card">
          <h3 class="section-title">Журнал действий</h3>
          <ul v-if="audit?.content.length" class="clean-list upload-meta mt-4">
            <li v-for="item in audit.content" :key="item.id">
              <span>{{ item.action }}</span>
              <strong>{{ formatDate(item.createdAt) }}</strong>
            </li>
          </ul>
          <p v-else class="helper-text mt-4">Событий нет.</p>
        </article>

        <article class="workspace-card">
          <h3 class="section-title">Очередь уведомлений</h3>
          <ul v-if="outbox?.content.length" class="clean-list upload-meta mt-4">
            <li v-for="item in outbox.content" :key="item.id">
              <span>{{ item.status }} · {{ item.eventType }}</span>
              <strong>{{ item.attempts }}</strong>
            </li>
          </ul>
          <p v-else class="helper-text mt-4">Ожидающих уведомлений нет.</p>
        </article>
      </aside>
    </div>
  </section>
</template>
