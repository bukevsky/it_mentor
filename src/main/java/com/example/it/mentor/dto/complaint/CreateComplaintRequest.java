package com.example.it.mentor.dto.complaint;

import com.example.it.mentor.entity.enums.ComplaintTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotNull ComplaintTargetType targetType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 2000) String reason
) {
}
