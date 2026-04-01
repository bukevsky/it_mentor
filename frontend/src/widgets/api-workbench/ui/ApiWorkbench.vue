<script setup lang="ts">
import { env } from "@/shared/config/env";
import AdminSection from "@/features/admin/ui/AdminSection.vue";
import AuthSection from "@/features/auth/ui/AuthSection.vue";
import ChatSection from "@/features/chat/ui/ChatSection.vue";
import DictionariesSection from "@/features/dictionaries/ui/DictionariesSection.vue";
import FileUploadsSection from "@/features/files/ui/FileUploadsSection.vue";
import MentoringRequestsSection from "@/features/mentoring/ui/MentoringRequestsSection.vue";
import MentorProfileSection from "@/features/mentor-profile/ui/MentorProfileSection.vue";
import ProfileSummarySection from "@/features/profile-summary/ui/ProfileSummarySection.vue";
import StudentProfileSection from "@/features/student-profile/ui/StudentProfileSection.vue";
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <div>
        <p class="eyebrow">IT Mentor</p>
        <h1>Frontend workbench под полный backend guide</h1>
        <p class="lead">
          Клиент покрывает все разделы `backend/README_FRONT.md`: аутентификацию,
          справочники, профили, файлы, поиск менторов, заявки, чаты и admin-операции.
          Backend при этом остается только источником контрактов и API.
        </p>
      </div>

      <dl class="meta-grid">
        <div>
          <dt>API base URL</dt>
          <dd>{{ env.apiBaseUrl }}</dd>
        </div>
        <div>
          <dt>Guide source</dt>
          <dd>backend/README_FRONT.md</dd>
        </div>
        <div>
          <dt>Подход</dt>
          <dd>Typed DTO + feature API modules + workbench UI</dd>
        </div>
      </dl>
    </section>

    <section class="workspace-stack">
      <AuthSection />
      <DictionariesSection />
      <ProfileSummarySection />
      <StudentProfileSection />
      <MentorProfileSection />
      <FileUploadsSection />
      <MentoringRequestsSection />
      <ChatSection />
      <AdminSection />
    </section>
  </main>
</template>
