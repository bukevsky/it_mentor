package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSkillRequest;
import com.example.it.mentor.entity.enums.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("MentorProfileController IT")
class MentorProfileControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    // ── PUT /profile/mentor ────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /profile/mentor")
    class UpsertMentorProfile {

        @Test
        @DisplayName("минимальный запрос → 200, id/firstName/lastName/userId в ответе")
        void minimalRequest_shouldReturn200WithRequiredFields() {
            String token = registerAndLogin();

            ResponseEntity<MentorProfileResponse> response = putProfile(minimalProfile("Алексей", "Смирнов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).as("id").isNotNull().isPositive();
            softly.assertThat(response.getBody().firstName()).as("firstName").isEqualTo("Алексей");
            softly.assertThat(response.getBody().lastName()).as("lastName").isEqualTo("Смирнов");
            softly.assertThat(response.getBody().userId()).as("userId").isNotNull();
            softly.assertAll();
        }

        @Test
        @DisplayName("полный профиль (с навыками и менторинг-полями) → 200 и все поля заполнены")
        void fullProfile_shouldReturn200WithAllFields() {
            String token = registerAndLogin();

            ResponseEntity<MentorProfileResponse> response = putProfile(fullProfile("Дмитрий", "Козлов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().skills()).as("skills").hasSize(1);
            softly.assertThat(response.getBody().mentoringType())
                    .as("mentoringType").isEqualTo(MentoringType.PRACTICE.name());
            softly.assertThat(response.getBody().mentoringChannel())
                    .as("mentoringChannel").isEqualTo(MentoringChannel.CALLS.name());
            softly.assertThat(response.getBody().mentoringDuration())
                    .as("mentoringDuration").isEqualTo(MentoringDuration.ONE_MONTH.name());
            softly.assertThat(response.getBody().recruitmentStatus())
                    .as("recruitmentStatus").isEqualTo(RecruitmentStatus.OPEN.name());
            softly.assertAll();
        }

        @Test
        @DisplayName("повторный PUT → обновляет firstName в существующем профиле")
        void secondPut_shouldUpdateExistingProfile() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Первое", "Имя"), token);

            ResponseEntity<MentorProfileResponse> response = putProfile(minimalProfile("Обновлённое", "Имя"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Обновлённое");
        }

        @Test
        @DisplayName("два PUT с навыком → навык не дублируется")
        void repeatedSkills_shouldNotDuplicate() {
            String token = registerAndLogin();
            putProfile(fullProfile("Борис", "Орлов"), token);

            ResponseEntity<MentorProfileResponse> response = putProfile(fullProfile("Борис", "Орлов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().skills())
                    .as("При повторном PUT навык не дублируется")
                    .hasSize(1);
        }

        @Test
        @DisplayName("PUT с recruitmentStatus=CLOSED → статус обновляется")
        void updateRecruitmentStatus_shouldReflectInResponse() {
            String token = registerAndLogin();
            // Создаём профиль с OPEN статусом
            putProfile(fullProfile("Иван", "Иванов"), token);

            // Обновляем статус на CLOSED
            MentorProfileRequest closedRequest = new MentorProfileRequest(
                    "Иван", "Иванов", null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, RecruitmentStatus.CLOSED, null);

            ResponseEntity<MentorProfileResponse> response = putProfile(closedRequest, token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().recruitmentStatus())
                    .as("Статус должен обновиться на CLOSED")
                    .isEqualTo(RecruitmentStatus.CLOSED.name());
        }

        @Test
        @DisplayName("PUT с навыками, потом без навыков → навыки очищаются")
        void putWithSkillsThenWithout_shouldClearSkills() {
            String token = registerAndLogin();
            putProfile(fullProfile("Ольга", "Новикова"), token); // с навыком

            MentorProfileRequest withoutSkills = new MentorProfileRequest(
                    "Ольга", "Новикова", null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, List.of()); // пустые навыки

            ResponseEntity<MentorProfileResponse> response = putProfile(withoutSkills, token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().skills())
                    .as("Навыки должны быть очищены при PUT с пустым списком")
                    .isEmpty();
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var entity = new HttpEntity<>(minimalProfile("Тест", "Тест"), headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT, entity, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("пустой firstName → 400")
        void blankFirstName_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(minimalProfile("", "Смирнов"), token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("пустой lastName → 400")
        void blankLastName_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(minimalProfile("Алексей", ""), token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("слишком длинный mentoringFrequency → 400")
        void tooLongMentoringFrequency_shouldReturn400() {
            String token = registerAndLogin();
            MentorProfileRequest request = new MentorProfileRequest(
                    "Иван", "Иванов", null, null, null,
                    null, null, null, null, null, null,
                    null, null, "раз в неделю ".repeat(10), null, null, null, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(request, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("несуществующий cityId → 404")
        void nonExistentCity_shouldReturn404() {
            String token = registerAndLogin();
            MentorProfileRequest request = new MentorProfileRequest(
                    "Иван", "Иванов", null, null, null,
                    999999L, null, null, null, null, null,
                    null, null, null, null, null, null, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(request, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("несуществующий skillId → 404")
        void nonExistentSkill_shouldReturn404() {
            String token = registerAndLogin();
            MentorProfileRequest request = new MentorProfileRequest(
                    "Иван", "Иванов", null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    List.of(new MentorSkillRequest(999999L, SkillLevel.INTERMEDIATE)));

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor", HttpMethod.PUT,
                    bearerRequest(request, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── GET /profile/mentor/me ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /profile/mentor/me")
    class GetMyProfile {

        @Test
        @DisplayName("после создания профиля → 200 и корректные данные")
        void afterCreate_shouldReturn200WithData() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Мой", "Профиль"), token);

            ResponseEntity<MentorProfileResponse> response = restTemplate.exchange(
                    "/profile/mentor/me", HttpMethod.GET,
                    bearerRequest(null, token), MentorProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Мой");
            assertThat(response.getBody().lastName()).isEqualTo("Профиль");
        }

        @Test
        @DisplayName("без профиля → 404")
        void withoutProfile_shouldReturn404() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/mentor/me", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profile/mentor/me", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("после обновления профиля → возвращает последнюю версию")
        void afterUpdate_shouldReturnLatestVersion() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Старое", "Имя"), token);
            putProfile(minimalProfile("Новое", "Имя"), token);

            ResponseEntity<MentorProfileResponse> response = restTemplate.exchange(
                    "/profile/mentor/me", HttpMethod.GET,
                    bearerRequest(null, token), MentorProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Новое");
        }
    }

    // ── GET /profiles/mentors/{id} ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /profiles/mentors/{id}")
    class GetProfileById {

        @Test
        @DisplayName("существующий профиль → 200 и корректные данные")
        void existingProfile_shouldReturn200WithData() {
            String token = registerAndLogin();
            ResponseEntity<MentorProfileResponse> created = putProfile(minimalProfile("Карточка", "Ментора"), token);
            Long profileId = created.getBody().id();

            ResponseEntity<MentorProfileResponse> response = restTemplate.exchange(
                    "/profiles/mentors/" + profileId, HttpMethod.GET,
                    bearerRequest(null, token), MentorProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).isEqualTo(profileId);
            softly.assertThat(response.getBody().firstName()).isEqualTo("Карточка");
            softly.assertThat(response.getBody().lastName()).isEqualTo("Ментора");
            softly.assertAll();
        }

        @ParameterizedTest(name = "id={0}")
        @ValueSource(longs = {999999L, Long.MAX_VALUE})
        @DisplayName("несуществующий id → 404")
        void nonExistentId_shouldReturn404(long id) {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors/" + id, HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("без токена → не 401: GET /profiles/mentors/{id} публичный")
        void withoutToken_shouldBePublic() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profiles/mentors/1", Object.class);
            // Эндпоинт публичный (см. SecurityConfig). Допустимы 200 (есть профиль)
            // или 404 (нет), но не 401.
            assertThat(response.getStatusCode())
                    .as("GET /profiles/mentors/{id} должен быть публичным")
                    .isNotEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getStatusCode())
                    .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "it_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private MentorProfileRequest minimalProfile(String firstName, String lastName) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private MentorProfileRequest fullProfile(String firstName, String lastName) {
        return new MentorProfileRequest(
                firstName, lastName, "Сергеевич", "Java Tech Lead", "R&D",
                null, "+79001234567", "maxnick",
                "Опытный ментор в области backend", "Готовность учиться", "Помогу с Java и Spring",
                MentoringType.PRACTICE, MentoringChannel.CALLS, "Раз в неделю",
                MentoringDuration.ONE_MONTH, 3, RecruitmentStatus.OPEN,
                List.of(new MentorSkillRequest(1L, SkillLevel.CONFIDENT)));
    }

    private ResponseEntity<MentorProfileResponse> putProfile(MentorProfileRequest request, String token) {
        return restTemplate.exchange(
                "/profile/mentor", HttpMethod.PUT,
                bearerRequest(request, token), MentorProfileResponse.class);
    }

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }
}
