package com.example.it.mentor.dto.notification;

import com.example.it.mentor.entity.enums.NotificationOutboxStatus;

import java.time.OffsetDateTime;

public record OutboxEntryResponse(
        Long id,
        String recipientEmail,
        String subject,
        String eventType,
        NotificationOutboxStatus status,
        int attempts,
        String lastError,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {}
