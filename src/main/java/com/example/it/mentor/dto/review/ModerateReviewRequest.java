package com.example.it.mentor.dto.review;

import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import jakarta.validation.constraints.NotNull;

public record ModerateReviewRequest(
        @NotNull ReviewModerationStatus moderationStatus
) {
}
