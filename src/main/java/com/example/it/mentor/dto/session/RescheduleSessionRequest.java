package com.example.it.mentor.dto.session;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RescheduleSessionRequest(
        @NotNull @Future OffsetDateTime newScheduledAt,
        @NotNull @Min(15) @Max(480) Integer durationMinutes,
        @Size(max = 500) String reason
) {
}
