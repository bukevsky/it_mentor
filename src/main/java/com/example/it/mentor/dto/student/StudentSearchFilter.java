package com.example.it.mentor.dto.student;

import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudentSearchFilter(
        String q,
        Long cityId,
        @Size(max = 20) List<Long> skillIds,
        EmploymentType employmentType,
        WorkFormat workFormat
) {}
