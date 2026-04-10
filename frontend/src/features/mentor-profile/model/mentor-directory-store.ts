import { computed, reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ErrorResponse,
  MentorCardResponse,
  MentorSearchParams,
  MentoringRequestCreateRequest,
  PagedResponse
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { mentorProfileApi } from "../api/mentor-profile-api";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";

export const useMentorDirectoryStore = defineStore("mentor-directory", () => {
  const authStore = useAuthStore();

  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isLoading = ref(false);
  const results = ref<PagedResponse<MentorCardResponse> | null>(null);
  const selectedMentor = ref<MentorCardResponse | null>(null);

  const searchForm = reactive({
    q: "",
    cityId: "",
    skillId: "",
    recruitmentStatus: "",
    mentoringType: "",
    mentoringChannel: ""
  });

  const requestForm = reactive({
    goalType: "PRACTICE",
    message: "Здравствуйте! Хочу обсудить развитие в backend-направлении и возможный формат совместной работы."
  });

  const mentorsCount = computed(() => results.value?.totalElements ?? 0);

  const searchMentors = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    isLoading.value = true;
    error.value = null;

    const params: MentorSearchParams = {
      q: searchForm.q || undefined,
      cityId: searchForm.cityId ? Number(searchForm.cityId) : undefined,
      skillIds: searchForm.skillId ? [Number(searchForm.skillId)] : undefined,
      recruitmentStatus: searchForm.recruitmentStatus || undefined,
      mentoringType: searchForm.mentoringType || undefined,
      mentoringChannel: searchForm.mentoringChannel || undefined,
      page: 0,
      size: 24,
      sort: "createdAt,desc"
    };

    try {
      results.value = await mentorProfileApi.search(params);
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/profiles/mentors");
    } finally {
      isLoading.value = false;
    }
  };

  const selectMentor = (mentor: MentorCardResponse) => {
    selectedMentor.value = mentor;
    successMessage.value = "";
    error.value = null;
  };

  const submitRequest = async () => {
    if (!selectedMentor.value) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "FORM_ERROR",
        message: "Сначала выберите ментора.",
        path: "/mentoring/requests"
      };
      return;
    }

    isLoading.value = true;
    error.value = null;
    successMessage.value = "";

    const payload: MentoringRequestCreateRequest = {
      targetProfileId: selectedMentor.value.id,
      goalType: requestForm.goalType,
      message: requestForm.message
    };

    try {
      await mentoringApi.create(payload);
      successMessage.value = "Заявка отправлена.";
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests");
    } finally {
      isLoading.value = false;
    }
  };

  return {
    error,
    isLoading,
    mentorsCount,
    requestForm,
    results,
    searchForm,
    searchMentors,
    selectedMentor,
    selectMentor,
    submitRequest,
    successMessage
  };
});
