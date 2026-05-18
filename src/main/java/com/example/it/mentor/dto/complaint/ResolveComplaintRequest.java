package com.example.it.mentor.dto.complaint;

import com.example.it.mentor.entity.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveComplaintRequest(
        @NotNull ComplaintStatus status,
        @Size(max = 2000) String resolution
) {
}
