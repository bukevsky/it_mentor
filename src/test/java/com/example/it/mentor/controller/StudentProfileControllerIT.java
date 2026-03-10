package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.student.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("StudentProfileController IT")
class StudentProfileControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    // ── PUT /profile/student ───────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /profile/student")
    class UpsertProfile {

        @Test
        @DisplayName("минимальный запрос → 200, firstName/lastName/id/userId в ответе")
        void minimalRequest_shouldReturn200WithRequiredFields() {
            String token = registerAndLogin();

            ResponseEntity<StudentProfileResponse> response = putProfile(minimalProfile("Иван", "Иванов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).as("id").isNotNull().isPositive();
            softly.assertThat(response.getBody().firstName()).as("firstName").isEqualTo("Иван");
            softly.assertThat(response.getBody().lastName()).as("lastName").isEqualTo("Иванов");
            softly.assertThat(response.getBody().userId()).as("userId").isNotNull();
            softly.assertAll();
        }

        @Test
        @DisplayName("полный профиль → 200, все коллекции и поля в ответе")
        void fullProfile_shouldReturn200WithAllFields() {
            String token = registerAndLogin();

            ResponseEntity<StudentProfileResponse> response = putProfile(fullProfile("Мария", "Петрова"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().educations()).as("educations").hasSize(1);
            softly.assertThat(response.getBody().languages()).as("languages").hasSize(1);
            softly.assertThat(response.getBody().skills()).as("skills").hasSize(1);
            softly.assertThat(response.getBody().employmentTypes())
                    .as("employmentTypes").contains(EmploymentType.FULL_TIME.name());
            softly.assertThat(response.getBody().workFormats())
                    .as("workFormats").contains(WorkFormat.REMOTE.name());
            softly.assertAll();
        }

        @Test
        @DisplayName("полный профиль → данные образования корректны")
        void fullProfile_educationFields_shouldBeCorrect() {
            String token = registerAndLogin();

            ResponseEntity<StudentProfileResponse> response = putProfile(fullProfile("Борис", "Орлов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().educations()).hasSize(1);

            StudentEducationResponse edu = response.getBody().educations().get(0);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(edu.institution()).as("institution").isEqualTo("МГУ");
            softly.assertThat(edu.specialty()).as("specialty").isEqualTo("Информатика");
            softly.assertThat(edu.degree()).as("degree").isEqualTo(EducationDegree.BACHELOR.name());
            softly.assertThat(edu.educationForm()).as("educationForm").isEqualTo(EducationForm.FULL_TIME.name());
            softly.assertThat(edu.startYear()).as("startYear").isEqualTo(2019);
            softly.assertThat(edu.graduationYear()).as("graduationYear").isEqualTo(2023);
            softly.assertAll();
        }

        @Test
        @DisplayName("повторный PUT → обновляет существующий профиль без дублирования")
        void secondPut_shouldUpdateWithoutDuplication() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Первое", "Имя"), token);

            ResponseEntity<StudentProfileResponse> response = putProfile(minimalProfile("Обновлённое", "Имя"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Обновлённое");
        }

        @Test
        @DisplayName("два PUT с образованием → образование не дублируется")
        void twoPutsWithEducation_shouldNotDuplicateEducations() {
            String token = registerAndLogin();
            putProfile(fullProfile("Борис", "Орлов"), token);

            ResponseEntity<StudentProfileResponse> response = putProfile(fullProfile("Борис", "Орлов"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().educations())
                    .as("При повторном PUT образование не дублируется")
                    .hasSize(1);
        }

        @Test
        @DisplayName("PUT с навыками, потом без навыков → навыки очищаются")
        void putWithSkillsThenWithoutSkills_shouldClearSkills() {
            String token = registerAndLogin();
            putProfile(fullProfile("Анна", "Иванова"), token); // с навыком

            // Повторный PUT без навыков
            StudentProfileRequest withoutSkills = new StudentProfileRequest(
                    "Анна", "Иванова", null, null, null,
                    null, null, null, null, null,
                    null, null, List.of(), List.of(), List.of()); // пустые коллекции

            ResponseEntity<StudentProfileResponse> response = putProfile(withoutSkills, token);

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
                    "/profile/student", HttpMethod.PUT, entity, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("пустой firstName → 400")
        void blankFirstName_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/student", HttpMethod.PUT,
                    bearerRequest(minimalProfile("", "Иванов"), token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("пустой lastName → 400")
        void blankLastName_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/student", HttpMethod.PUT,
                    bearerRequest(minimalProfile("Иван", ""), token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("несуществующий cityId → 404")
        void nonExistentCityId_shouldReturn404() {
            String token = registerAndLogin();
            StudentProfileRequest request = new StudentProfileRequest(
                    "Иван", "Иванов", null, null, 999999L,
                    null, null, null, null, null,
                    null, null, null, null, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profile/student", HttpMethod.PUT,
                    bearerRequest(request, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── GET /profile/student/me ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /profile/student/me")
    class GetMyProfile {

        @Test
        @DisplayName("после регистрации → сразу 200 с firstName/lastName из запроса")
        void afterRegistration_shouldReturnProfileWithNames() {
            String token = registerAndLogin();

            ResponseEntity<StudentProfileResponse> response = restTemplate.exchange(
                    "/profile/student/me", HttpMethod.GET,
                    bearerRequest(null, token), StudentProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().firstName()).isEqualTo("Иван");
            assertThat(response.getBody().lastName()).isEqualTo("Иванов");
        }

        @Test
        @DisplayName("после PUT → возвращает актуальные данные")
        void afterPut_shouldReturnUpdatedProfile() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Мой", "Профиль"), token);

            ResponseEntity<StudentProfileResponse> response = restTemplate.exchange(
                    "/profile/student/me", HttpMethod.GET,
                    bearerRequest(null, token), StudentProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Мой");
            assertThat(response.getBody().lastName()).isEqualTo("Профиль");
        }

        @Test
        @DisplayName("после двух PUT → возвращает последнюю версию")
        void afterMultiplePuts_shouldReturnLastVersion() {
            String token = registerAndLogin();
            putProfile(minimalProfile("Старое", "Имя"), token);
            putProfile(minimalProfile("Новое", "Имя"), token);

            ResponseEntity<StudentProfileResponse> response = restTemplate.exchange(
                    "/profile/student/me", HttpMethod.GET,
                    bearerRequest(null, token), StudentProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Новое");
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profile/student/me", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── GET /profiles/students/{id} ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /profiles/students/{id}")
    class GetProfileById {

        @Test
        @DisplayName("существующий профиль → 200 и корректные данные")
        void existingProfile_shouldReturn200WithCorrectData() {
            String token = registerAndLogin();
            ResponseEntity<StudentProfileResponse> created = putProfile(minimalProfile("Карточка", "Студента"), token);
            Long profileId = created.getBody().id();

            ResponseEntity<StudentProfileResponse> response = restTemplate.exchange(
                    "/profiles/students/" + profileId, HttpMethod.GET,
                    bearerRequest(null, token), StudentProfileResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).isEqualTo(profileId);
            softly.assertThat(response.getBody().firstName()).isEqualTo("Карточка");
            softly.assertThat(response.getBody().lastName()).isEqualTo("Студента");
            softly.assertAll();
        }

        @ParameterizedTest(name = "id={0}")
        @ValueSource(longs = {999999L, Long.MAX_VALUE})
        @DisplayName("несуществующий id → 404")
        void nonExistentId_shouldReturn404(long id) {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/students/" + id, HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profiles/students/1", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── GET /profile/me ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /profile/me")
    class GetProfileSummary {

        @Test
        @DisplayName("новый пользователь → role=STUDENT, profileExists=true (профиль создаётся при регистрации)")
        void afterRegistration_shouldReturnStudentRoleAndProfileExists() {
            String token = registerAndLogin();

            ResponseEntity<ProfileSummaryResponse> response = restTemplate.exchange(
                    "/profile/me", HttpMethod.GET,
                    bearerRequest(null, token), ProfileSummaryResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().role()).as("role").isEqualTo("STUDENT");
            softly.assertThat(response.getBody().profileExists()).as("profileExists").isTrue();
            softly.assertThat(response.getBody().profileId()).as("profileId").isNotNull().isPositive();
            softly.assertAll();
        }

        @Test
        @DisplayName("после PUT профиля → profileId совпадает с id из PUT-ответа")
        void afterPut_profileIdMatchesCreatedProfile() {
            String token = registerAndLogin();
            ResponseEntity<StudentProfileResponse> created = putProfile(minimalProfile("Иван", "Иванов"), token);
            Long profileId = created.getBody().id();

            ResponseEntity<ProfileSummaryResponse> response = restTemplate.exchange(
                    "/profile/me", HttpMethod.GET,
                    bearerRequest(null, token), ProfileSummaryResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().profileId())
                    .as("profileId в summary должен совпадать с id созданного профиля")
                    .isEqualTo(profileId);
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profile/me", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "it_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "password123", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "password123"), LoginResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private StudentProfileRequest minimalProfile(String firstName, String lastName) {
        return new StudentProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null,
                null, null, List.of(), List.of(), List.of());
    }

    private StudentProfileRequest fullProfile(String firstName, String lastName) {
        return new StudentProfileRequest(
                firstName, lastName, "Иванович", "+79001234567", null,
                "Java Developer", 20, LocalDate.of(2025, 6, 1),
                "О себе", "maxnick",
                Set.of(EmploymentType.FULL_TIME),
                Set.of(WorkFormat.REMOTE),
                List.of(new StudentEducationRequest(
                        "МГУ", "Информатика",
                        EducationDegree.BACHELOR, EducationForm.FULL_TIME,
                        2019, 2023)),
                List.of(new StudentLanguageRequest(1L, LanguageLevel.B2)),
                List.of(new StudentSkillRequest(1L, SkillLevel.INTERMEDIATE)));
    }

    private ResponseEntity<StudentProfileResponse> putProfile(StudentProfileRequest request, String token) {
        return restTemplate.exchange(
                "/profile/student", HttpMethod.PUT,
                bearerRequest(request, token), StudentProfileResponse.class);
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
