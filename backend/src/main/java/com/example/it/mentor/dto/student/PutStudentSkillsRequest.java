package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PutStudentSkillsRequest(
        @NotNull List<@NotNull SkillItem> skills
) {
    public record SkillItem(
            @NotNull Long skillId,
            @NotNull SkillLevel level,
            int position
    ) {}
}
