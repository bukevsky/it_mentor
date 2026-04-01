package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record StudentProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        String phone,
        Long cityId,
        String desiredPosition,
        @Min(0) @Max(168) Integer hoursPerWeek,
        LocalDate availableFrom,
        String about,
        String max,
        Set<EmploymentType> employmentTypes,
        Set<WorkFormat> workFormats,
        @Valid List<StudentEducationRequest> educations,
        @Valid List<StudentLanguageRequest> languages,
        @Valid List<StudentSkillRequest> skills
) {
}
