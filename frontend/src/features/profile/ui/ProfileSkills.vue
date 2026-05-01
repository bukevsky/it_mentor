<script setup lang="ts">
import { computed, ref } from "vue";
import { BaseButton, BaseSelect } from "conductor";
import type { StudentFormSkill } from "@/features/profile/model/profiles-store";
import { skillLevelOptions } from "@/shared/lib/options";

const props = defineProps<{
  skills: StudentFormSkill[];
  skillOptions: Array<{ value: string; label: string }>;
  selectState: string;
}>();

const emit = defineEmits<{
  (event: "add", skillId: string, level: string): void;
  (event: "remove", skillId: string): void;
  (event: "update-level", skillId: string, level: string): void;
}>();

const selectedSkillId = ref("");
const selectedLevel = ref("INTERMEDIATE");

const availableSkillOptions = computed(() => {
  const selectedIds = new Set(props.skills.map((skill) => skill.skillId));
  return props.skillOptions.filter((option) => option.value && !selectedIds.has(option.value));
});

const getSkillLabel = (skillId: string) => {
  return props.skillOptions.find((option) => option.value === skillId)?.label ?? "Навык";
};

const addSkill = () => {
  emit("add", selectedSkillId.value, selectedLevel.value);
  selectedSkillId.value = "";
  selectedLevel.value = "INTERMEDIATE";
};
</script>

<template>
  <div class="profile-skills">
    <div class="profile-skills__add">
      <BaseSelect
        v-model="selectedSkillId"
        size="m"
        title="Навык"
        placeholder="Выберите навык"
        :state="selectState"
        :options="availableSkillOptions"
      />
      <BaseSelect v-model="selectedLevel" size="m" title="Уровень" :options="skillLevelOptions" />
      <BaseButton
        size="m"
        label="Добавить"
        :disabled="!selectedSkillId"
        @click="addSkill"
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
      Добавьте несколько навыков, чтобы ментор быстрее понял ваш фокус.
    </div>
  </div>
</template>
