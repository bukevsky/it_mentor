package com.example.it.mentor.dto.review;

import com.example.it.mentor.entity.enums.ReviewModerationStatus;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long id,
        Long mentoringRequestId,
        Long reviewerUserId,
        Long mentorUserId,
        int rating,
        String comment,
        ReviewModerationStatus moderationStatus,
        OffsetDateTime createdAt
) {
}
