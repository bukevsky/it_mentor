package com.example.it.mentor.dto.session;

import com.example.it.mentor.entity.enums.MentoringSessionStatus;

import java.time.OffsetDateTime;

public record SessionResponse(
        Long id,
        Long mentoringRequestId,
        Long studentUserId,
        Long mentorUserId,
        String studentName,
        String mentorName,
        OffsetDateTime scheduledAt,
        Integer durationMinutes,
        MentoringSessionStatus status,
        String cancelReason,
        String rescheduleReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
