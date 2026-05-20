import { computed, reactive, ref } from "vue";
import { defineStore } from "pinia";
import type {
  ErrorResponse,
  MentoringRequestCreateRequest,
  MentoringRequestStatus,
  PagedResponse,
  StudentProfileResponse,
  StudentSearchParams
} from "@/shared/api/contracts";
import { useAuthStore } from "@/features/auth/model/auth-store";
import { mentoringApi } from "@/features/mentoring/api/mentoring-api";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { studentProfileApi } from "../api/student-profile-api";

export const useStudentDirectoryStore = defineStore("student-directory", () => {
  const authStore = useAuthStore();

  const error = ref<ErrorResponse | null>(null);
  const successMessage = ref("");
  const isLoading = ref(false);
  const results = ref<PagedResponse<StudentProfileResponse> | null>(null);
  const selectedStudent = ref<StudentProfileResponse | null>(null);
  const requestedStudentIds = ref<Set<number>>(new Set());
  let searchRequestId = 0;

  const activeRequestStatuses: MentoringRequestStatus[] = [
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
    employmentType: "",
    workFormat: ""
  });

  const requestForm = reactive({
    goalType: "PRACTICE",
    message: "Здравствуйте! Хочу предложить менторство и обсудить, чем могу быть полезен в вашем развитии."
  });

  const studentsCount = computed(() => results.value?.totalElements ?? 0);

  const getSelectedSkillIds = () =>
    Array.isArray(searchForm.skillIds) ? searchForm.skillIds : [];

  const loadRequestedStudents = async () => {
    const response = await mentoringApi.getList({
      page: 0,
      size: 100
    });

    requestedStudentIds.value = new Set(
      response.content
        .filter((request) => activeRequestStatuses.includes(request.status))
        .map((request) => request.studentProfileId)
    );
  };

  const searchStudents = async () => {
    if (!authStore.isAuthenticated) {
      return;
    }

    const currentRequestId = searchRequestId + 1;
    searchRequestId = currentRequestId;
    isLoading.value = true;
    error.value = null;

    const params: StudentSearchParams = {
      q: searchForm.q || undefined,
      cityId: searchForm.cityId ? Number(searchForm.cityId) : undefined,
      skillIds: getSelectedSkillIds().length ? getSelectedSkillIds().map(Number) : undefined,
      employmentType: searchForm.employmentType || undefined,
      workFormat: searchForm.workFormat || undefined,
      page: 0,
      size: 24,
      sort: "createdAt,desc"
    };

    try {
      const [studentResults] = await Promise.all([
        studentProfileApi.search(params),
        loadRequestedStudents()
      ]);

      if (currentRequestId === searchRequestId) {
        results.value = studentResults;
      }
    } catch (rawError) {
      if (currentRequestId === searchRequestId) {
        error.value = normalizeErrorResponse(rawError, "/profiles/students");
      }
    } finally {
      if (currentRequestId === searchRequestId) {
        isLoading.value = false;
      }
    }
  };

  const selectStudent = (student: StudentProfileResponse) => {
    selectedStudent.value = student;
    successMessage.value = "";
    error.value = null;
  };

  const clearSelectedStudent = () => {
    selectedStudent.value = null;
    error.value = null;
  };

  const submitRequest = async () => {
    if (!selectedStudent.value) {
      error.value = {
        timestamp: new Date().toISOString(),
        status: 0,
        error: "FORM_ERROR",
        message: "Сначала выберите студента.",
        path: "/mentoring/requests"
      };
      return false;
    }

    isLoading.value = true;
    error.value = null;
    successMessage.value = "";

    const payload: MentoringRequestCreateRequest = {
      targetProfileId: selectedStudent.value.id,
      goalType: requestForm.goalType,
      message: requestForm.message
    };

    try {
      await mentoringApi.create(payload);
      requestedStudentIds.value = new Set([...requestedStudentIds.value, selectedStudent.value.id]);
      successMessage.value = "Приглашение отправлено.";
      selectedStudent.value = null;
      return true;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/mentoring/requests");
      return false;
    } finally {
      isLoading.value = false;
    }
  };

  return {
    clearSelectedStudent,
    error,
    isLoading,
    requestForm,
    requestedStudentIds,
    results,
    searchForm,
    loadRequestedStudents,
    searchStudents,
    selectedStudent,
    selectStudent,
    studentsCount,
    submitRequest,
    successMessage
  };
});
