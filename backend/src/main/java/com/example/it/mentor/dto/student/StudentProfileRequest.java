package com.example.it.mentor.dto.student;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record StudentProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String middleName,
        @Size(max = 30) String phone,
        Long cityId,
        @Size(max = 255) String desiredPosition,
        @Min(0) @Max(168) Integer hoursPerWeek,
        LocalDate availableFrom,
        String about,
        @Size(max = 100) @JsonAlias("max") String maxContact,
        Set<EmploymentType> employmentTypes,
        Set<WorkFormat> workFormats,
        @Valid List<StudentEducationRequest> educations,
        @Valid List<StudentLanguageRequest> languages,
        @Valid List<StudentSkillRequest> skills
) {
}
