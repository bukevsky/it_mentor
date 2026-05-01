import { computed, reactive, ref } from "vue";
import { defineStore } from "pinia";
import { ApiError } from "@/shared/api/http";
import type {
  ErrorResponse,
  MentorProfileRequest,
  MentorProfileResponse,
  StudentProfileRequest,
  StudentProfileResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { mentorProfileApi } from "@/features/mentor-profile/api/mentor-profile-api";
import { studentProfileApi } from "@/features/student-profile/api/student-profile-api";

type ProfileMode = "student" | "mentor";

export interface StudentFormSkill {
  skillId: string;
  level: string;
}

export interface StudentFormLanguage {
  languageId: string;
  level: string;
}

export const useProfilesStore = defineStore("profiles", () => {
  const authStore = useAuthStore();

  const mode = ref<ProfileMode>("student");
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isBusy = ref(false);
  const studentProfile = ref<StudentProfileResponse | null>(null);
  const mentorProfile = ref<MentorProfileResponse | null>(null);
  const studentSavedState = ref("");

  const studentForm = reactive({
    firstName: "",
    lastName: "",
    middleName: "",
    phone: "",
    cityId: "",
    desiredPosition: "",
    hoursPerWeek: 20,
    availableFrom: "",
    about: "",
    max: "",
    employmentTypes: ["PRACTICE"] as string[],
    workFormats: ["REMOTE"] as string[],
    skills: [] as StudentFormSkill[],
    languages: [] as StudentFormLanguage[]
  });

  const mentorForm = reactive({
    firstName: "",
    lastName: "",
    middleName: "",
    position: "",
    department: "",
    cityId: "",
    phone: "",
    max: "",
    description: "",
    expectations: "",
    canHelpWith: "",
    mentoringType: "INTERNSHIP",
    mentoringChannel: "MIXED",
    mentoringDuration: "THREE_MONTHS",
    mentoringFrequency: "2 раза в неделю",
    menteeLimit: 3,
    recruitmentStatus: "OPEN",
    skillId: "",
    skillLevel: "CONFIDENT"
  });

  const canUseProfiles = computed(() => authStore.isAuthenticated);
  const studentFormState = computed(() =>
    JSON.stringify({
      firstName: studentForm.firstName,
      lastName: studentForm.lastName,
      phone: studentForm.phone,
      cityId: studentForm.cityId,
      desiredPosition: studentForm.desiredPosition,
      hoursPerWeek: studentForm.hoursPerWeek,
      availableFrom: studentForm.availableFrom,
      about: studentForm.about,
      max: studentForm.max,
      employmentTypes: [...studentForm.employmentTypes].sort(),
      workFormats: [...studentForm.workFormats].sort(),
      skills: [...studentForm.skills].sort((left, right) => left.skillId.localeCompare(right.skillId)),
      languages: [...studentForm.languages].sort((left, right) => left.languageId.localeCompare(right.languageId))
    })
  );
  const isStudentDirty = computed(() => studentFormState.value !== studentSavedState.value);
  const preferredMode = computed<ProfileMode>(() => {
    return authStore.user?.roles.includes("MENTOR") ? "mentor" : "student";
  });

  const isProfileMissing = (rawError: unknown) => {
    return rawError instanceof ApiError && rawError.status === 404;
  };

  const patchStudentForm = (profile: StudentProfileResponse) => {
    studentForm.firstName = profile.firstName;
    studentForm.lastName = profile.lastName;
    studentForm.middleName = profile.middleName ?? "";
    studentForm.phone = profile.phone ?? "";
    studentForm.cityId = profile.city?.id ? String(profile.city.id) : "";
    studentForm.desiredPosition = profile.desiredPosition ?? "";
    studentForm.hoursPerWeek = profile.hoursPerWeek ?? 20;
    studentForm.availableFrom = profile.availableFrom ?? "";
    studentForm.about = profile.about ?? "";
    studentForm.max = profile.max ?? "";
    studentForm.employmentTypes = [...profile.employmentTypes];
    studentForm.workFormats = [...profile.workFormats];
    studentForm.skills = profile.skills.map((skill) => ({
      skillId: String(skill.skill.id),
      level: skill.level
    }));
    studentForm.languages = profile.languages.map((language) => ({
      languageId: String(language.language.id),
      level: language.level
    }));
    studentSavedState.value = studentFormState.value;
  };

  const patchMentorForm = (profile: MentorProfileResponse) => {
    mentorForm.firstName = profile.firstName;
    mentorForm.lastName = profile.lastName;
    mentorForm.middleName = profile.middleName ?? "";
    mentorForm.position = profile.position ?? "";
    mentorForm.department = profile.department ?? "";
    mentorForm.cityId = profile.city?.id ? String(profile.city.id) : "";
    mentorForm.phone = profile.phone ?? "";
    mentorForm.max = profile.max ?? "";
    mentorForm.description = profile.description ?? "";
    mentorForm.expectations = profile.expectations ?? "";
    mentorForm.canHelpWith = profile.canHelpWith ?? "";
    mentorForm.mentoringType = profile.mentoringType ?? "INTERNSHIP";
    mentorForm.mentoringChannel = profile.mentoringChannel ?? "MIXED";
    mentorForm.mentoringDuration = profile.mentoringDuration ?? "THREE_MONTHS";
    mentorForm.mentoringFrequency = profile.mentoringFrequency ?? "2 раза в неделю";
    mentorForm.menteeLimit = profile.menteeLimit ?? 3;
    mentorForm.recruitmentStatus = profile.recruitmentStatus ?? "OPEN";
    mentorForm.skillId = profile.skills[0]?.skill.id ? String(profile.skills[0].skill.id) : "";
    mentorForm.skillLevel = profile.skills[0]?.level ?? "CONFIDENT";
  };

  const buildStudentPayload = (): StudentProfileRequest => {
    return {
      firstName: studentForm.firstName,
      lastName: studentForm.lastName,
      middleName: studentForm.middleName || null,
      phone: studentForm.phone || null,
      cityId: studentForm.cityId ? Number(studentForm.cityId) : null,
      desiredPosition: studentForm.desiredPosition || null,
      hoursPerWeek: studentForm.hoursPerWeek || null,
      availableFrom: studentForm.availableFrom || null,
      about: studentForm.about || null,
      max: studentForm.max || null,
      employmentTypes: studentForm.employmentTypes.length
        ? [...studentForm.employmentTypes] as StudentProfileRequest["employmentTypes"]
        : null,
      workFormats: studentForm.workFormats.length
        ? [...studentForm.workFormats] as StudentProfileRequest["workFormats"]
        : null,
      languages: studentForm.languages.length
        ? studentForm.languages.map((language) => ({
            languageId: Number(language.languageId),
            level: language.level as StudentProfileRequest["languages"][number]["level"]
          }))
        : null,
      skills: studentForm.skills.length
        ? studentForm.skills.map((skill) => ({
            skillId: Number(skill.skillId),
            level: skill.level as StudentProfileRequest["skills"][number]["level"]
          }))
        : null
    };
  };

  const buildMentorPayload = (): MentorProfileRequest => {
    return {
      firstName: mentorForm.firstName,
      lastName: mentorForm.lastName,
      middleName: mentorForm.middleName || null,
      position: mentorForm.position || null,
      department: mentorForm.department || null,
      cityId: mentorForm.cityId ? Number(mentorForm.cityId) : null,
      phone: mentorForm.phone || null,
      max: mentorForm.max || null,
      description: mentorForm.description || null,
      expectations: mentorForm.expectations || null,
      canHelpWith: mentorForm.canHelpWith || null,
      mentoringType: mentorForm.mentoringType as MentorProfileRequest["mentoringType"],
      mentoringChannel: mentorForm.mentoringChannel as MentorProfileRequest["mentoringChannel"],
      mentoringFrequency: mentorForm.mentoringFrequency || null,
      mentoringDuration: mentorForm.mentoringDuration as MentorProfileRequest["mentoringDuration"],
      menteeLimit: mentorForm.menteeLimit || null,
      recruitmentStatus: mentorForm.recruitmentStatus as MentorProfileRequest["recruitmentStatus"],
      skills: mentorForm.skillId
        ? [
            {
              skillId: Number(mentorForm.skillId),
              level: mentorForm.skillLevel as MentorProfileRequest["skills"][number]["level"]
            }
          ]
        : null
    };
  };

  const toggleItem = (list: string[], value: string) => {
    const index = list.indexOf(value);

    if (index >= 0) {
      list.splice(index, 1);
      return;
    }

    list.push(value);
  };

  const addStudentSkill = (skillId: string, level: string) => {
    if (!skillId || studentForm.skills.some((skill) => skill.skillId === skillId)) {
      return;
    }

    studentForm.skills.push({ skillId, level });
  };

  const removeStudentSkill = (skillId: string) => {
    studentForm.skills = studentForm.skills.filter((skill) => skill.skillId !== skillId);
  };

  const updateStudentSkillLevel = (skillId: string, level: string) => {
    const target = studentForm.skills.find((skill) => skill.skillId === skillId);

    if (target) {
      target.level = level;
    }
  };

  const addStudentLanguage = (languageId: string, level: string) => {
    if (!languageId || studentForm.languages.some((language) => language.languageId === languageId)) {
      return;
    }

    studentForm.languages.push({ languageId, level });
  };

  const removeStudentLanguage = (languageId: string) => {
    studentForm.languages = studentForm.languages.filter((language) => language.languageId !== languageId);
  };

  const updateStudentLanguageLevel = (languageId: string, level: string) => {
    const target = studentForm.languages.find((language) => language.languageId === languageId);

    if (target) {
      target.level = level;
    }
  };

  const loadStudentProfile = async () => {
    if (!canUseProfiles.value) {
      return;
    }

    isBusy.value = true;
    error.value = null;

    try {
      const response = await studentProfileApi.getMine();
      studentProfile.value = response;
      patchStudentForm(response);
    } catch (rawError) {
      if (isProfileMissing(rawError)) {
      studentProfile.value = null;
        studentSavedState.value = studentFormState.value;
        return;
      }

      error.value = normalizeErrorResponse(rawError, "/profile/student/me");
    } finally {
      isBusy.value = false;
    }
  };

  const loadMentorProfile = async () => {
    if (!canUseProfiles.value) {
      return;
    }

    isBusy.value = true;
    error.value = null;

    try {
      const response = await mentorProfileApi.getMine();
      mentorProfile.value = response;
      patchMentorForm(response);
    } catch (rawError) {
      if (isProfileMissing(rawError)) {
        mentorProfile.value = null;
        return;
      }

      error.value = normalizeErrorResponse(rawError, "/profile/mentor/me");
    } finally {
      isBusy.value = false;
    }
  };

  const saveStudentProfile = async () => {
    isBusy.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      const response = await studentProfileApi.upsert(buildStudentPayload());
      studentProfile.value = response;
      patchStudentForm(response);
      successMessage.value = "Профиль студента сохранён.";
      studentSavedState.value = studentFormState.value;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/profile/student");
    } finally {
      isBusy.value = false;
    }
  };

  const saveMentorProfile = async () => {
    isBusy.value = true;
    error.value = null;
    successMessage.value = "";

    try {
      const response = await mentorProfileApi.upsert(buildMentorPayload());
      mentorProfile.value = response;
      patchMentorForm(response);
      successMessage.value = "Профиль ментора сохранён.";
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/profile/mentor");
    } finally {
      isBusy.value = false;
    }
  };

  const initialize = async () => {
    if (!canUseProfiles.value) {
      return;
    }

    mode.value = preferredMode.value;
    error.value = null;

    if (preferredMode.value === "mentor") {
      await loadMentorProfile();
      return;
    }

    await loadStudentProfile();
  };

  return {
    addStudentLanguage,
    addStudentSkill,
    canUseProfiles,
    error,
    initialize,
    isBusy,
    isStudentDirty,
    loadMentorProfile,
    loadStudentProfile,
    mentorForm,
    mentorProfile,
    mode,
    preferredMode,
    removeStudentLanguage,
    removeStudentSkill,
    saveMentorProfile,
    saveStudentProfile,
    setMode: (nextMode: ProfileMode) => {
      mode.value = nextMode;
      error.value = null;
      successMessage.value = "";
    },
    studentForm,
    studentProfile,
    successMessage,
    toggleItem,
    updateStudentLanguageLevel,
    updateStudentSkillLevel
  };
});
