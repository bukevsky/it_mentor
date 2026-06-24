export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}

export interface HealthComponentStatus {
  status: string;
}

export interface HealthResponse {
  status: string;
  components?: {
    db?: HealthComponentStatus;
    readinessState?: HealthComponentStatus;
    livenessState?: HealthComponentStatus;
    mail?: HealthComponentStatus;
    [key: string]: HealthComponentStatus | undefined;
  };
}

export type UserStatus = "ACTIVE" | "EMAIL_NOT_CONFIRMED" | "BLOCKED" | "DELETED";
export type RoleCode = "STUDENT" | "MENTOR" | "ADMIN";

export type EducationDegree = "BACHELOR" | "SPECIALIST" | "MASTER" | "COURSE" | "OTHER";
export type EducationForm = "FULL_TIME" | "PART_TIME" | "DISTANCE";
export type EmploymentType =
  | "PRACTICE"
  | "INTERNSHIP"
  | "PART_TIME"
  | "FULL_TIME"
  | "PROJECT"
  | "OTHER";
export type WorkFormat = "REMOTE" | "OFFICE" | "HYBRID";
export type LanguageLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2" | "NATIVE";
export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "CONFIDENT";

export type MentoringType = "PRACTICE" | "INTERNSHIP" | "PROJECT";
export type MentoringChannel = "CHAT" | "CALLS" | "MIXED";
export type MentoringDuration = "ONE_MONTH" | "THREE_MONTHS" | "FLEXIBLE";
export type RecruitmentStatus = "OPEN" | "PAUSED" | "CLOSED";

export type MentoringRequestDirection = "STUDENT_TO_MENTOR" | "MENTOR_TO_STUDENT";
export type MentoringRequestStatus =
  | "SENT"
  | "REVIEWING"
  | "NEEDS_CLARIFICATION"
  | "ACCEPTED"
  | "REJECTED"
  | "CANCELLED"
  | "COMPLETED";

export type FileType = "RESUME" | "PORTFOLIO" | "CHAT_ATTACHMENT" | "AVATAR";
export type FileStatus = "ACTIVE" | "DELETED";
export type MentoringSessionStatus = "SCHEDULED" | "RESCHEDULED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type ReviewModerationStatus = "VISIBLE" | "HIDDEN" | "UNDER_REVIEW";
export type ComplaintStatus = "OPEN" | "RESOLVED" | "REJECTED";
export type ComplaintTargetType = "REVIEW" | "USER";
export type NotificationOutboxStatus = "PENDING" | "SENT" | "FAILED";
export type AuditAction =
  | "ROLE_CHANGED"
  | "REVIEW_MODERATED"
  | "COMPLAINT_RESOLVED"
  | "USER_STATUS_CHANGED"
  | "DICTIONARY_CHANGED";
