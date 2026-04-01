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
  region: string;
  country: string;
}

export interface SkillResponse {
  id: number;
  name: string;
  category: string;
}

export interface LanguageResponse {
  id: number;
  name: string;
  code: string;
}

export interface InteractionTypeResponse {
  id: number;
  name: string;
  description: string;
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
}

export interface StudentSkillRequest {
  skillId: number;
  level: SkillLevel;
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
}

export interface StudentSkillResponse {
  id: number;
  skill: SkillResponse;
  level: string;
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

export interface FileUploadResponse {
  id: number;
  originalFilename: string;
  contentType: string;
  size: number;
  fileType: FileType;
  uploadedAt: string;
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
}

export interface ChatMessageResponse {
  id: number;
  chatId: number;
  senderUserId: number;
  body: string | null;
  attachment: AttachmentInfo | null;
  createdAt: string;
}

export interface SendMessageRequest {
  body?: string | null;
  attachmentFileId?: number | null;
}

export interface AdminRoleRequest {
  role: "STUDENT" | "MENTOR";
}
