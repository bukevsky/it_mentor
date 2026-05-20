import type {
  ActivityItemResponse,
  DashboardSummaryResponse,
  MentorStatsResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const dashboardApi = {
  getSummary() {
    return request<DashboardSummaryResponse>("/dashboard/summary");
  },
  getActivity(params: { limit?: number } = {}) {
    return request<ActivityItemResponse[]>(`/dashboard/activity${buildQuery(params)}`);
  },
  getMentorStats() {
    return request<MentorStatsResponse>("/mentor-stats/me");
  }
};
