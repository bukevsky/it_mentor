package com.example.it.mentor.dto.student;

import com.example.it.mentor.dto.dict.SkillResponse;

public record StudentSkillResponse(
        Long id,
        SkillResponse skill,
        String level
) {
}
