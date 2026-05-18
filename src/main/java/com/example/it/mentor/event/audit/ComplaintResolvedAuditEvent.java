package com.example.it.mentor.event.audit;

import com.example.it.mentor.entity.enums.ComplaintStatus;

public record ComplaintResolvedAuditEvent(
        Long adminUserId,
        Long complaintId,
        ComplaintStatus status,
        String resolution
) implements AuditEvent {
}
