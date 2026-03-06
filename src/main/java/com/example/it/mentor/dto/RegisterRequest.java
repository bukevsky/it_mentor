package com.example.it.mentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, message = "Пароль должен содержать не менее 8 символов")
        String password
) {
}
