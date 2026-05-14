package com.example.it.mentor.dto.dashboard;

public record DashboardSummaryResponse(
        String role,
        int sentRequests,
        int pendingRequests,
        int acceptedRequests,
        int totalChats,
        int unreadChats,
        int profileCompletion,
        Object nextSession
) {}
