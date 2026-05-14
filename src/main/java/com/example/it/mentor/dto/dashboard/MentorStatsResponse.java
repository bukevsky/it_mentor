package com.example.it.mentor.dto.dashboard;

public record MentorStatsResponse(
        double averageRating,
        int reviewCount,
        int completedRequests,
        double responseRate,
        String level,
        int progress
) {}
