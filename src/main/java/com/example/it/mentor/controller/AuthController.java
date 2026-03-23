package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.service.AuthService;
import com.example.it.mentor.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для аутентификации, регистрации и управления паролем.
 *
 * <p>Все эндпоинты ({@code /auth/**}) публичны и не требуют JWT-токена.</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация и регистрация")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Регистрирует нового пользователя.
     *
     * @param request данные для регистрации (email, пароль, роль)
     * @return информация о созданном пользователе
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Выполняет вход и возвращает JWT-токен.
     *
     * @param request учётные данные (email, пароль)
     * @return JWT-токен для последующих запросов
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Возвращает информацию о текущем аутентифицированном пользователе.
     *
     * @return профиль текущего пользователя
     */
    @GetMapping("/me")
    public UserInfoResponse me() {
        return userService.getCurrentUser();
    }

    /**
     * Инициирует процедуру восстановления пароля — отправляет токен на email.
     *
     * @param request email пользователя
     */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.OK)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    /**
     * Устанавливает новый пароль по токену из письма.
     *
     * @param request токен сброса и новый пароль
     */
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
