package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.RegisterResponse;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class AuthControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register: новый пользователь → 201 и роль STUDENT")
    void register_newUser_shouldReturn201() {
        var request = new RegisterRequest("new_it@example.com", "password123");

        var response = restTemplate.postForEntity("/auth/register", request, RegisterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("new_it@example.com");
        assertThat(response.getBody().roles()).contains("STUDENT");
    }

    @Test
    @DisplayName("POST /auth/register: дубликат email → 409")
    void register_duplicateEmail_shouldReturn409() {
        var request = new RegisterRequest("duplicate@example.com", "password123");
        restTemplate.postForEntity("/auth/register", request, Object.class);

        var response = restTemplate.postForEntity("/auth/register", request, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login: валидные креды → 200 и JWT")
    void login_validCredentials_shouldReturn200WithToken() {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest("login_ok@example.com", "password123"), Object.class);

        var response = restTemplate.postForEntity("/auth/login",
                new LoginRequest("login_ok@example.com", "password123"), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("POST /auth/login: неверный пароль → 401")
    void login_wrongPassword_shouldReturn401() {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest("login_bad@example.com", "password123"), Object.class);

        var response = restTemplate.postForEntity("/auth/login",
                new LoginRequest("login_bad@example.com", "wrongpassword"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/me: c валидным Bearer → 200")
    void me_withValidToken_shouldReturn200() {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest("me_ok@example.com", "password123"), Object.class);
        var loginResponse = restTemplate.postForEntity("/auth/login",
                new LoginRequest("me_ok@example.com", "password123"), LoginResponse.class);
        String token = loginResponse.getBody().accessToken();

        var response = restTemplate.exchange("/auth/me", HttpMethod.GET,
                bearerRequest(token), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /auth/me: без токена → 401")
    void me_withoutToken_shouldReturn401() {
        var response = restTemplate.getForEntity("/auth/me", Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── blocked user ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login: пользователь в статусе BLOCKED → 401")
    void login_blockedUser_shouldReturn401() {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest("blocked@example.com", "password123"), Object.class);

        userRepository.findByEmailAndDeletedFalse("blocked@example.com").ifPresent(user -> {
            user.setStatus(UserStatus.BLOCKED);
            userRepository.save(user);
        });

        var response = restTemplate.postForEntity("/auth/login",
                new LoginRequest("blocked@example.com", "password123"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpEntity<Void> bearerRequest(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
