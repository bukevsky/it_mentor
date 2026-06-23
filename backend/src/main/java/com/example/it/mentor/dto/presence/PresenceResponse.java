package com.example.it.mentor.dto.presence;

import java.time.OffsetDateTime;

public record PresenceResponse(
        Long userId,
        String status,
        OffsetDateTime lastSeenAt
) {}
