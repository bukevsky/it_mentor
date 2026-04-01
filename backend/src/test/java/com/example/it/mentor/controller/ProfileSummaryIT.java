package com.example.it.mentor.controller;

import com.example.it.mentor.dto.*;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для GET /profile/me с разными ролями.
 * Проверяет ролевое поведение: STUDENT, MENTOR (без профиля и с профилем).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /profile/me: роль-зависимое поведение IT")
class ProfileSummaryIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    // ── Студент ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Пользователь с ролью STUDENT")
    class StudentRole {

        @Test
        @DisplayName("новый студент → role=STUDENT, profileExists=true (создаётся при регистрации)")
        void newStudent_shouldHaveStudentRoleAndExistingProfile() {
            String token = registerAndLogin(uniqueEmail());

            ResponseEntity<ProfileSummaryResponse> response = getSummary(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().role()).as("role").isEqualTo("STUDENT");
            softly.assertThat(response.getBody().profileExists()).as("profileExists").isTrue();
            softly.assertThat(response.getBody().profileId()).as("profileId").isNotNull().isPositive();
            softly.assertAll();
        }

        @Test
        @DisplayName("два разных студента → у каждого свой profileId")
        void twoStudents_shouldHaveDistinctProfileIds() {
            String token1 = registerAndLogin(uniqueEmail());
            String token2 = registerAndLogin(uniqueEmail());

            Long profileId1 = getSummary(token1).getBody().profileId();
            Long profileId2 = getSummary(token2).getBody().profileId();

            assertThat(profileId1).isNotEqualTo(profileId2);
        }
    }

    // ── Ментор без профиля ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Пользователь с ролью MENTOR (без профиля)")
    class MentorRoleWithoutProfile {

        @Test
        @DisplayName("ментор без профиля → role=MENTOR, profileExists=false, profileId=null")
        void mentorWithoutProfile_shouldReturnMentorRoleNoProfile() {
            String email = uniqueEmail();
            String token = registerAndLogin(email);
            grantMentorRole(email);

            ResponseEntity<ProfileSummaryResponse> response = getSummary(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().role()).as("role").isEqualTo("MENTOR");
            softly.assertThat(response.getBody().profileExists()).as("profileExists").isFalse();
            softly.assertThat(response.getBody().profileId()).as("profileId").isNull();
            softly.assertAll();
        }
    }

    // ── Ментор с профилем ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Пользователь с ролью MENTOR (с профилем)")
    class MentorRoleWithProfile {

        @Test
        @DisplayName("ментор после создания профиля → role=MENTOR, profileExists=true, profileId совпадает")
        void mentorAfterCreatingProfile_shouldHaveCorrectSummary() {
            String email = uniqueEmail();
            String token = registerAndLogin(email);
            grantMentorRole(email);

            // Создаём профиль ментора
            ResponseEntity<MentorProfileResponse> profileResp = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(minimalMentorProfile("Алексей", "Смирнов"), token),
                    MentorProfileResponse.class);
            Long createdProfileId = profileResp.getBody().id();

            // Получаем summary
            ResponseEntity<ProfileSummaryResponse> summaryResp = getSummary(token);

            assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(summaryResp.getBody().role()).as("role").isEqualTo("MENTOR");
            softly.assertThat(summaryResp.getBody().profileExists()).as("profileExists").isTrue();
            softly.assertThat(summaryResp.getBody().profileId())
                    .as("profileId должен совпадать с созданным профилем")
                    .isEqualTo(createdProfileId);
            softly.assertAll();
        }

        @Test
        @DisplayName("ментор обновил профиль → profileId не меняется (upsert)")
        void mentorAfterUpdatingProfile_profileIdRemainsSame() {
            String email = uniqueEmail();
            String token = registerAndLogin(email);
            grantMentorRole(email);

            ResponseEntity<MentorProfileResponse> firstPut = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(minimalMentorProfile("Алексей", "Смирнов"), token),
                    MentorProfileResponse.class);
            Long originalProfileId = firstPut.getBody().id();

            restTemplate.exchange("/profile/mentor", HttpMethod.PUT,
                    bearerRequest(minimalMentorProfile("Алексей", "Новиков"), token),
                    MentorProfileResponse.class);

            Long summaryProfileId = getSummary(token).getBody().profileId();

            assertThat(summaryProfileId).isEqualTo(originalProfileId);
        }
    }

    // ── Доступ без токена ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Безопасность")
    class Security {

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profile/me", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("истёкший/неверный токен → 401")
        void invalidToken_shouldReturn401() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalid.jwt.token");
            ResponseEntity<Object> response = restTemplate.exchange("/profile/me", HttpMethod.GET,
                    new HttpEntity<>(headers), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private void grantMentorRole(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email).orElseThrow();
        Role mentorRole = roleRepository.findByCode(RoleCode.MENTOR).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(mentorRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private MentorProfileRequest minimalMentorProfile(String firstName, String lastName) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, RecruitmentStatus.OPEN, null);
    }

    private ResponseEntity<ProfileSummaryResponse> getSummary(String token) {
        return restTemplate.exchange("/profile/me", HttpMethod.GET,
                bearerRequest(null, token), ProfileSummaryResponse.class);
    }

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    private String uniqueEmail() {
        return "summary_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
