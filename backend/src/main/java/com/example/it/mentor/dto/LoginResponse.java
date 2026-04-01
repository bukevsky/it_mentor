package com.example.it.mentor.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UserInfoResponse user
) {
}
