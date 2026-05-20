import { computed } from "vue";
import { storeToRefs } from "pinia";
import { useDictionariesStore } from "@/features/dictionaries/model/dictionaries-store";
import { useProfilesStore } from "@/features/profile/model/profiles-store";
import {
  employmentTypeOptions,
  languageLevelOptions,
  mentoringChannelOptions,
  mentoringDurationOptions,
  mentoringTypeOptions,
  skillLevelOptions,
  workFormatOptions
} from "@/shared/lib/options";
import { getOptionLabel } from "@/shared/lib/presenters";

export type StudentProfileSectionId =
  | "main"
  | "about"
  | "skills"
  | "work"
  | "languages"
  | "files";

export type MentorProfileSectionId =
  | "main"
  | "about"
  | "format"
  | "skills"
  | "recruitment";

export interface StudentProfileSection {
  id: StudentProfileSectionId;
  title: string;
  summary: string;
}

export interface MentorProfileSection {
  id: MentorProfileSectionId;
  title: string;
  summary: string;
}

export interface CompletionTask {
  id: string;
  label: string;
  done: boolean;
}

export const studentProfileSections: StudentProfileSection[] = [
  { id: "main", title: "Основное", summary: "Имя, город и желаемая позиция" },
  { id: "about", title: "О себе", summary: "Описание, цели и контакт" },
  { id: "skills", title: "Навыки", summary: "Ключевые навыки и уровень" },
  { id: "work", title: "Опыт / занятость", summary: "Формат работы и нагрузка" },
  { id: "languages", title: "Языки", summary: "Языки и уровень владения" },
  { id: "files", title: "Файлы", summary: "Резюме и материалы профиля" }
];

export const mentorProfileSections: MentorProfileSection[] = [
  { id: "main", title: "Основное", summary: "Имя, позиция, город и контакты" },
  { id: "about", title: "Описание", summary: "О себе, ожидания и помощь" },
  { id: "format", title: "Формат", summary: "Тип, канал и длительность менторства" },
  { id: "skills", title: "Навыки", summary: "Направления, по которым вы менторите" },
  { id: "recruitment", title: "Набор", summary: "Статус, лимит и частота встреч" }
];

