package com.example.it.mentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email
) {
}
