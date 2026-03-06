package com.example.it.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Токен обязателен")
        String token,

        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 8, message = "Пароль должен содержать не менее 8 символов")
        String newPassword
) {
}
