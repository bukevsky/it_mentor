package com.example.it.mentor.dto.dashboard;

import com.example.it.mentor.dto.session.NextSessionSummary;

public record DashboardSummaryResponse(
        String role,
        int sentRequests,
        int pendingRequests,
        int acceptedRequests,
        int totalChats,
        int unreadChats,
        int profileCompletion,
        NextSessionSummary nextSession
) {}