export type DictionaryType = "cities" | "skills" | "languages" | "interaction-types";
export type DictionaryOperation = "CREATE" | "UPDATE" | "DEACTIVATE" | "RESTORE";

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface RegisterResponse {
  id: number;
  email: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserInfoResponse {
  id: number;
  email: string;
  roles: RoleCode[];
  status: UserStatus;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  user: UserInfoResponse;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface ProfileSummaryResponse {
  role: Exclude<RoleCode, never>;
  profileId: number | null;
  profileExists: boolean;
}

export interface CityResponse {
  id: number;
  name: string;
  region: string | null;
  country: string;
  active: boolean;
}

export interface SkillResponse {
  id: number;
  name: string;
  category: string | null;
  active: boolean;
}

export interface LanguageResponse {
  id: number;
  name: string;
  code: string;
  active: boolean;
}

export interface InteractionTypeResponse {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
}

export interface CreateCityRequest {
  name: string;
  region?: string | null;
  country: string;
}

export interface UpdateCityRequest extends CreateCityRequest {
  active: boolean;
}

export interface CreateSkillRequest {
  name: string;
  category?: string | null;
}

export interface UpdateSkillRequest extends CreateSkillRequest {
  active: boolean;
}

export interface CreateLanguageRequest {
  name: string;
  code: string;
}

export interface UpdateLanguageRequest extends CreateLanguageRequest {
  active: boolean;
}

export interface CreateInteractionTypeRequest {
  name: string;
  description?: string | null;
}

export interface UpdateInteractionTypeRequest extends CreateInteractionTypeRequest {
  active: boolean;
}

export interface StudentEducationRequest {
  institution: string;
  specialty?: string | null;
  degree?: EducationDegree | null;
  educationForm?: EducationForm | null;
  startYear?: number | null;
  graduationYear?: number | null;
}

export interface StudentLanguageRequest {
  languageId: number;
  level: LanguageLevel;
  position?: number;
}

export interface StudentSkillRequest {
  skillId: number;
  level: SkillLevel;
  position?: number;
}

export interface StudentProfileRequest {
  firstName: string;
  lastName: string;
  middleName?: string | null;
  phone?: string | null;
  cityId?: number | null;
  desiredPosition?: string | null;
  hoursPerWeek?: number | null;
  availableFrom?: string | null;
  about?: string | null;
  max?: string | null;
  employmentTypes?: EmploymentType[] | null;
  workFormats?: WorkFormat[] | null;
  educations?: StudentEducationRequest[] | null;
  languages?: StudentLanguageRequest[] | null;
  skills?: StudentSkillRequest[] | null;
}

export interface StudentEducationResponse {
  id: number;
  institution: string;
  specialty: string | null;
  degree: string | null;
  educationForm: string | null;
  startYear: number | null;
  graduationYear: number | null;
}

export interface StudentLanguageResponse {
  id: number;
  language: LanguageResponse;
  level: string;
  position: number;
}

export interface StudentSkillResponse {
  id: number;
  skill: SkillResponse;
  level: string;
  position: number;
}

export interface StudentProfileResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  phone: string | null;
  city: CityResponse | null;
  desiredPosition: string | null;
  hoursPerWeek: number | null;
  availableFrom: string | null;
  about: string | null;
  max: string | null;
  employmentTypes: string[];
  workFormats: string[];
  educations: StudentEducationResponse[];
  languages: StudentLanguageResponse[];
  skills: StudentSkillResponse[];
  resumeFileId: number | null;
}

export interface PatchStudentProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  middleName?: string | null;
  phone?: string | null;
  desiredPosition?: string | null;
  hoursPerWeek?: number | null;
  availableFrom?: string | null;
  about?: string | null;
  maxContact?: string | null;
  cityId?: number | null;
  employmentTypes?: EmploymentType[] | null;
  workFormats?: WorkFormat[] | null;
}

export interface PutStudentSkillsRequest {
  skills: Array<{
    skillId: number;
    level: SkillLevel;
    position: number;
  }>;
}

export interface PutStudentLanguagesRequest {
  languages: Array<{
    languageId: number;
    level: LanguageLevel;
    position: number;
  }>;
}

export interface StudentCompletionResponse {
  percent: number;
  mainDone: boolean;
  aboutDone: boolean;
  skillsDone: boolean;
  resumeDone: boolean;
}

export interface MentorSkillRequest {
  skillId: number;
  level: SkillLevel;
}

export interface MentorProfileRequest {
  firstName: string;
  lastName: string;
  middleName?: string | null;
  position?: string | null;
  department?: string | null;
  cityId?: number | null;
  phone?: string | null;
  max?: string | null;
  description?: string | null;
  expectations?: string | null;
  canHelpWith?: string | null;
  mentoringType?: MentoringType | null;
  mentoringChannel?: MentoringChannel | null;
  mentoringFrequency?: string | null;
  mentoringDuration?: MentoringDuration | null;
  menteeLimit?: number | null;
  recruitmentStatus?: RecruitmentStatus | null;
  skills?: MentorSkillRequest[] | null;
}

export interface MentorSkillResponse {
  id: number;
  skill: SkillResponse;
  level: string;
}

export interface MentorProfileResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  position: string | null;
  department: string | null;
  city: CityResponse | null;
  phone: string | null;
  max: string | null;
  description: string | null;
  expectations: string | null;
  canHelpWith: string | null;
  mentoringType: string | null;
  mentoringChannel: string | null;
  mentoringFrequency: string | null;
  mentoringDuration: string | null;
  menteeLimit: number | null;
  recruitmentStatus: string;
  skills: MentorSkillResponse[];
}

