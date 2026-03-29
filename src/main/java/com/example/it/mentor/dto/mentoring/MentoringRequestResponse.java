package com.example.it.mentor.dto.mentoring;

import java.time.OffsetDateTime;

public record MentoringRequestResponse(
        Long id,
        Long studentProfileId,
        Long mentorProfileId,
        StudentProfileShortResponse studentProfile,
        MentorProfileShortResponse mentorProfile,
        String direction,
        String status,
        String goalType,
        String message,
        String clarificationNote,
        String reason,
        OffsetDateTime createdAt,
        OffsetDateTime respondedAt,
        OffsetDateTime completedAt
) {}
