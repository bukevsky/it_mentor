package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.service.EmailService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("Auth Password Reset IT")
class AuthPasswordResetIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoSpyBean
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── POST /auth/password/reset ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/password/reset")
    class ResetPassword {

        @Test
        @DisplayName("валидный код + новый пароль → 200")
        void validTokenAndPassword_shouldReturn200() {
            String email = uniqueEmail();
            register(email);
            String code = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("после сброса можно войти с новым паролем")
        void afterReset_canLoginWithNewPassword() {
            String email = uniqueEmail();
            register(email);
            String code = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "NewPassword123!"), Object.class);

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
            String code = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "NewPassword123!"), Object.class);

            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "Password123"), Object.class);

            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("код можно использовать только один раз — второй вызов → 401")
        void tokenIsOneTimeUse_secondAttemptShouldReturn401() {
            String email = uniqueEmail();
            register(email);
            String code = createResetToken(email);

            restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "NewPassword123!"), Object.class);

            var secondResponse = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "AnotherPassword456!"), Object.class);

            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("несуществующий код → 401")
        void nonExistentToken_shouldReturn401() {
            String email = uniqueEmail();
            register(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, "000000", "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("явно неверный код (пустой) → 400")
        void blankToken_shouldReturn400() {
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest("user@example.com", "", "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("новый пароль слишком короткий (менее 8 символов) → 400")
        void shortPassword_shouldReturn400() {
            String email = uniqueEmail();
            register(email);
            String code = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "short"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("пустой новый пароль → 400")
        void blankPassword_shouldReturn400() {
            String email = uniqueEmail();
            register(email);
            String code = createResetToken(email);

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, ""), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("ответ об ошибке содержит корректный формат ErrorResponse")
        void errorResponse_shouldHaveCorrectFormat() {
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest("nonexistent@example.com", "000000", "NewPassword123!"),
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

        @Test
        @DisplayName("правильный email, неправильный код → 401")
        void wrongCode_shouldReturn401() {
            String email = uniqueEmail();
            register(email);
            createResetToken(email); // создаём токен, но используем другой код

            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, "999999", "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
            String code = createResetToken(email);

            // Act: reset
            var resetResponse = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, code, "BrandNewPass99!"), Object.class);
            assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Assert: login with new password
            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "BrandNewPass99!"), LoginResponse.class);
            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(loginResponse.getBody().accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("второй forgot-запрос инвалидирует первый код — остаётся только 1 активный")
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

            assertThat(tokenCount).isEqualTo(1);
        }

        @Test
        @DisplayName("второй forgot инвалидирует первый код — сброс с первым кодом → 401")
        void forgotTwice_firstCodeNoLongerWorks() {
            String email = uniqueEmail();
            register(email);

            // Первый forgot — получаем первый код
            String firstCode = createResetToken(email);

            // Второй forgot — инвалидирует первый код и создаёт новый
            restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest(email), Object.class);

            // Первый код больше не работает
            var response = restTemplate.postForEntity("/auth/password/reset",
                    new ResetPasswordRequest(email, firstCode, "NewPassword123!"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void register(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
    }

    /**
     * Вызывает forgot-password и перехватывает OTP-код через spy на EmailService.
     * После миграции 029 OTP хранится в БД как BCrypt-хеш, поэтому JDBC-запрос к plain-text коду невозможен.
     */
    private String createResetToken(String email) {
        Mockito.clearInvocations(emailService);

        restTemplate.postForEntity("/auth/password/forgot",
                new ForgotPasswordRequest(email), Object.class);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetOtp(eq(email), codeCaptor.capture());

        String code = codeCaptor.getValue();
        assertThat(code).as("OTP-код для %s должен быть создан", email).isNotBlank();
        return code;
    }

    private String uniqueEmail() {
        return "reset_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
