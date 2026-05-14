package com.example.it.mentor.dto.admin;

import java.util.Map;

public record AdminUsersStatsResponse(
        long totalUsers,
        Map<String, Long> byRole,
        Map<String, Long> byStatus
) {}
