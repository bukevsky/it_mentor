<script setup lang="ts">
import { BaseSelect } from "conductor";
import type { StudentFormSkill } from "@/features/profile/model/profiles-store";
import ProfileSkillPicker from "@/features/profile/ui/ProfileSkillPicker.vue";
import { skillLevelOptions } from "@/shared/lib/options";

const props = withDefaults(
  defineProps<{
    skills: StudentFormSkill[];
    skillOptions: Array<{ value: string; label: string }>;
    selectState: string;
    pickerTitle?: string;
    pickerPlaceholder?: string;
    emptyText?: string;
    defaultLevel?: string;
  }>(),
  {
    pickerTitle: "Навыки",
    pickerPlaceholder: "Выберите навыки",
    emptyText: "Добавьте несколько навыков, чтобы ментор быстрее понял ваш фокус.",
    defaultLevel: "INTERMEDIATE"
  }
);

const emit = defineEmits<{
  (event: "add", skillId: string, level: string): void;
  (event: "remove", skillId: string): void;
  (event: "update-level", skillId: string, level: string): void;
}>();

const getSkillLabel = (skillId: string) => {
  return props.skillOptions.find((option) => option.value === skillId)?.label ?? "Навык";
};

const addSkill = (skillId: string, level: string) => {
  emit("add", skillId, level);
};
</script>

<template>
  <div class="profile-skills">
    <div class="profile-skills__add">
      <ProfileSkillPicker
        :skills="skills"
        :options="skillOptions"
        :select-state="selectState"
        :title="pickerTitle"
        :placeholder="pickerPlaceholder"
        :default-level="defaultLevel"
        @add="addSkill"
        @remove="emit('remove', $event)"
      />
    </div>

    <div v-if="skills.length" class="profile-tag-list">
      <div v-for="skill in skills" :key="skill.skillId" class="profile-tag profile-tag--skill">
        <strong>{{ getSkillLabel(skill.skillId) }}</strong>
        <BaseSelect
          :model-value="skill.level"
          size="s"
          title=""
          :options="skillLevelOptions"
          @update:model-value="emit('update-level', skill.skillId, String($event))"
        />
        <button type="button" class="profile-tag__remove" @click="emit('remove', skill.skillId)">
          Удалить
        </button>
      </div>
    </div>

    <div v-else class="empty-state profile-inline-empty">
      {{ emptyText }}
    </div>
  </div>
</template>