export const useProfile = () => {
  const dictionariesStore = useDictionariesStore();
  const profilesStore = useProfilesStore();

  const { cities, languages, skills } = storeToRefs(dictionariesStore);
  const { isStudentDirty, mentorForm, studentForm, studentProfile } = storeToRefs(profilesStore);

  const getCityLabel = (cityId: string) => {
    const city = cities.value.find((item) => String(item.id) === cityId);
    return city ? `${city.name}, ${city.region}` : "Город не выбран";
  };

  const getSkillLabel = (skillId: string) => {
    return skills.value.find((skill) => String(skill.id) === skillId)?.name ?? "Навык";
  };

  const getLanguageLabel = (languageId: string) => {
    return languages.value.find((language) => String(language.id) === languageId)?.name ?? "Язык";
  };

  const completionTasks = computed<CompletionTask[]>(() => [
    {
      id: "main",
      label: "Заполнить имя, фамилию, город и позицию",
      done: Boolean(
        studentForm.value.firstName.trim() &&
          studentForm.value.lastName.trim() &&
          studentForm.value.cityId &&
          studentForm.value.desiredPosition.trim()
      )
    },
    {
      id: "about",
      label: "Добавить описание и цели",
      done: Boolean(studentForm.value.about.trim().length >= 40)
    },
    {
      id: "skills",
      label: "Добавить минимум 3 навыка",
      done: studentForm.value.skills.length >= 3
    },
    {
      id: "work",
      label: "Указать занятость, формат работы и часы",
      done: Boolean(
        studentForm.value.employmentTypes.length &&
          studentForm.value.workFormats.length &&
          studentForm.value.hoursPerWeek
      )
    },
    {
      id: "languages",
      label: "Добавить язык",
      done: studentForm.value.languages.length > 0
    },
    {
      id: "files",
      label: "Загрузить резюме",
      done: Boolean(studentProfile.value?.resumeFileId)
    }
  ]);

  const completionPercent = computed(() => {
    const doneCount = completionTasks.value.filter((task) => task.done).length;
    return Math.round((doneCount / completionTasks.value.length) * 100);
  });

  const nextTasks = computed(() => completionTasks.value.filter((task) => !task.done).slice(0, 3));

  const mentorCompletionTasks = computed<CompletionTask[]>(() => [
    {
      id: "main",
      label: "Заполнить имя, фамилию, позицию и город",
      done: Boolean(
        mentorForm.value.firstName.trim() &&
          mentorForm.value.lastName.trim() &&
          mentorForm.value.position.trim() &&
          mentorForm.value.cityId
      )
    },
    {
      id: "about",
      label: "Добавить описание и чем можете помочь",
      done: Boolean(mentorForm.value.description.trim().length >= 40 && mentorForm.value.canHelpWith.trim())
    },
    {
      id: "format",
      label: "Настроить формат менторства",
      done: Boolean(
        mentorForm.value.mentoringType &&
          mentorForm.value.mentoringChannel &&
          mentorForm.value.mentoringDuration
      )
    },
    {
      id: "skills",
      label: "Добавить минимум 3 навыка",
      done: mentorForm.value.skills.length >= 3
    },
    {
      id: "recruitment",
      label: "Указать статус набора и лимит",
      done: Boolean(mentorForm.value.recruitmentStatus && mentorForm.value.menteeLimit)
    }
  ]);

  const mentorCompletionPercent = computed(() => {
    const doneCount = mentorCompletionTasks.value.filter((task) => task.done).length;
    return Math.round((doneCount / mentorCompletionTasks.value.length) * 100);
  });

  const mentorNextTasks = computed(() => mentorCompletionTasks.value.filter((task) => !task.done).slice(0, 3));

  const preview = computed(() => ({
    fullName: `${studentForm.value.firstName} ${studentForm.value.lastName}`.trim() || "Имя студента",
    city: getCityLabel(studentForm.value.cityId),
    position: studentForm.value.desiredPosition || "Желаемая позиция",
    about: studentForm.value.about || "Короткое описание появится здесь по мере заполнения профиля.",
    skills: studentForm.value.skills.map((skill) => ({
      id: skill.skillId,
      label: getSkillLabel(skill.skillId),
      level: getOptionLabel(skillLevelOptions, skill.level)
    })),
    workFormats: studentForm.value.workFormats.map((value) => getOptionLabel(workFormatOptions, value)),
    employmentTypes: studentForm.value.employmentTypes.map((value) => getOptionLabel(employmentTypeOptions, value)),
    languages: studentForm.value.languages.map((language) => ({
      id: language.languageId,
      label: getLanguageLabel(language.languageId),
      level: getOptionLabel(languageLevelOptions, language.level)
    }))
  }));

  const mentorPreview = computed(() => ({
    fullName: `${mentorForm.value.firstName} ${mentorForm.value.lastName}`.trim() || "Имя ментора",
    city: getCityLabel(mentorForm.value.cityId),
    position: mentorForm.value.position || "Позиция ментора",
    about: mentorForm.value.description || "Описание появится здесь по мере заполнения профиля.",
    skills: mentorForm.value.skills.map((skill) => ({
      id: skill.skillId,
      label: getSkillLabel(skill.skillId),
      level: getOptionLabel(skillLevelOptions, skill.level)
    })),
    workFormats: [
      getOptionLabel(mentoringTypeOptions, mentorForm.value.mentoringType),
      getOptionLabel(mentoringChannelOptions, mentorForm.value.mentoringChannel),
      getOptionLabel(mentoringDurationOptions, mentorForm.value.mentoringDuration)
    ],
    employmentTypes: mentorForm.value.department ? [mentorForm.value.department] : [],
    languages: []
  }));

  return {
    completionPercent,
    completionTasks,
    getLanguageLabel,
    getSkillLabel,
    hasUnsavedStudentChanges: isStudentDirty,
    mentorCompletionPercent,
    mentorCompletionTasks,
    mentorNextTasks,
    mentorPreview,
    mentorProfileSections,
    nextTasks,
    preview,
    studentProfileSections
  };
};
