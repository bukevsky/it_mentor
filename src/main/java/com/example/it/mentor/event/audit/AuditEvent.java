package com.example.it.mentor.event.audit;

public sealed interface AuditEvent
        permits RoleChangedAuditEvent, ReviewModeratedAuditEvent, ComplaintResolvedAuditEvent {

    Long adminUserId();
}
