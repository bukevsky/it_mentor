package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для проверки формата ErrorResponse.
 * Проверяет структуру и содержимое ответов об ошибках для разных типов исключений.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("ErrorResponse: формат ответа об ошибке IT")
class ErrorResponseFormatIT {

    @Autowired
    private TestRestTemplate restTemplate;

    // ── 400 Bad Request ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 Bad Request — валидация")
    class BadRequestErrors {

        @Test
        @DisplayName("пустой firstName → 400 с полем details")
        void blankFirstName_shouldReturn400WithDetails() {
            var request = new RegisterRequest("test@example.com", "Password123", "", "Иванов");

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/register", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody()).as("body").isNotNull();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo(400);
            softly.assertThat(response.getBody().error()).as("error").isNotBlank();
            softly.assertThat(response.getBody().message()).as("message").isNotBlank();
            softly.assertThat(response.getBody().timestamp()).as("timestamp").isNotNull();
            softly.assertThat(response.getBody().details()).as("details").isNotNull().isNotEmpty();
            softly.assertAll();
        }

        @Test
        @DisplayName("некорректный JSON → 400 с кодом MALFORMED_JSON")
        void malformedJson_shouldReturn400WithMalformedJsonError() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{not valid json}", headers);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/register", entity, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().error()).isEqualTo("MALFORMED_JSON");
        }

        @Test
        @DisplayName("пустое тело запроса → 400")
        void emptyBody_shouldReturn400() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/register", entity, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("отсутствует обязательный @RequestParam → 400 VALIDATION_ERROR, не 500")
        void missingRequiredRequestParam_shouldReturn400() {
            String email = "missparam_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
            ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
            String token = loginResp.getBody().accessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/chats/1/messages/cursor", HttpMethod.GET,
                    new HttpEntity<>(headers), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().details())
                    .anyMatch(d -> d.contains("beforeMessageId"));
        }
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 Unauthorized")
    class UnauthorizedErrors {

        @Test
        @DisplayName("неверные учётные данные → 401 с корректным форматом")
        void wrongCredentials_shouldReturn401WithCorrectFormat() {
            String email = "err_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(email, "wrongpassword"), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo(401);
            softly.assertThat(response.getBody().error()).as("error").isNotBlank();
            softly.assertThat(response.getBody().message()).as("message").isNotBlank();
            softly.assertThat(response.getBody().path()).as("path").contains("/auth/login");
            softly.assertAll();
        }

        @Test
        @DisplayName("запрос без токена к защищённому эндпоинту → 401 с форматом ErrorResponse")
        void noToken_toProtectedEndpoint_shouldReturn401WithErrorFormat() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/auth/me", HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
        }

        @Test
        @DisplayName("401 без токена — кириллическое сообщение в UTF-8 (не кракозябры)")
        void noToken_shouldReturnCyrillicMessageInUtf8() {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "/auth/me", HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), byte[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            MediaType contentType = response.getHeaders().getContentType();
            assertThat(contentType).isNotNull();
            assertThat(contentType.toString().toLowerCase())
                    .as("Content-Type должен содержать charset=utf-8")
                    .contains("charset=utf-8");

            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            assertThat(body)
                    .as("сообщение должно быть в UTF-8, без знаков '?' вместо кириллицы")
                    .contains("Требуется аутентификация")
                    .doesNotContain("?????");
        }
    }

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("404 Not Found")
    class NotFoundErrors {

        @Test
        @DisplayName("несуществующий профиль студента → 404 с корректным форматом")
        void nonExistentStudentProfile_shouldReturn404WithCorrectFormat() {
            String email = "err404_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
            ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
            String token = loginResp.getBody().accessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/profiles/students/999999", HttpMethod.GET,
                    new HttpEntity<>(headers), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo(404);
            softly.assertThat(response.getBody().error()).as("error").isNotBlank();
            softly.assertThat(response.getBody().message()).as("message").isNotBlank();
            softly.assertThat(response.getBody().timestamp()).as("timestamp").isNotNull();
            softly.assertAll();
        }

        @Test
        @DisplayName("несуществующий путь (NoResourceFoundException) → 404 NOT_FOUND, не 500")
        void unknownPath_shouldReturn404NotFound() {
            // путь под публичным префиксом, чтобы запрос дошёл до DispatcherServlet
            // (иначе Spring Security вернёт 401 раньше, чем сработает NoResourceFoundException)
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    "/dictionaries/no-such-subpath-" + UUID.randomUUID(), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        }
    }

    // ── 405 Method Not Allowed ────────────────────────────────────────────────

    @Nested
    @DisplayName("405 Method Not Allowed")
    class MethodNotAllowed {

        @Test
        @DisplayName("DELETE /auth/register → 405 с кодом METHOD_NOT_ALLOWED")
        void deleteOnRegisterEndpoint_shouldReturn405() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/auth/register", HttpMethod.DELETE,
                    new HttpEntity<>(new HttpHeaders()), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody().error()).isEqualTo("METHOD_NOT_ALLOWED");
        }

        @Test
        @DisplayName("POST /dictionaries/cities (только GET) → 405 (публичный эндпоинт, без auth)")
        void postOnPublicGetEndpoint_shouldReturn405() {
            // Используем публичный эндпоинт (без аутентификации), чтобы
            // Spring Security не перехватила запрос до метода диспетчера
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/dictionaries/cities", null, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody().error()).isEqualTo("METHOD_NOT_ALLOWED");
        }
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("409 Conflict")
    class ConflictErrors {

        @Test
        @DisplayName("повторная регистрация с тем же email → 409 с корректным форматом")
        void duplicateRegistration_shouldReturn409WithCorrectFormat() {
            var request = new RegisterRequest(
                    "conflict_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                    "Password123", "Иван", "Иванов");
            restTemplate.postForEntity("/auth/register", request, Object.class);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/register", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo(409);
            softly.assertThat(response.getBody().error()).as("error").isNotBlank();
            softly.assertThat(response.getBody().message()).as("message").isNotBlank();
            softly.assertThat(response.getBody().timestamp()).as("timestamp").isNotNull();
            softly.assertAll();
        }
    }

    // ── Путь (path) в ответе ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Поле path в ErrorResponse")
    class PathField {

        @Test
        @DisplayName("path содержит правильный URI")
        void error_pathShouldMatchRequestUri() {
            String email = "path_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(email, "WRONG"), ErrorResponse.class);

            assertThat(response.getBody().path()).isEqualTo("/auth/login");
        }
    }

    // ── Публичные эндпоинты (словари) не требуют аутентификации ──────────────

    @Nested
    @DisplayName("Публичные эндпоинты — нет 401")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /dictionaries/cities без токена → 200")
        void getCitiesWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/dictionaries/cities", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("GET /dictionaries/skills без токена → 200")
        void getSkillsWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/dictionaries/skills", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("GET /dictionaries/languages без токена → 200")
        void getLanguagesWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/dictionaries/languages", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("GET /dictionaries/interaction-types без токена → 200")
        void getInteractionTypesWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                    "/dictionaries/interaction-types", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("GET /actuator/health без токена → 200")
        void getHealthWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/actuator/health", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("GET /profiles/mentors без токена → 200 (публичный поиск менторов)")
        void getMentorListWithoutToken_shouldReturn200() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profiles/mentors", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ── 403 Forbidden — UTF-8 в сообщении ─────────────────────────────────────

    @Nested
    @DisplayName("403 Forbidden — UTF-8")
    class ForbiddenEncoding {

        @Test
        @DisplayName("403 для STUDENT → /admin/** — кириллическое сообщение в UTF-8")
        void studentToAdminPath_shouldReturn403WithCyrillicUtf8() {
            String email = "enc403_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            restTemplate.postForEntity("/auth/register",
                    new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
            ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
            String token = loginResp.getBody().accessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "/admin/users", HttpMethod.GET,
                    new HttpEntity<>(headers), byte[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            MediaType contentType = response.getHeaders().getContentType();
            assertThat(contentType).isNotNull();
            assertThat(contentType.toString().toLowerCase())
                    .as("Content-Type должен содержать charset=utf-8")
                    .contains("charset=utf-8");

            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            assertThat(body)
                    .as("сообщение 403 должно быть в UTF-8")
                    .contains("Доступ запрещён")
                    .doesNotContain("?????");
        }
    }
}
