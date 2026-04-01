import type {
  EducationDegree,
  EducationForm,
  EmploymentType,
  LanguageLevel,
  MentoringChannel,
  MentoringDuration,
  MentoringRequestStatus,
  MentoringType,
  RecruitmentStatus,
  RoleCode,
  SkillLevel,
  WorkFormat
} from "@/shared/api/contracts";

export interface Option<T extends string> {
  value: T;
  label: string;
}

export const mentoringTypeOptions: Option<MentoringType>[] = [
  { value: "PRACTICE", label: "Практика" },
  { value: "INTERNSHIP", label: "Стажировка" },
  { value: "PROJECT", label: "Проектное сопровождение" }
];

export const mentoringChannelOptions: Option<MentoringChannel>[] = [
  { value: "CHAT", label: "Чат" },
  { value: "CALLS", label: "Созвоны" },
  { value: "MIXED", label: "Смешанный" }
];

export const mentoringDurationOptions: Option<MentoringDuration>[] = [
  { value: "ONE_MONTH", label: "1 месяц" },
  { value: "THREE_MONTHS", label: "3 месяца" },
  { value: "FLEXIBLE", label: "Гибко" }
];

export const recruitmentStatusOptions: Option<RecruitmentStatus>[] = [
  { value: "OPEN", label: "Набираю" },
  { value: "PAUSED", label: "Пауза" },
  { value: "CLOSED", label: "Нет мест" }
];

export const mentoringRequestStatusOptions: Option<MentoringRequestStatus>[] = [
  { value: "SENT", label: "Отправлена" },
  { value: "REVIEWING", label: "На рассмотрении" },
  { value: "NEEDS_CLARIFICATION", label: "Требует уточнения" },
  { value: "ACCEPTED", label: "Принята" },
  { value: "REJECTED", label: "Отклонена" },
  { value: "CANCELLED", label: "Отменена" },
  { value: "COMPLETED", label: "Завершена" }
];

export const roleOptions: Option<Extract<RoleCode, "STUDENT" | "MENTOR">>[] = [
  { value: "STUDENT", label: "Студент" },
  { value: "MENTOR", label: "Ментор" }
];

export const employmentTypeOptions: Option<EmploymentType>[] = [
  { value: "PRACTICE", label: "Практика" },
  { value: "INTERNSHIP", label: "Стажировка" },
  { value: "PART_TIME", label: "Part-time" },
  { value: "FULL_TIME", label: "Full-time" },
  { value: "PROJECT", label: "Проектная" },
  { value: "OTHER", label: "Другое" }
];

export const workFormatOptions: Option<WorkFormat>[] = [
  { value: "REMOTE", label: "Удаленно" },
  { value: "OFFICE", label: "Офис" },
  { value: "HYBRID", label: "Гибрид" }
];

export const languageLevelOptions: Option<LanguageLevel>[] = [
  { value: "A1", label: "A1" },
  { value: "A2", label: "A2" },
  { value: "B1", label: "B1" },
  { value: "B2", label: "B2" },
  { value: "C1", label: "C1" },
  { value: "C2", label: "C2" },
  { value: "NATIVE", label: "Родной" }
];

export const skillLevelOptions: Option<SkillLevel>[] = [
  { value: "BEGINNER", label: "Начальный" },
  { value: "INTERMEDIATE", label: "Средний" },
  { value: "CONFIDENT", label: "Уверенный" }
];

export const educationDegreeOptions: Option<EducationDegree>[] = [
  { value: "BACHELOR", label: "Бакалавр" },
  { value: "SPECIALIST", label: "Специалист" },
  { value: "MASTER", label: "Магистр" },
  { value: "COURSE", label: "Курсы" },
  { value: "OTHER", label: "Другое" }
];

export const educationFormOptions: Option<EducationForm>[] = [
  { value: "FULL_TIME", label: "Очно" },
  { value: "PART_TIME", label: "Очно-заочно" },
  { value: "DISTANCE", label: "Заочно" }
];
