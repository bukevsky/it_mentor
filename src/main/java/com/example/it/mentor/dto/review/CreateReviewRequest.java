package com.example.it.mentor.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull Long mentoringRequestId,
        @Min(1) @Max(5) int rating,
        @Size(max = 2000) String comment
) {
}
