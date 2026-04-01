package com.example.it.mentor.dto;

public record ProfileSummaryResponse(
        String role,
        Long profileId,
        boolean profileExists
) {
}
