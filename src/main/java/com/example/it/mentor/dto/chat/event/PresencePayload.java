package com.example.it.mentor.dto.chat.event;

import java.time.OffsetDateTime;

public record PresencePayload(Long userId, String status, OffsetDateTime lastSeenAt) {
}
