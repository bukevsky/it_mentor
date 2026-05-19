package com.example.it.mentor.event.audit;

import com.example.it.mentor.entity.UserStatus;

public record UserStatusChangedAuditEvent(
        Long adminUserId,
        Long targetUserId,
        UserStatus oldStatus,
        UserStatus newStatus) implements AuditEvent {}
