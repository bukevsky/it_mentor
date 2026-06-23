package com.example.it.mentor.event.audit;

import com.example.it.mentor.entity.enums.ReviewModerationStatus;

public record ReviewModeratedAuditEvent(
        Long adminUserId,
        Long reviewId,
        ReviewModerationStatus newStatus
) implements AuditEvent {
}
