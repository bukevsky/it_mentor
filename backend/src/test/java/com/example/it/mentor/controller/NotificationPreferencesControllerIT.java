package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.notification.NotificationPreferencesResponse;
import com.example.it.mentor.dto.notification.UpdateNotificationPreferencesRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("NotificationPreferencesController IT")
class NotificationPreferencesControllerIT {

    @Autowired private TestRestTemplate restTemplate;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = registerAndLogin("prefs_" + uid() + "@test.com");
    }

    @Nested
    @DisplayName("GET /profile/me/notifications")
    class GetPreferences {

        @Test
        @DisplayName("первый запрос — возвращает дефолтные настройки (всё включено)")
        void get_firstRequest_returnsDefaults() {
            ResponseEntity<NotificationPreferencesResponse> response = restTemplate.exchange(
                    "/profile/me/notifications", HttpMethod.GET,
                    bearer(null, userToken), NotificationPreferencesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().emailRequestEvents()).isTrue();
            assertThat(response.getBody().emailSessionEvents()).isTrue();
            assertThat(response.getBody().emailReviewEvents()).isTrue();
        }

        @Test
        @DisplayName("без токена → 401")
        void get_unauthenticated_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profile/me/notifications", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /profile/me/notifications")
    class UpdatePreferences {

        @Test
        @DisplayName("отключить request-уведомления → сохраняется")
        void put_disableRequestEvents_savedCorrectly() {
            UpdateNotificationPreferencesRequest dto =
                    new UpdateNotificationPreferencesRequest(false, null, null);

            ResponseEntity<NotificationPreferencesResponse> put = restTemplate.exchange(
                    "/profile/me/notifications", HttpMethod.PUT,
                    bearer(dto, userToken), NotificationPreferencesResponse.class);

            assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(put.getBody().emailRequestEvents()).isFalse();
            assertThat(put.getBody().emailSessionEvents()).isTrue();
            assertThat(put.getBody().emailReviewEvents()).isTrue();
        }

        @Test
        @DisplayName("повторный GET после PUT — возвращает обновлённые значения")
        void put_thenGet_returnsUpdated() {
            restTemplate.exchange("/profile/me/notifications", HttpMethod.PUT,
                    bearer(new UpdateNotificationPreferencesRequest(false, false, false), userToken),
                    NotificationPreferencesResponse.class);

            ResponseEntity<NotificationPreferencesResponse> get = restTemplate.exchange(
                    "/profile/me/notifications", HttpMethod.GET,
                    bearer(null, userToken), NotificationPreferencesResponse.class);

            assertThat(get.getBody().emailRequestEvents()).isFalse();
            assertThat(get.getBody().emailSessionEvents()).isFalse();
            assertThat(get.getBody().emailReviewEvents()).isFalse();
        }

        @Test
        @DisplayName("без токена → 401")
        void put_unauthenticated_shouldReturn401() {
            UpdateNotificationPreferencesRequest dto =
                    new UpdateNotificationPreferencesRequest(false, null, null);
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/me/notifications", HttpMethod.PUT,
                    new HttpEntity<>(dto, new HttpHeaders()), Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private <T> HttpEntity<T> bearer(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
