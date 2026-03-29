package com.example.it.mentor.dto.mentoring;

public record MentorProfileShortResponse(
        Long id,
        String firstName,
        String lastName,
        String position
) {}
