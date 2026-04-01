package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;

public record StudentSkillRequest(
        @NotNull Long skillId,
        @NotNull SkillLevel level
) {
}
