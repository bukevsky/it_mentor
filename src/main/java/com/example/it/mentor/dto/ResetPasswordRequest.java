package com.example.it.mentor.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Код обязателен")
        @Pattern(regexp = "\\d{6}", message = "Код должен содержать ровно 6 цифр")
        String code,

        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 8, max = 128, message = "Пароль должен содержать от 8 до 128 символов")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Пароль должен содержать минимум одну заглавную букву, одну строчную и одну цифру")
        String newPassword
) {
}
