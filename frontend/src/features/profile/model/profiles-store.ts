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

export const useProfilesStore = defineStore("profiles", () => {
  const authStore = useAuthStore();

  const mode = ref<ProfileMode>("student");
  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isBusy = ref(false);
  const studentProfile = ref<StudentProfileResponse | null>(null);
  const mentorProfile = ref<MentorProfileResponse | null>(null);

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
    skillId: "",
    skillLevel: "INTERMEDIATE",
    languageId: "",
    languageLevel: "B2"
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
    studentForm.skillId = profile.skills[0]?.skill.id ? String(profile.skills[0].skill.id) : "";
    studentForm.skillLevel = profile.skills[0]?.level ?? "INTERMEDIATE";
    studentForm.languageId = profile.languages[0]?.language.id ? String(profile.languages[0].language.id) : "";
    studentForm.languageLevel = profile.languages[0]?.level ?? "B2";
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
      languages: studentForm.languageId
        ? [
            {
              languageId: Number(studentForm.languageId),
              level: studentForm.languageLevel as StudentProfileRequest["languages"][number]["level"]
            }
          ]
        : null,
      skills: studentForm.skillId
        ? [
            {
              skillId: Number(studentForm.skillId),
              level: studentForm.skillLevel as StudentProfileRequest["skills"][number]["level"]
            }
          ]
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
    canUseProfiles,
    error,
    initialize,
    isBusy,
    loadMentorProfile,
    loadStudentProfile,
    mentorForm,
    mentorProfile,
    mode,
    preferredMode,
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
    toggleItem
  };
});
