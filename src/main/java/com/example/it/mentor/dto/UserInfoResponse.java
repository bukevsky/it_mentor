package com.example.it.mentor.dto;

import java.util.List;

public record UserInfoResponse(
        Long id,
        String email,
        List<String> roles,
        String status
) {
}