export interface MentorCardResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  position: string | null;
  department: string | null;
  city: CityResponse | null;
  mentoringType: string | null;
  mentoringChannel: string | null;
  mentoringDuration: string | null;
  menteeLimit: number | null;
  recruitmentStatus: string;
  skills: MentorSkillResponse[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface MentorSearchParams {
  q?: string;
  skillIds?: number[];
  cityId?: number;
  recruitmentStatus?: RecruitmentStatus;
  mentoringType?: MentoringType;
  mentoringChannel?: MentoringChannel;
  page?: number;
  size?: number;
  sort?: string;
}

export interface StudentSearchParams {
  q?: string;
  skillIds?: number[];
  cityId?: number;
  employmentType?: EmploymentType;
  workFormat?: WorkFormat;
  page?: number;
  size?: number;
  sort?: string;
}

export interface MentoringRequestCreateRequest {
  targetProfileId: number;
  goalType: MentoringType;
  message: string;
}

export interface MentoringRequestClarifyRequest {
  clarificationNote: string;
}

export interface MentoringRequestRejectRequest {
  reason?: string | null;
}

export interface StudentProfileShortResponse {
  id: number;
  firstName: string;
  lastName: string;
}

export interface MentorProfileShortResponse {
  id: number;
  firstName: string;
  lastName: string;
  position: string | null;
}

export interface MentoringRequestResponse {
  id: number;
  studentProfileId: number;
  mentorProfileId: number;
  studentProfile: StudentProfileShortResponse;
  mentorProfile: MentorProfileShortResponse;
  direction: MentoringRequestDirection;
  status: MentoringRequestStatus;
  goalType: string;
  message: string;
  clarificationNote: string | null;
  reason: string | null;
  createdAt: string;
  respondedAt: string | null;
  completedAt: string | null;
}

export interface ReviewResponse {
  id: number;
  mentoringRequestId: number;
  reviewerUserId: number;
  mentorUserId: number;
  rating: number;
  comment: string | null;
  moderationStatus: ReviewModerationStatus;
  createdAt: string;
}

export interface ReviewSummaryResponse {
  averageRating: number;
  totalReviews: number;
  distribution: Record<1 | 2 | 3 | 4 | 5, number>;
}

export interface ReviewCreateRequest {
  mentoringRequestId: number;
  rating: number;
  comment?: string | null;
}

export interface ReviewableRequest {
  id: number;
  mentorProfileId: number;
  mentorName: string;
  goalType: string;
  completedAt: string | null;
}

export interface NextSessionSummary {
  id: number;
  mentoringRequestId: number;
  scheduledAt: string;
  durationMinutes: number | null;
  counterpartyName: string;
}

export interface CreateSessionRequest {
  mentoringRequestId: number;
  scheduledAt: string;
  durationMinutes: number;
}

export interface RescheduleSessionRequest {
  newScheduledAt: string;
  durationMinutes: number;
  reason?: string | null;
}

export interface CancelSessionRequest {
  reason?: string | null;
}

export interface SessionResponse {
  id: number;
  mentoringRequestId: number;
  studentUserId: number;
  mentorUserId: number;
  studentName: string;
  mentorName: string;
  scheduledAt: string;
  durationMinutes: number;
  status: MentoringSessionStatus;
  cancelReason: string | null;
  rescheduleReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummaryResponse {
  role: string;
  sentRequests: number;
  pendingRequests: number;
  acceptedRequests: number;
  totalChats: number;
  unreadChats: number;
  profileCompletion: number;
  nextSession: NextSessionSummary | null;
}

export interface ActivityItemResponse {
  type: string;
  referenceId: number;
  summary: string;
  occurredAt: string;
}

export interface MentorStatsResponse {
  averageRating: number;
  reviewCount: number;
  completedRequests: number;
  responseRate: number;
  level: string;
  progress: number;
}

export interface FileUploadResponse {
  id: number;
  originalFilename: string;
  contentType: string;
  size: number;
  fileType: FileType;
  uploadedAt: string;
}

export interface FileResponse {
  id: number;
  originalFilename: string;
  contentType: string;
  size: number;
  fileType: FileType;
  status: FileStatus;
  previewUrl: string | null;
  uploadedAt: string;
}

export interface StudentFilesResponse {
  resume: FileResponse | null;
  portfolioCount: number;
  avatarFileId: number | null;
}

export interface AttachmentInfo {
  fileId: number;
  originalFilename: string;
  contentType: string;
  size: number;
}

export interface ChatResponse {
  id: number;
  mentoringRequestId: number;
  studentUserId: number;
  mentorUserId: number;
  createdAt: string;
  lastMessage: ChatMessageResponse | null;
  lastMessageAt: string | null;
  lastSenderUserId: number | null;
  unreadCount: number;
  studentName: string | null;
  mentorName: string | null;
  mentoringRequestStatus: MentoringRequestStatus | null;
}

export interface ChatMessageResponse {
  id: number;
  chatId: number;
  senderUserId: number;
  clientMessageId: string;
  body: string | null;
  attachment: AttachmentInfo | null;
  deliveryStatus: ChatMessageDeliveryStatus;
  deliveredAt: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface SendMessageRequest {
  body?: string | null;
  attachmentFileId?: number | null;
}

export interface TypingRequest {
  typing: boolean;
}

export type ChatMessageDeliveryStatus = "SENT" | "DELIVERED" | "READ";

export interface SendMessageCommand {
  requestId: string;
  body: string | null;
  attachmentFileId: number | null;
}

export interface MessageStatusCommand {
  requestId: string;
  upToMessageId: number;
}

export interface TypingCommand {
  requestId: string;
  typing: boolean;
}

export interface ChatCommandAckPayload {
  command: "send" | "delivered" | "read" | "typing";
  resourceId: number | null;
  duplicate: boolean;
}

export interface ChatCommandErrorPayload {
  code:
    | "NOT_FOUND"
    | "FORBIDDEN"
    | "CONFLICT"
    | "BUSINESS_RULE_VIOLATION"
    | "VALIDATION_ERROR"
    | "INTERNAL_ERROR";
  message: string;
  fieldErrors: Record<string, string>;
}

export interface MessageStatusChangedPayload {
  actorUserId: number;
  upToMessageId: number;
  status: "DELIVERED" | "READ";
  changedAt: string;
  changedCount: number;
}

export interface ChatTypingPayload {
  chatId: number;
  userId: number;
  typing: boolean;
}

export interface PresenceChangedPayload {
  userId: number;
  status: "online" | "offline";
  lastSeenAt: string | null;
}

export type ChatEventEnvelope =
  | {
      type: "chat.command.ack";
      requestId: string;
      chatId: number;
      occurredAt: string;
      payload: ChatCommandAckPayload;
    }
  | {
      type: "chat.command.error";
      requestId: string | null;
      chatId: number | null;
      occurredAt: string;
      payload: ChatCommandErrorPayload;
    }
  | {
      type: "chat.message.created";
      requestId: string;
      chatId: number;
      occurredAt: string;
      payload: ChatMessageResponse;
    }
  | {
      type: "chat.message.status.changed";
      requestId: string;
      chatId: number;
      occurredAt: string;
      payload: MessageStatusChangedPayload;
    }
  | {
      type: "chat.typing";
      requestId: string;
      chatId: number;
      occurredAt: string;
      payload: ChatTypingPayload;
    }
  | {
      type: "presence.changed";
      requestId: null;
      chatId: null;
      occurredAt: string;
      payload: PresenceChangedPayload;
    };

export interface AdminRoleRequest {
  role: "STUDENT" | "MENTOR";
}

export interface AdminUserStatusRequest {
  status: Extract<UserStatus, "ACTIVE" | "BLOCKED" | "DELETED">;
}

export interface AdminUserResponse {
  id: number;
  email: string;
  status: UserStatus;
  roles: RoleCode[];
  firstName: string | null;
  lastName: string | null;
  createdAt: string;
}

export interface AdminUsersStatsResponse {
  totalUsers: number;
  byRole: Record<string, number>;
  byStatus: Record<string, number>;
}

export interface NotificationPreferencesResponse {
  emailRequestEvents: boolean;
  emailSessionEvents: boolean;
  emailReviewEvents: boolean;
}

export interface UpdateNotificationPreferencesRequest {
  emailRequestEvents?: boolean | null;
  emailSessionEvents?: boolean | null;
  emailReviewEvents?: boolean | null;
}

export interface MentorStatsResponse {
  averageRating: number | null;
  reviewCount: number;
  completedRequests: number;
  responseRate: number;
  level: "BEGINNER" | "INTERMEDIATE" | "EXPERT";
  progress: number;
}

export interface CreateComplaintRequest {
  targetType: ComplaintTargetType;
  targetId: number;
  reason: string;
}

export interface ResolveComplaintRequest {
  status: Extract<ComplaintStatus, "RESOLVED" | "REJECTED">;
  resolution?: string | null;
}

export interface ComplaintResponse {
  id: number;
  targetType: ComplaintTargetType;
  targetId: number;
  reporterUserId: number;
  reason: string;
  status: ComplaintStatus;
  resolution: string | null;
  resolvedBy: number | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface ModerateReviewRequest {
  moderationStatus: ReviewModerationStatus;
}

export interface AuditLogResponse {
  id: number;
  adminUserId: number;
  action: AuditAction;
  targetType: string;
  targetId: number | null;
  payload: string | null;
  createdAt: string;
}

export interface OutboxEntryResponse {
  id: number;
  recipientEmail: string;
  subject: string;
  eventType: string;
  status: NotificationOutboxStatus;
  attempts: number;
  lastError: string | null;
  nextAttemptAt: string | null;
  sentAt: string | null;
  createdAt: string;
}

export interface PresenceResponse {
  userId: number;
  status: "online" | "offline";
  lastSeenAt: string | null;
}
