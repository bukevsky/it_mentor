package com.example.it.mentor.dto;

import java.util.List;

public record RegisterResponse(
        Long id,
        String email,
        List<String> roles
) {
}
