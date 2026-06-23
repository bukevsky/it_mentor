package com.example.it.mentor.dto.session;

import jakarta.validation.constraints.Size;

public record CancelSessionRequest(
        @Size(max = 500) String reason
) {
}
