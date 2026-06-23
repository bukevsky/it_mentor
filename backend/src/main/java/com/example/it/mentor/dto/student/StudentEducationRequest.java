package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.EducationDegree;
import com.example.it.mentor.entity.enums.EducationForm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentEducationRequest(
        @NotBlank @Size(max = 255) String institution,
        @Size(max = 255) String specialty,
        EducationDegree degree,
        EducationForm educationForm,
        @Min(1900) @Max(2100) Integer startYear,
        @Min(1900) @Max(2100) Integer graduationYear
) {
}
