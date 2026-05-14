package com.example.it.mentor.dto.session;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateSessionRequest(
        @NotNull Long mentoringRequestId,
        @NotNull @Future OffsetDateTime scheduledAt,
        @NotNull @Min(15) @Max(480) Integer durationMinutes
) {
}
