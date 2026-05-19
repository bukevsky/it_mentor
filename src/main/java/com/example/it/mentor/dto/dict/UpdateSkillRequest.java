package com.example.it.mentor.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String category,
        @NotNull Boolean active) {}
