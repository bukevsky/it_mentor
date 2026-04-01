export type UserRole = "STUDENT" | "MENTOR" | "ADMIN";

export type UserStatus =
  | "ACTIVE"
  | "EMAIL_NOT_CONFIRMED"
  | "BLOCKED"
  | "DELETED";

export interface UserInfo {
  id: number;
  email: string;
  roles: UserRole[];
  status: UserStatus;
}
