package com.example.it.mentor.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInteractionTypeRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull Boolean active) {}
