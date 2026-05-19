package com.example.it.mentor.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCityRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String region,
        @NotBlank @Size(max = 100) String country) {}
