package com.example.it.mentor.dto.student;

public record StudentEducationResponse(
        Long id,
        String institution,
        String specialty,
        String degree,
        String educationForm,
        Integer startYear,
        Integer graduationYear
) {
}
