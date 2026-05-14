package com.example.it.mentor.dto.dashboard;

import java.time.OffsetDateTime;

public record ActivityItemResponse(
        String type,
        Long referenceId,
        String summary,
        OffsetDateTime occurredAt
) {}
