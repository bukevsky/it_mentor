package com.example.it.mentor.service;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.entity.PasswordResetToken;
import com.example.it.mentor.mapper.AuthMapper;
import com.example.it.mentor.repository.PasswordResetTokenRepository;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.exception.UnauthorizedException;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.security.JwtProvider;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserService userService;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private AuthMapper authMapper;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: новый email → возвращает RegisterResponse")
    void register_happyPath_shouldReturnResponse() {
        var request = new RegisterRequest("User@Example.com", "password123");
        var role = buildRole(RoleCode.STUDENT);
        var savedUser = buildUser("user@example.com", UserStatus.EMAIL_NOT_CONFIRMED);
        var expected = new RegisterResponse(1L, "user@example.com", java.util.List.of("STUDENT"));

        when(userService.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(authMapper.toRegisterResponse(savedUser)).thenReturn(expected);

        var result = authService.register(request);

        assertThat(result).isEqualTo(expected);
        verify(userService).save(any(User.class));
    }

    @Test
    @DisplayName("register: дублирующий email → ConflictException")
    void register_duplicateEmail_shouldThrowConflict() {
        when(userService.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "password123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email уже зарегистрирован");

        verify(userService, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: корректные учётные данные → выдаёт JWT и user info")
    void login_correctCredentials_shouldReturnTokenResponse() {
        var request = new LoginRequest("user@example.com", "password123");
        var user = buildUser("user@example.com", UserStatus.ACTIVE);
        var userInfo = new UserInfoResponse(1L, "user@example.com", java.util.List.of("STUDENT"), "ACTIVE");
        var springUser = mock(UserDetails.class);

        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(springUser);
        when(jwtProvider.generateToken(springUser)).thenReturn("jwt-token");
        when(authMapper.toUserInfoResponse(user)).thenReturn(userInfo);

        var result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login: пользователь не найден → UnauthorizedException с маскировкой")
    void login_userNotFound_shouldThrowUnauthorized() {
        when(userService.findByEmail(anyString())).thenThrow(new NotFoundException("не найден"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "pass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Неверный email или пароль");
    }

    @Test
    @DisplayName("login: неверный пароль → UnauthorizedException")
    void login_wrongPassword_shouldThrowUnauthorized() {
        var user = buildUser("user@example.com", UserStatus.ACTIVE);
        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrongpass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Неверный email или пароль");
    }

    @Test
    @DisplayName("login: пользователь заблокирован → UnauthorizedException")
    void login_blockedUser_shouldThrowUnauthorized() {
        var user = buildUser("user@example.com", UserStatus.BLOCKED);
        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "password123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("заблокирован");
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
