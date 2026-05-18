package com.example.it.mentor.event.audit;

import com.example.it.mentor.entity.RoleCode;

public record RoleChangedAuditEvent(
        Long adminUserId,
        Long targetUserId,
        RoleCode oldRole,
        RoleCode newRole
) implements AuditEvent {
}
