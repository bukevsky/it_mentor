package com.example.it.mentor.dto.mentor;

import com.example.it.mentor.entity.enums.MentoringChannel;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;

import java.util.List;

public record MentorSearchFilter(
        String q,
        List<Long> skillIds,
        Long cityId,
        RecruitmentStatus recruitmentStatus,
        MentoringType mentoringType,
        MentoringChannel mentoringChannel
) {
}
