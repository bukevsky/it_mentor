package com.example.it.mentor.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCityRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String region,
        @NotBlank @Size(max = 100) String country,
        @NotNull Boolean active) {}
