package com.example.it.mentor.dto.sse;

import java.time.OffsetDateTime;

public record PresencePayload(
        Long userId,
        String status,
        OffsetDateTime lastSeenAt
) {}
