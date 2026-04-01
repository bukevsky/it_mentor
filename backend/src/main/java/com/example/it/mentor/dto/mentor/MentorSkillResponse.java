package com.example.it.mentor.dto.mentor;

import com.example.it.mentor.dto.dict.SkillResponse;

public record MentorSkillResponse(
        Long id,
        SkillResponse skill,
        String level
) {
}
