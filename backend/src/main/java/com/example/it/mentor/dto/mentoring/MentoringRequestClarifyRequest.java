package com.example.it.mentor.dto.mentoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MentoringRequestClarifyRequest(
        @NotBlank @Size(max = 2000) String clarificationNote
) {}
