package com.example.it.mentor.dto.complaint;

import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;

import java.time.OffsetDateTime;

public record ComplaintResponse(
        Long id,
        ComplaintTargetType targetType,
        Long targetId,
        Long reporterUserId,
        String reason,
        ComplaintStatus status,
        String resolution,
        Long resolvedBy,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt
) {
}
