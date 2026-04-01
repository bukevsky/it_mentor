package com.example.it.mentor.dto.mentor;

import com.example.it.mentor.entity.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;

public record MentorSkillRequest(
        @NotNull Long skillId,
        @NotNull SkillLevel level
) {
}
