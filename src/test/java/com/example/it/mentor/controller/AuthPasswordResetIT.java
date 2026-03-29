package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.repository.PasswordResetTokenRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("Auth Password Reset IT")
class AuthPasswordResetIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── POST /auth/password/reset ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/password/reset")
    class ResetPassword {

        @Test
        @DisplayName("валидный токен + новый пароль → 200")
        void validTokenAndPassword_shouldReturn200() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("после сброса можно войти с новым паролем")
        void afterReset_canLoginWithNewPassword() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "NewPassword123!"), Object.class);

            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "NewPassword123!"), LoginResponse.class);

            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(loginResponse.getBody()).isNotNull();
            assertThat(loginResponse.getBody().accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("после сброса старый пароль не работает")
        void afterReset_oldPasswordShouldFail() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "NewPassword123!"), Object.class);

            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "Password123"), Object.class);

            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("токен можно использовать только один раз — второй вызов → 401")
        void tokenIsOneTimeUse_secondAttemptShouldReturn401() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "NewPassword123!"), Object.class);

            var secondResponse = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "AnotherPassword456!"), Object.class);

            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("несуществующий токен → 401")
        void nonExistentToken_shouldReturn401() {
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(UUID.randomUUID().toString(), "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("явно неверный токен (пустой) → 400")
        void blankToken_shouldReturn400() {
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest("", "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("новый пароль слишком короткий (менее 8 символов) → 400")
        void shortPassword_shouldReturn400() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "short"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("пустой новый пароль → 400")
        void blankPassword_shouldReturn400() {
            String email = uniqueEmail();
            register(email);
            String token = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, ""), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("ответ об ошибке содержит корректный формат ErrorResponse")
        void errorResponse_shouldHaveCorrectFormat() {
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(UUID.randomUUID().toString(), "NewPassword123!"),
                    ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo(401);
            softly.assertThat(response.getBody().error()).as("error").isNotBlank();
            softly.assertThat(response.getBody().message()).as("message").isNotBlank();
            softly.assertThat(response.getBody().timestamp()).as("timestamp").isNotNull();
            softly.assertAll();
        }
    }

    // ── POST /auth/password/forgot → токен создаётся в БД ─────────────────────

    @Nested
    @DisplayName("Интеграция forgot + reset")
    class ForgotAndResetIntegration {

        @Test
        @DisplayName("forgot → сохраняет токен в БД")
        void forgot_shouldSaveTokenToDatabase() {
            String email = uniqueEmail();
            register(email);

            restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest(email), Object.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM password_reset_tokens t " +
                    "JOIN users u ON t.user_id = u.id WHERE u.email = ?",
                    Integer.class, email);

            assertThat(count).isGreaterThan(0);
        }

        @Test
        @DisplayName("полный flow: register → forgot → reset → login → 200")
        void fullPasswordResetFlow_shouldSucceed() {
            // Arrange
            String email = uniqueEmail();
            register(email);

            // Act: forgot + get token via JDBC
            String token = createResetToken(email);

            // Act: reset
            var resetResponse = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(token, "BrandNewPass99!"), Object.class);
            assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Assert: login with new password
            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "BrandNewPass99!"), LoginResponse.class);
            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(loginResponse.getBody().accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("несколько forgot-запросов → каждый создаёт отдельный токен, все действительны до use")
        void multipleForgot_eachCreatesNewToken() {
            String email = uniqueEmail();
            register(email);

            restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest(email), Object.class);
            restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest(email), Object.class);

            Integer tokenCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM password_reset_tokens t " +
                    "JOIN users u ON t.user_id = u.id " +
                    "WHERE u.email = ? AND t.used = false",
                    Integer.class, email);

            assertThat(tokenCount).isGreaterThanOrEqualTo(2);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void register(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
    }

    /**
     * Вызывает forgot-password и возвращает токен из БД.
     * Использует JDBC напрямую, чтобы избежать LazyInitializationException
     * при обращении к полю user у detached-сущности PasswordResetToken.
     */
    private String createResetToken(String email) {
        restTemplate.postForEntity("/auth/password/forgot",
                new ForgotPasswordRequest(email), Object.class);

        String token = jdbcTemplate.queryForObject(
                "SELECT t.token FROM password_reset_tokens t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE u.email = ? AND t.used = false " +
                "ORDER BY t.id DESC LIMIT 1",
                String.class, email);

        assertThat(token).as("Токен для %s должен быть создан в БД", email).isNotBlank();
        return token;
    }

    private String uniqueEmail() {
        return "reset_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
