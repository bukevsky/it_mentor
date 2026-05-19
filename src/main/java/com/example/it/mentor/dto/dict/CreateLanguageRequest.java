package com.example.it.mentor.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLanguageRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 10) String code) {}
