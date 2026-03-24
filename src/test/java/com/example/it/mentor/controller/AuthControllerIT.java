package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.repository.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("AuthController IT")
class AuthControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("новый пользователь → 201, email в нижнем регистре, роль STUDENT")
        void newUser_shouldReturn201WithStudentRole() {
            var request = new RegisterRequest("New_IT@Example.com", "Password123", "Иван", "Иванов");

            var response = restTemplate.postForEntity("/auth/register", request, RegisterResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).as("id").isNotNull();
            softly.assertThat(response.getBody().email())
                    .as("email должен быть нормализован в нижний регистр")
                    .isEqualTo("new_it@example.com");
            softly.assertThat(response.getBody().roles())
                    .as("роль должна быть STUDENT")
                    .contains("STUDENT");
            softly.assertAll();
        }

        @Test
        @DisplayName("email в верхнем регистре → сохраняется в нижнем (нормализация)")
        void uppercaseEmail_shouldBeNormalizedToLowercase() {
            String uniqueEmail = "UPPER_" + UUID.randomUUID().toString().substring(0, 8) + "@EXAMPLE.COM";
            var request = new RegisterRequest(uniqueEmail, "Password123", "Анна", "Петрова");

            var response = restTemplate.postForEntity("/auth/register", request, RegisterResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().email()).isEqualTo(uniqueEmail.toLowerCase());
        }

        @Test
        @DisplayName("дубликат email → 409 Conflict")
        void duplicateEmail_shouldReturn409() {
            var request = new RegisterRequest("duplicate@example.com", "Password123", "Иван", "Иванов");
            restTemplate.postForEntity("/auth/register", request, Object.class);

            var response = restTemplate.postForEntity("/auth/register", request, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("дубликат email с другим регистром → 409 (нормализация работает)")
        void duplicateEmailDifferentCase_shouldReturn409() {
            String email = "casedup_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            // Тот же email, но в верхнем регистре
            var response = restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email.toUpperCase(), "Password123", "Иван", "Иванов"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("пустой firstName → 400 Bad Request")
        void blankFirstName_shouldReturn400() {
            var request = new RegisterRequest("valid@example.com", "Password123", "", "Иванов");

            var response = restTemplate.postForEntity("/auth/register", request, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("пустой lastName → 400 Bad Request")
        void blankLastName_shouldReturn400() {
            var request = new RegisterRequest("valid2@example.com", "Password123", "Иван", "");

            var response = restTemplate.postForEntity("/auth/register", request, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("валидные учётные данные → 200, непустой JWT, tokenType Bearer")
        void validCredentials_shouldReturn200WithToken() {
            String email = "login_ok_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            var response = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "Password123"), LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().accessToken()).as("accessToken").isNotBlank();
            softly.assertThat(response.getBody().tokenType()).as("tokenType").isEqualTo("Bearer");
            softly.assertThat(response.getBody().user()).as("user info").isNotNull();
            softly.assertAll();
        }

        @Test
        @DisplayName("неверный пароль → 401")
        void wrongPassword_shouldReturn401() {
            String email = "login_bad_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            var response = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "wrongpassword"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("несуществующий email → 401 (не 404, чтобы не раскрывать наличие email)")
        void nonExistentEmail_shouldReturn401NotLeak() {
            var response = restTemplate.postForEntity("/auth/login",
                    new LoginRequest("ghost_" + UUID.randomUUID() + "@example.com", "pass"), Object.class);

            assertThat(response.getStatusCode())
                    .as("Несуществующий email должен возвращать 401, не 404")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("email в верхнем регистре → 200 (вход нечувствителен к регистру)")
        void uppercaseEmail_shouldLoginSuccessfully() {
            String email = "case_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            var response = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email.toUpperCase(), "Password123"), LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("заблокированный пользователь → 401")
        void blockedUser_shouldReturn401() {
            String email = "blocked_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
                user.setStatus(UserStatus.BLOCKED);
                userRepository.save(user);
            });

            var response = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "Password123"), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /auth/me")
    class Me {

        @Test
        @DisplayName("валидный Bearer токен → 200 и корректный user info")
        void validToken_shouldReturn200WithUserInfo() {
            String email = "me_ok_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
            var loginResponse = restTemplate.postForEntity("/auth/login",
                    new LoginRequest(email, "Password123"), LoginResponse.class);
            String token = loginResponse.getBody().accessToken();

            var response = restTemplate.exchange("/auth/me", HttpMethod.GET,
                    bearerRequest(token), UserInfoResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().email()).as("email").isEqualTo(email);
            softly.assertThat(response.getBody().roles()).as("roles").contains("STUDENT");
            softly.assertThat(response.getBody().status()).as("status").isNotBlank();
            softly.assertAll();
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            var response = restTemplate.getForEntity("/auth/me", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("неверный токен → 401")
        void invalidToken_shouldReturn401() {
            var headers = new HttpHeaders();
            headers.setBearerAuth("totally.invalid.jwt");
            var response = restTemplate.exchange("/auth/me", HttpMethod.GET,
                    new HttpEntity<>(headers), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── POST /auth/password/forgot ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/password/forgot")
    class ForgotPassword {

        @Test
        @DisplayName("существующий email → 200 (не раскрывает факт существования)")
        void existingEmail_shouldReturn200() {
            String email = "forgot_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            var response = restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest(email), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("несуществующий email → тоже 200 (безопасность: не раскрываем наличие email)")
        void nonExistingEmail_shouldReturn200ToNotLeakInfo() {
            var response = restTemplate.postForEntity("/auth/password/forgot",
                    new ForgotPasswordRequest("ghost_" + UUID.randomUUID() + "@example.com"), Object.class);

            assertThat(response.getStatusCode())
                    .as("Несуществующий email должен давать 200, чтобы не раскрывать факт существования аккаунта")
                    .isEqualTo(HttpStatus.OK);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpEntity<Void> bearerRequest(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
