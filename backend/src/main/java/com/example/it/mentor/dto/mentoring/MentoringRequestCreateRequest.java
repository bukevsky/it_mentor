package com.example.it.mentor.dto.mentoring;

import com.example.it.mentor.entity.enums.MentoringType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MentoringRequestCreateRequest(
        @NotNull Long targetProfileId,
        @NotNull MentoringType goalType,
        @NotBlank @Size(max = 2000) String message
) {}
