package com.example.it.mentor.dto.audit;

import com.example.it.mentor.entity.enums.AuditAction;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        Long adminUserId,
        AuditAction action,
        String targetType,
        Long targetId,
        String payload,
        OffsetDateTime createdAt
) {
}
