import type {
  AdminRoleRequest,
  AdminUserResponse,
  AdminUsersStatsResponse,
  AdminUserStatusRequest,
  AuditAction,
  AuditLogResponse,
  CityResponse,
  ComplaintResponse,
  ComplaintStatus,
  ComplaintTargetType,
  CreateCityRequest,
  CreateInteractionTypeRequest,
  CreateLanguageRequest,
  CreateSkillRequest,
  DictionaryType,
  InteractionTypeResponse,
  LanguageResponse,
  ModerateReviewRequest,
  NotificationOutboxStatus,
  OutboxEntryResponse,
  PagedResponse,
  ResolveComplaintRequest,
  ReviewResponse,
  RoleCode,
  SkillResponse,
  UpdateCityRequest,
  UpdateInteractionTypeRequest,
  UpdateLanguageRequest,
  UpdateSkillRequest,
  UserStatus
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

type DictionaryResponseMap = {
  cities: CityResponse;
  skills: SkillResponse;
  languages: LanguageResponse;
  "interaction-types": InteractionTypeResponse;
};

type DictionaryCreateRequestMap = {
  cities: CreateCityRequest;
  skills: CreateSkillRequest;
  languages: CreateLanguageRequest;
  "interaction-types": CreateInteractionTypeRequest;
};

type DictionaryUpdateRequestMap = {
  cities: UpdateCityRequest;
  skills: UpdateSkillRequest;
  languages: UpdateLanguageRequest;
  "interaction-types": UpdateInteractionTypeRequest;
};

export const adminApi = {
  assignRole(userId: number, payload: AdminRoleRequest) {
    return request<void>(`/admin/users/${userId}/role`, {
      method: "PUT",
      body: payload
    });
  },
  changeUserStatus(userId: number, payload: AdminUserStatusRequest) {
    return request<void>(`/admin/users/${userId}/status`, {
      method: "PUT",
      body: payload
    });
  },
  getUsers(params: { q?: string; role?: RoleCode; status?: UserStatus; page?: number; size?: number } = {}) {
    return request<PagedResponse<AdminUserResponse>>(`/admin/users${buildQuery(params)}`);
  },
  getUsersStats() {
    return request<AdminUsersStatsResponse>("/admin/users/stats");
  },
  getDictionary<T extends DictionaryType>(type: T) {
    return request<Array<DictionaryResponseMap[T]>>(`/admin/dictionaries/${type}`);
  },
  createDictionaryItem<T extends DictionaryType>(type: T, payload: DictionaryCreateRequestMap[T]) {
    return request<DictionaryResponseMap[T]>(`/admin/dictionaries/${type}`, {
      method: "POST",
      body: payload
    });
  },
  updateDictionaryItem<T extends DictionaryType>(type: T, id: number, payload: DictionaryUpdateRequestMap[T]) {
    return request<DictionaryResponseMap[T]>(`/admin/dictionaries/${type}/${id}`, {
      method: "PUT",
      body: payload
    });
  },
  deleteDictionaryItem(type: DictionaryType, id: number) {
    return request<void>(`/admin/dictionaries/${type}/${id}`, {
      method: "DELETE"
    });
  },
  restoreDictionaryItem<T extends DictionaryType>(type: T, id: number) {
    return request<DictionaryResponseMap[T]>(`/admin/dictionaries/${type}/${id}/restore`, {
      method: "PUT"
    });
  },
  getComplaints(
    params: { status?: ComplaintStatus; targetType?: ComplaintTargetType; page?: number; size?: number } = {}
  ) {
    return request<PagedResponse<ComplaintResponse>>(`/admin/complaints${buildQuery(params)}`);
  },
  resolveComplaint(complaintId: number, payload: ResolveComplaintRequest) {
    return request<ComplaintResponse>(`/admin/complaints/${complaintId}/resolve`, {
      method: "PUT",
      body: payload
    });
  },
  moderateReview(reviewId: number, payload: ModerateReviewRequest) {
    return request<ReviewResponse>(`/admin/reviews/${reviewId}/moderate`, {
      method: "PUT",
      body: payload
    });
  },
  getAudit(params: { action?: AuditAction; adminUserId?: number; from?: string; to?: string; page?: number; size?: number } = {}) {
    return request<PagedResponse<AuditLogResponse>>(`/admin/audit${buildQuery(params)}`);
  },
  getOutbox(params: { status?: NotificationOutboxStatus; page?: number; size?: number } = {}) {
    return request<PagedResponse<OutboxEntryResponse>>(`/admin/notifications/outbox${buildQuery(params)}`);
  }
};
