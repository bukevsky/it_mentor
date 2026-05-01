<script setup lang="ts">
import { BaseButton, BaseSelect } from "conductor";

const users = [
  { name: "Алексей Иванов", email: "alex.i@example.com", role: "Ментор", status: "Активен", date: "12 Окт 2026" },
  { name: "Мария Петрова", email: "maria.p@example.com", role: "Студент", status: "Заблокирован", date: "10 Окт 2026" },
  { name: "Иван Смирнов", email: "ivan.s@example.com", role: "Ментор", status: "На модерации", date: "08 Окт 2026" }
];

const roleOptions = [
  { value: "ALL", label: "Все роли" },
  { value: "STUDENT", label: "Студенты" },
  { value: "MENTOR", label: "Менторы" },
  { value: "ADMIN", label: "Администраторы" }
];

const statusOptions = [
  { value: "ALL", label: "Все статусы" },
  { value: "ACTIVE", label: "Активные" },
  { value: "BLOCKED", label: "Заблокированные" }
];
</script>

<template>
  <section class="app-section">
    <div class="workspace-header">
      <div>
        <h1 class="workspace-title title-with-badge">
          Панель управления
          <span class="demo-badge">Демо</span>
        </h1>
        <p class="workspace-subtitle">Пользователи, отзывы, справочники и аудит действий.</p>
      </div>
      <BaseButton size="l" label="Создать отчет" />
    </div>

    <div class="admin-grid">
      <div class="workspace-stack">
        <div class="admin-metrics">
          <article class="stat-card">
            <p class="stat-card__label">Всего пользователей</p>
            <p class="stat-card__value title-with-badge">1,248 <span class="demo-badge">Демо</span></p>
            <p class="stat-card__hint">+12% за месяц</p>
          </article>
          <article class="stat-card">
            <p class="stat-card__label">Активных менторов</p>
            <p class="stat-card__value title-with-badge">342 <span class="demo-badge">Демо</span></p>
            <p class="stat-card__hint">+5% за месяц</p>
          </article>
          <article class="stat-card">
            <p class="stat-card__label">Ожидают модерации</p>
            <p class="stat-card__value title-with-badge">28 <span class="demo-badge">Демо</span></p>
            <p class="stat-card__hint">Требует внимания</p>
          </article>
        </div>

        <article class="table-card">
          <div class="table-card__header">
            <h3 class="section-title">Последние регистрации</h3>
            <div class="base-actions">
              <BaseSelect title="" placeholder="Роль" :options="roleOptions" />
              <BaseSelect title="" placeholder="Статус" :options="statusOptions" />
            </div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>ID / имя</th>
                <th>Роль</th>
                <th>Статус</th>
                <th>Дата регистрации</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.email">
                <td>
                  <strong>{{ user.name }}</strong><br />
                  <span class="helper-text">{{ user.email }}</span>
                  <span class="demo-badge mt-1">Демо</span>
                </td>
                <td>{{ user.role }}</td>
                <td>
                  <span
                    class="status-pill"
                    :class="{
                      'status-pill--success': user.status === 'Активен',
                      'status-pill--danger': user.status === 'Заблокирован',
                      'status-pill--warning': user.status === 'На модерации'
                    }"
                  >
                    {{ user.status }}
                  </span>
                </td>
                <td>{{ user.date }}</td>
                <td><BaseButton variant="clear" size="m" label="Открыть" /></td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>

      <aside class="workspace-stack">
        <article class="workspace-card">
          <div class="workspace-header">
            <h3 class="section-title">Модерация отзывов</h3>
            <div class="badge-stack">
              <span class="demo-badge">Демо</span>
              <span class="status-pill status-pill--danger">12</span>
            </div>
          </div>
          <div class="detail-box mt-4">
            "Отличный ментор, но материалился во время сессии, что неприемлемо."
          </div>
          <div class="base-actions mt-4">
            <BaseButton variant="secondary" size="m" label="Отклонить" />
            <BaseButton size="m" label="Одобрить" />
          </div>
        </article>

        <article class="workspace-card">
          <h3 class="section-title title-with-badge">
            Справочники
            <span class="demo-badge">Демо</span>
          </h3>
          <ul class="clean-list upload-meta mt-4">
            <li><span>Города</span><strong>142 записи</strong></li>
            <li><span>Навыки</span><strong>854 записи</strong></li>
            <li><span>Языки</span><strong>18 записей</strong></li>
          </ul>
        </article>
      </aside>
    </div>
  </section>
</template>
