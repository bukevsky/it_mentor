package com.example.it.mentor.dto.review;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long id,
        Long mentoringRequestId,
        Long reviewerUserId,
        Long mentorUserId,
        int rating,
        String comment,
        OffsetDateTime createdAt
) {
}
