package com.example.it.mentor.dto.chat;

import java.time.OffsetDateTime;

public record ChatResponse(
        Long id,
        Long mentoringRequestId,
        Long studentUserId,
        Long mentorUserId,
        OffsetDateTime createdAt
) {}
