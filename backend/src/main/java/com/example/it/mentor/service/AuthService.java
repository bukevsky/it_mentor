package com.example.it.mentor.service;

import com.example.it.mentor.config.OtpProperties;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthMapper authMapper;
    private final EmailService emailService;
    private final OtpProperties otpProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userService.existsByEmail(email)) {
            throw new ConflictException("Email уже зарегистрирован");
        }

        var studentRole = roleRepository.findByCode(RoleCode.STUDENT)
                .orElseThrow(() -> new IllegalStateException("Роль STUDENT не найдена в БД"));

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.EMAIL_NOT_CONFIRMED)
                .roles(Set.of(studentRole))
                .build();

        User saved;
        try {
            saved = userService.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email уже зарегистрирован");
        }

        StudentProfile profile = StudentProfile.builder()
                .user(saved)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
        studentProfileRepository.save(profile);

        log.info("Зарегистрирован новый пользователь: {}", email);
        return authMapper.toRegisterResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        User user;
        try {
            user = userService.findByEmail(email);
        } catch (NotFoundException e) {
            throw new UnauthorizedException("Неверный email или пароль");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Неверный email или пароль");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedException("Аккаунт заблокирован");
        }

        String token = jwtProvider.generateToken(user.getEmail());

        log.info("Успешный вход пользователя: {}", email);
        return new LoginResponse(token, "Bearer", authMapper.toUserInfoResponse(user));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase();

        // ifPresent — не раскрываем факт существования email: всегда возвращаем 200
        userService.findByEmailOptional(email).ifPresent(user -> {
            // Аннулируем все старые активные коды для этого пользователя
            passwordResetTokenRepository.invalidateAllByUserId(user.getId());

            String otp = generateOtp();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(otp)
                    .expiresAt(OffsetDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetOtp(email, otp);
            log.info("OTP-код сброса пароля сгенерирован для: {}", email);
        });
    }

    private static String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().toLowerCase();

        User user = userService.findByEmailOptional(email)
                .orElseThrow(() -> new UnauthorizedException("Недействительный код сброса пароля"));

        int updated = passwordResetTokenRepository.markTokenUsed(
                user.getId(), request.code(), OffsetDateTime.now(), otpProperties.getMaxAttempts());

        if (updated == 0) {
            passwordResetTokenRepository.incrementAttempts(user.getId(), request.code());
            throw new UnauthorizedException("Недействительный, истёкший или заблокированный код сброса пароля");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userService.save(user);
        log.info("Пароль успешно сброшен для пользователя: {}", user.getEmail());
    }
}
