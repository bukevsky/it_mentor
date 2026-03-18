package com.example.it.mentor.dto.student;

import com.example.it.mentor.dto.dict.LanguageResponse;

public record StudentLanguageResponse(
        Long id,
        LanguageResponse language,
        String level
) {
}
