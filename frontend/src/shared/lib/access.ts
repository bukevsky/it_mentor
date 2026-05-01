import type { RoleCode, UserInfoResponse } from "@/shared/api/contracts";

export type AccessRole = RoleCode;

export const hasAnyRole = (
  user: UserInfoResponse | null | undefined,
  roles?: readonly AccessRole[]
) => {
  if (!roles?.length) {
    return true;
  }

  return Boolean(user?.roles.some((role) => roles.includes(role)));
};

export const getPrimaryWorkspaceRole = (user: UserInfoResponse | null | undefined) => {
  if (user?.roles.includes("ADMIN")) {
    return "ADMIN";
  }

  if (user?.roles.includes("MENTOR")) {
    return "MENTOR";
  }

  if (user?.roles.includes("STUDENT")) {
    return "STUDENT";
  }

  return null;
};
