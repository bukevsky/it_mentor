package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.LanguageLevel;
import jakarta.validation.constraints.NotNull;

public record StudentLanguageRequest(
        @NotNull Long languageId,
        @NotNull LanguageLevel level
) {
}
