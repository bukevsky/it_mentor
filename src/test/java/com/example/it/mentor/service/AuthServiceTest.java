package com.example.it.mentor.service;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.entity.*;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.exception.UnauthorizedException;
import com.example.it.mentor.mapper.AuthMapper;
import com.example.it.mentor.repository.PasswordResetTokenRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserService userService;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private AuthMapper authMapper;

    // ── register ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("новый email → возвращает RegisterResponse")
        void newEmail_shouldReturnRegisterResponse() {
            var request = new RegisterRequest("User@Example.com", "password123", "Иван", "Иванов");
            var role = buildRole(RoleCode.STUDENT);
            var savedUser = buildUser("user@example.com", UserStatus.EMAIL_NOT_CONFIRMED);
            var expected = new RegisterResponse(1L, "user@example.com", List.of("STUDENT"));

            when(userService.existsByEmail("user@example.com")).thenReturn(false);
            when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("password123")).thenReturn("hashed");
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(studentProfileRepository.save(any(StudentProfile.class)))
                    .thenReturn(StudentProfile.builder().build());
            when(authMapper.toRegisterResponse(savedUser)).thenReturn(expected);

            var result = authService.register(request);

            assertThat(result)
                    .as("Ответ не должен быть null")
                    .isNotNull();
            assertThat(result.email()).isEqualTo("user@example.com");
            assertThat(result.roles()).contains("STUDENT");
        }

        @Test
        @DisplayName("email нормализуется в нижний регистр перед проверкой дубликата")
        void emailNormalization_shouldLowercaseBeforeExistsCheck() {
            var request = new RegisterRequest("USER@EXAMPLE.COM", "password123", "Иван", "Иванов");
            when(userService.existsByEmail("user@example.com")).thenReturn(false);
            when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(buildRole(RoleCode.STUDENT)));
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            var savedUser = buildUser("user@example.com", UserStatus.EMAIL_NOT_CONFIRMED);
            when(userService.save(any())).thenReturn(savedUser);
            when(studentProfileRepository.save(any())).thenReturn(StudentProfile.builder().build());
            when(authMapper.toRegisterResponse(any())).thenReturn(
                    new RegisterResponse(1L, "user@example.com", List.of("STUDENT")));

            authService.register(request);

            // Проверяем, что existsByEmail вызван с нормализованным email
            verify(userService).existsByEmail("user@example.com");
            // а не с оригинальным верхним регистром
            verify(userService, never()).existsByEmail("USER@EXAMPLE.COM");
        }

        @Test
        @DisplayName("дублирующий email → ConflictException, сохранение не происходит")
        void duplicateEmail_shouldThrowConflictAndNeverSave() {
            when(userService.existsByEmail("user@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    authService.register(new RegisterRequest("user@example.com", "password123", "Иван", "Иванов")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email уже зарегистрирован");

            verify(userService, never()).save(any());
            verify(studentProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("дублирующий email с другим регистром → ConflictException (проверка нормализации)")
        void duplicateEmailUpperCase_shouldThrowConflict() {
            when(userService.existsByEmail("user@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    authService.register(new RegisterRequest("User@Example.COM", "password123", "Иван", "Иванов")))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("роль STUDENT не найдена в БД → IllegalStateException")
        void roleStudentNotFound_shouldThrowIllegalState() {
            when(userService.existsByEmail("user@example.com")).thenReturn(false);
            when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.register(new RegisterRequest("user@example.com", "password123", "Иван", "Иванов")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Роль STUDENT не найдена");
        }

        @Test
        @DisplayName("при создании профиля StudentProfile сохраняется с firstName и lastName из запроса")
        void register_shouldCreateStudentProfileWithCorrectNames() {
            var request = new RegisterRequest("user@example.com", "password123", "Иван", "Иванов");
            var role = buildRole(RoleCode.STUDENT);
            var savedUser = buildUser("user@example.com", UserStatus.EMAIL_NOT_CONFIRMED);

            when(userService.existsByEmail("user@example.com")).thenReturn(false);
            when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userService.save(any())).thenReturn(savedUser);
            when(studentProfileRepository.save(any())).thenReturn(StudentProfile.builder().build());
            when(authMapper.toRegisterResponse(any())).thenReturn(
                    new RegisterResponse(1L, "user@example.com", List.of("STUDENT")));

            authService.register(request);

            ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
            verify(studentProfileRepository).save(profileCaptor.capture());

            assertThat(profileCaptor.getValue().getFirstName()).isEqualTo("Иван");
            assertThat(profileCaptor.getValue().getLastName()).isEqualTo("Иванов");
        }

        @Test
        @DisplayName("пароль кодируется перед сохранением пользователя")
        void register_shouldEncodePasswordBeforeSaving() {
            var request = new RegisterRequest("user@example.com", "plainpassword", "Иван", "Иванов");
            var role = buildRole(RoleCode.STUDENT);
            var savedUser = buildUser("user@example.com", UserStatus.EMAIL_NOT_CONFIRMED);

            when(userService.existsByEmail("user@example.com")).thenReturn(false);
            when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$bcrypt_hashed");
            when(userService.save(any())).thenReturn(savedUser);
            when(studentProfileRepository.save(any())).thenReturn(StudentProfile.builder().build());
            when(authMapper.toRegisterResponse(any())).thenReturn(
                    new RegisterResponse(1L, "user@example.com", List.of("STUDENT")));

            authService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userService).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash())
                    .as("Пароль должен быть закодирован, а не храниться в открытом виде")
                    .isEqualTo("$2a$bcrypt_hashed")
                    .isNotEqualTo("plainpassword");
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("корректные учётные данные → JWT + tokenType Bearer + user info")
        void correctCredentials_shouldReturnTokenAndUserInfo() {
            var request = new LoginRequest("user@example.com", "password123");
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            var userInfo = new UserInfoResponse(1L, "user@example.com", List.of("STUDENT"), "ACTIVE");

            when(userService.findByEmail("user@example.com")).thenReturn(user);
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtProvider.generateToken("user@example.com")).thenReturn("jwt-token");
            when(authMapper.toUserInfoResponse(user)).thenReturn(userInfo);

            var result = authService.login(request);

            assertThat(result.accessToken()).as("accessToken").isEqualTo("jwt-token");
            assertThat(result.tokenType()).as("tokenType").isEqualTo("Bearer");
            assertThat(result.user()).as("user info").isNotNull();
            assertThat(result.user().email()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("email нормализуется в нижний регистр при поиске пользователя")
        void emailNormalization_shouldLowercaseBeforeLookup() {
            var request = new LoginRequest("USER@EXAMPLE.COM", "password123");
            var user = buildUser("user@example.com", UserStatus.ACTIVE);

            when(userService.findByEmail("user@example.com")).thenReturn(user);
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtProvider.generateToken("user@example.com")).thenReturn("token");
            when(authMapper.toUserInfoResponse(user)).thenReturn(
                    new UserInfoResponse(1L, "user@example.com", List.of("STUDENT"), "ACTIVE"));

            authService.login(request);

            verify(userService).findByEmail("user@example.com");
            verify(userService, never()).findByEmail("USER@EXAMPLE.COM");
        }

        @Test
        @DisplayName("пользователь не найден → UnauthorizedException с маскированным сообщением")
        void userNotFound_shouldThrowUnauthorizedWithMaskedMessage() {
            when(userService.findByEmail(anyString())).thenThrow(new NotFoundException("не найден"));

            Throwable thrown = catchThrowable(() ->
                    authService.login(new LoginRequest("missing@example.com", "pass")));
            assertThat(thrown)
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Неверный email или пароль");
            // Исходное сообщение NotFoundException не должно просачиваться
            assertThat(thrown.getMessage()).doesNotContain("не найден");
        }

        @Test
        @DisplayName("неверный пароль → UnauthorizedException")
        void wrongPassword_shouldThrowUnauthorized() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            when(userService.findByEmail("user@example.com")).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() ->
                    authService.login(new LoginRequest("user@example.com", "wrongpass")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Неверный email или пароль");
        }

        @Test
        @DisplayName("пользователь BLOCKED → UnauthorizedException с упоминанием блокировки")
        void blockedUser_shouldThrowUnauthorizedWithBlockedMessage() {
            var user = buildUser("user@example.com", UserStatus.BLOCKED);
            when(userService.findByEmail("user@example.com")).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() ->
                    authService.login(new LoginRequest("user@example.com", "password123")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("заблокирован");
        }

        @Test
        @DisplayName("JWT генерируется с email пользователя из БД (после нормализации)")
        void login_shouldGenerateTokenWithNormalizedEmail() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            when(userService.findByEmail("user@example.com")).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtProvider.generateToken("user@example.com")).thenReturn("token");
            when(authMapper.toUserInfoResponse(user)).thenReturn(
                    new UserInfoResponse(1L, "user@example.com", List.of("STUDENT"), "ACTIVE"));

            authService.login(new LoginRequest("user@example.com", "password123"));

            verify(jwtProvider).generateToken("user@example.com");
        }
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("существующий email → создаёт и сохраняет PasswordResetToken")
        void existingEmail_shouldSaveResetToken() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            when(userService.findByEmailOptional("user@example.com")).thenReturn(Optional.of(user));

            authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());

            PasswordResetToken saved = tokenCaptor.getValue();
            assertThat(saved.getToken())
                    .as("Токен должен быть непустой строкой (UUID)")
                    .isNotBlank();
            assertThat(saved.getUser())
                    .as("Токен должен быть привязан к пользователю")
                    .isEqualTo(user);
            assertThat(saved.getExpiresAt())
                    .as("Токен должен иметь время истечения в будущем")
                    .isAfter(OffsetDateTime.now());
        }

        @Test
        @DisplayName("несуществующий email → не создаёт токен, не бросает исключение")
        void nonExistingEmail_shouldNotSaveTokenAndNotThrow() {
            when(userService.findByEmailOptional("ghost@example.com")).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() ->
                    authService.forgotPassword(new ForgotPasswordRequest("ghost@example.com")));

            verify(passwordResetTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("email нормализуется перед поиском пользователя")
        void emailNormalization_shouldNormalizeEmail() {
            when(userService.findByEmailOptional("user@example.com")).thenReturn(Optional.empty());

            authService.forgotPassword(new ForgotPasswordRequest("USER@EXAMPLE.COM"));

            verify(userService).findByEmailOptional("user@example.com");
            verify(userService, never()).findByEmailOptional("USER@EXAMPLE.COM");
        }
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("валидный токен → пароль обновляется, токен помечается как использованный")
        void validToken_shouldUpdatePasswordAndMarkUsed() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            var resetToken = PasswordResetToken.builder()
                    .token("valid-uuid-token")
                    .user(user)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("valid-uuid-token"))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$new_hashed");
            when(userService.save(any())).thenReturn(user);

            authService.resetPassword(new ResetPasswordRequest("valid-uuid-token", "newPassword123"));

            assertThat(user.getPasswordHash())
                    .as("Пароль пользователя должен обновиться")
                    .isEqualTo("$2a$new_hashed");
            assertThat(resetToken.isUsed())
                    .as("Токен должен быть помечен как использованный")
                    .isTrue();
            verify(passwordResetTokenRepository).save(resetToken);
        }

        @Test
        @DisplayName("токен не найден → UnauthorizedException")
        void tokenNotFound_shouldThrowUnauthorized() {
            when(passwordResetTokenRepository.findByToken("non-existent-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.resetPassword(new ResetPasswordRequest("non-existent-token", "newPass")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Недействительный токен");
        }

        @Test
        @DisplayName("токен уже использован → UnauthorizedException")
        void tokenAlreadyUsed_shouldThrowUnauthorized() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            var usedToken = PasswordResetToken.builder()
                    .token("used-token")
                    .user(user)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .used(true)
                    .build();

            when(passwordResetTokenRepository.findByToken("used-token"))
                    .thenReturn(Optional.of(usedToken));

            assertThatThrownBy(() ->
                    authService.resetPassword(new ResetPasswordRequest("used-token", "newPass")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Токен уже был использован");

            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("токен истёк → UnauthorizedException, пароль не меняется")
        void expiredToken_shouldThrowUnauthorized() {
            var user = buildUser("user@example.com", UserStatus.ACTIVE);
            var expiredToken = PasswordResetToken.builder()
                    .token("expired-token")
                    .user(user)
                    .expiresAt(OffsetDateTime.now().minusHours(1)) // уже истёк
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() ->
                    authService.resetPassword(new ResetPasswordRequest("expired-token", "newPass")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Токен истёк");

            verify(userService, never()).save(any());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email, UserStatus status) {
        return User.builder()
                .email(email)
                .passwordHash("hashed")
                .status(status)
                .roles(Set.of())
                .build();
    }

    private Role buildRole(RoleCode code) {
        var role = new Role();
        org.springframework.test.util.ReflectionTestUtils.setField(role, "code", code);
        org.springframework.test.util.ReflectionTestUtils.setField(role, "name", code.name());
        return role;
    }
}
