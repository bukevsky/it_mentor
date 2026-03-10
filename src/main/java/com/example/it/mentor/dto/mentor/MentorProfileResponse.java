package com.example.it.mentor.dto.mentor;

import com.example.it.mentor.dto.dict.CityResponse;

import java.util.List;

public record MentorProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String middleName,
        String position,
        String department,
        CityResponse city,
        String phone,
        String max,
        String description,
        String expectations,
        String canHelpWith,
        String mentoringType,
        String mentoringChannel,
        String mentoringFrequency,
        String mentoringDuration,
        Integer menteeLimit,
        String recruitmentStatus,
        List<MentorSkillResponse> skills
) {
}
