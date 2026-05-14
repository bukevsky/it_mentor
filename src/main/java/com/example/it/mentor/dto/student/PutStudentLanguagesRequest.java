package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.LanguageLevel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PutStudentLanguagesRequest(
        @NotNull List<@NotNull LanguageItem> languages
) {
    public record LanguageItem(
            @NotNull Long languageId,
            @NotNull LanguageLevel level,
            int position
    ) {}
}
