package com.example.it.mentor.event.audit;

public sealed interface AuditEvent
        permits RoleChangedAuditEvent,
                ReviewModeratedAuditEvent,
                ComplaintResolvedAuditEvent,
                UserStatusChangedAuditEvent,
                DictionaryChangedAuditEvent {

    Long adminUserId();
}
