package com.example.it.mentor.dto.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String email,
        String status,
        List<String> roles,
        String firstName,
        String lastName,
        OffsetDateTime createdAt
) {}
