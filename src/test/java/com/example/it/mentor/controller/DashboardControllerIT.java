package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.dashboard.ActivityItemResponse;
import com.example.it.mentor.dto.dashboard.DashboardSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /dashboard IT")
class DashboardControllerIT {

    @Autowired private TestRestTemplate restTemplate;

    private String studentToken;

    @BeforeEach
    void setUp() {
        String email = "dash_" + uid() + "@test.com";
        studentToken = registerAndLogin(email);
    }

    @Test
    @DisplayName("GET /dashboard/summary студент → 200, role=STUDENT, нулевые счётчики")
    void getSummary_authenticatedStudent_shouldReturn200WithStudentRole() {
        ResponseEntity<DashboardSummaryResponse> response = restTemplate.exchange(
                "/dashboard/summary", HttpMethod.GET,
                bearerRequest(null, studentToken), DashboardSummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo("STUDENT");
        assertThat(response.getBody().sentRequests()).isZero();
        assertThat(response.getBody().totalChats()).isZero();
        assertThat(response.getBody().unreadChats()).isZero();
    }

    @Test
    @DisplayName("GET /dashboard/summary без токена → 401")
    void getSummary_unauthenticated_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/dashboard/summary", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /dashboard/activity аутентифицированный → 200, пустой список")
    void getActivity_authenticated_shouldReturn200() {
        ResponseEntity<List<ActivityItemResponse>> response = restTemplate.exchange(
                "/dashboard/activity?limit=20", HttpMethod.GET,
                bearerRequest(null, studentToken),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /dashboard/activity без токена → 401")
    void getActivity_unauthenticated_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/dashboard/activity", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /dashboard/activity limit превышает 50 → 400")
    void getActivity_limitExceedsMax_shouldReturn400() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/dashboard/activity?limit=51", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
