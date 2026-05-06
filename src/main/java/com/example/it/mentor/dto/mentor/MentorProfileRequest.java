package com.example.it.mentor.dto.mentor;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.example.it.mentor.entity.enums.MentoringChannel;
import com.example.it.mentor.entity.enums.MentoringDuration;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MentorProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String middleName,
        @Size(max = 255) String position,
        @Size(max = 255) String department,
        Long cityId,
        @Size(max = 30) String phone,
        @Size(max = 100) @JsonAlias("max") String maxContact,
        String description,
        String expectations,
        String canHelpWith,
        MentoringType mentoringType,
        MentoringChannel mentoringChannel,
        @Size(max = 100) String mentoringFrequency,
        MentoringDuration mentoringDuration,
        @Min(0) @Max(100) Integer menteeLimit,
        RecruitmentStatus recruitmentStatus,
        @Valid List<MentorSkillRequest> skills
) {
}
