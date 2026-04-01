package com.example.it.mentor.dto.mentoring;

import jakarta.validation.constraints.Size;

public record MentoringRequestRejectRequest(
        @Size(max = 2000) String reason
) {}
