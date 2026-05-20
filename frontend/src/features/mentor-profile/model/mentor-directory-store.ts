import { computed, reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ErrorResponse,
  MentorCardResponse,
  MentorSearchParams,
  MentoringRequestCreateRequest,
  MentoringRequestStatus,
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
  const requestedMentorIds = ref<Set<number>>(new Set());
  let searchRequestId = 0;

  const excludedRequestStatuses: MentoringRequestStatus[] = [
    "SENT",
    "REVIEWING",
    "NEEDS_CLARIFICATION",
    "ACCEPTED",
    "COMPLETED"
  ];

  const searchForm = reactive({
    q: "",
    cityId: "",
    skillIds: [] as string[],
    recruitmentStatus: "",
    mentoringType: "",
    mentoringChannel: ""
  });

  const requestForm = reactive({
    goalType: "PRACTICE",
    message: "Здравствуйте! Хочу обсудить развитие в backend-направлении и возможный формат совместной работы."
  });

  const mentorsCount = computed(() => results.value?.totalElements ?? 0);

  const getSelectedSkillIds = () =>
    Array.isArray(searchForm.skillIds) ? searchForm.skillIds : [];

  const loadRequestedMentors = async () => {
    const response = await mentoringApi.getList({
      page: 0,
      size: 100
    });

    requestedMentorIds.value = new Set(
      response.content
        .filter((request) => excludedRequestStatuses.includes(request.status))
        .map((request) => request.mentorProfileId)
    );
  };

  const searchMentors = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    const currentRequestId = searchRequestId + 1;
    searchRequestId = currentRequestId;
    isLoading.value = true;
    error.value = null;

    const params: MentorSearchParams = {
      q: searchForm.q || undefined,
      cityId: searchForm.cityId ? Number(searchForm.cityId) : undefined,
      skillIds: getSelectedSkillIds().length ? getSelectedSkillIds().map(Number) : undefined,
      recruitmentStatus: searchForm.recruitmentStatus || undefined,
      mentoringType: searchForm.mentoringType || undefined,
      mentoringChannel: searchForm.mentoringChannel || undefined,
      page: 0,
      size: 24,
      sort: "createdAt,desc"
    };

    try {
      const [mentorResults] = await Promise.all([
        mentorProfileApi.search(params),
        loadRequestedMentors()
      ]);

      if (currentRequestId === searchRequestId) {
        results.value = mentorResults;
      }
    } catch (rawError) {
      if (currentRequestId === searchRequestId) {
        error.value = normalizeErrorResponse(rawError, "/profiles/mentors");
      }
    } finally {
      if (currentRequestId === searchRequestId) {
        isLoading.value = false;
      }
    }
  };

  const selectMentor = (mentor: MentorCardResponse) => {
    selectedMentor.value = mentor;
    successMessage.value = "";
    error.value = null;
  };

  const clearSelectedMentor = () => {
    selectedMentor.value = null;
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
      return false;
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
      requestedMentorIds.value = new Set([...requestedMentorIds.value, selectedMentor.value.id]);
      successMessage.value = "Заявка отправлена.";
      selectedMentor.value = null;
      return true;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests");
      return false;
    } finally {
      isLoading.value = false;
    }
  };

  return {
    error,
    isLoading,
    mentorsCount,
    requestForm,
    requestedMentorIds,
    results,
    searchForm,
    clearSelectedMentor,
    loadRequestedMentors,
    searchMentors,
    selectedMentor,
    selectMentor,
    submitRequest,
    successMessage
  };
});
