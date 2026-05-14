package com.example.it.mentor.dto.session;

import java.time.OffsetDateTime;

public record NextSessionSummary(
        Long id,
        Long mentoringRequestId,
        OffsetDateTime scheduledAt,
        Integer durationMinutes,
        String counterpartyName
) {
}
