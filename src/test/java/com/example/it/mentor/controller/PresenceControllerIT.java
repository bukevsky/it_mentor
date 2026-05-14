package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.presence.PresenceResponse;
import com.example.it.mentor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /presence/{userId} IT")
class PresenceControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;

    private String token;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        String callerEmail = "caller_" + uid() + "@test.com";
        String targetEmail = "target_" + uid() + "@test.com";

        token = registerAndLogin(callerEmail);
        registerAndLogin(targetEmail);
        targetUserId = userRepository.findByEmailAndDeletedFalse(targetEmail).orElseThrow().getId();
    }

    @Test
    @DisplayName("существующий пользователь без SSE-подключения → 200, статус offline")
    void getPresence_existingUser_shouldReturn200WithOfflineStatus() {
        ResponseEntity<PresenceResponse> response = restTemplate.exchange(
                "/presence/" + targetUserId, HttpMethod.GET,
                bearerRequest(null, token), PresenceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(targetUserId);
        assertThat(response.getBody().status()).isEqualTo("offline");
    }

    @Test
    @DisplayName("несуществующий userId → 404")
    void getPresence_nonExistingUser_shouldReturn404() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/presence/99999999", HttpMethod.GET,
                bearerRequest(null, token), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("без токена → 401")
    void getPresence_unauthenticated_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/presence/" + targetUserId, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
