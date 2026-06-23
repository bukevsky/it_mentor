package com.example.it.mentor.dto.student;

import com.example.it.mentor.dto.dict.CityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record StudentProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String middleName,
        String phone,
        CityResponse city,
        String desiredPosition,
        Integer hoursPerWeek,
        LocalDate availableFrom,
        String about,
        String maxContact,
        Set<String> employmentTypes,
        Set<String> workFormats,
        List<StudentEducationResponse> educations,
        List<StudentLanguageResponse> languages,
        List<StudentSkillResponse> skills,
        Long resumeFileId
) {
}
