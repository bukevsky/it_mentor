package com.example.it.mentor.dto.mentor;

import com.example.it.mentor.entity.enums.MentoringChannel;
import com.example.it.mentor.entity.enums.MentoringDuration;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MentorProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        String position,
        String department,
        Long cityId,
        String phone,
        String max,
        String description,
        String expectations,
        String canHelpWith,
        MentoringType mentoringType,
        MentoringChannel mentoringChannel,
        String mentoringFrequency,
        MentoringDuration mentoringDuration,
        @Min(0) @Max(100) Integer menteeLimit,
        RecruitmentStatus recruitmentStatus,
        @Valid List<MentorSkillRequest> skills
) {
}
