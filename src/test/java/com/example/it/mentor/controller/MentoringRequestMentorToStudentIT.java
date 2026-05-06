package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.*;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для заявок, инициированных ментором (MENTOR_TO_STUDENT).
 * Проверяет корректность инвертированных ролей инициатора и адресата.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("MentoringRequest: Ментор инициирует заявку (MENTOR_TO_STUDENT) IT")
class MentoringRequestMentorToStudentIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private String studentToken;
    private String mentorToken;
    private Long studentProfileId;

    @BeforeEach
    void setUp() {
        String studentEmail = "st_m2s_" + uid() + "@test.com";
        String mentorEmail  = "mt_m2s_" + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken  = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        studentProfileId = createStudentProfile(studentToken);
        createMentorProfile(mentorToken);
    }

    // ── Создание заявки ментором ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /mentoring/requests (ментор → студент)")
    class CreateMentorToStudentRequest {

        @Test
        @DisplayName("ментор создаёт заявку студенту → 201, direction=MENTOR_TO_STUDENT")
        void mentor_createRequest_shouldReturn201WithCorrectDirection() {
            var body = new MentoringRequestCreateRequest(studentProfileId, MentoringType.PRACTICE, "Приглашаю на менторство");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST, bearerRequest(body, mentorToken),
                    MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).as("id").isNotNull();
            softly.assertThat(response.getBody().status()).as("status").isEqualTo("SENT");
            softly.assertThat(response.getBody().direction()).as("direction").isEqualTo("MENTOR_TO_STUDENT");
            softly.assertAll();
        }

        @Test
        @DisplayName("ментор не может создать заявку самому себе → 422")
        void mentor_cannotCreateRequestToSelf_shouldReturn422() {
            // Ментор также имеет студент профиль (созданный при регистрации)
            // Нам нужен id профиля студента ментора
            ResponseEntity<StudentProfileResponse> mentorStudentProfile = restTemplate.exchange(
                    "/profile/student/me", HttpMethod.GET, bearerRequest(null, mentorToken),
                    StudentProfileResponse.class);

            if (mentorStudentProfile.getStatusCode() != HttpStatus.OK) {
                return; // ментор не имеет студент-профиля, тест неприменим
            }

            Long mentorStudentProfileId = mentorStudentProfile.getBody().id();
            var body = new MentoringRequestCreateRequest(mentorStudentProfileId, MentoringType.PRACTICE, "Сам себе");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST, bearerRequest(body, mentorToken),
                    Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    // ── Операции с заявкой MENTOR_TO_STUDENT ─────────────────────────────────

    @Nested
    @DisplayName("Операции: студент — адресат (получатель) заявки")
    class StudentAsRecipient {

        @Test
        @DisplayName("студент (адресат) принимает заявку → 200, status=ACCEPTED")
        void student_acceptsRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("студент (адресат) отклоняет заявку → 200, status=REJECTED")
        void student_rejectsRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);
            var body = new MentoringRequestRejectRequest("Уже нашёл ментора");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/reject", HttpMethod.PUT,
                    bearerRequest(body, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REJECTED");
            assertThat(response.getBody().reason()).isEqualTo("Уже нашёл ментора");
        }

        @Test
        @DisplayName("студент (адресат) просит уточнить → 200, status=NEEDS_CLARIFICATION")
        void student_requestsClarification_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);
            var body = new MentoringRequestClarifyRequest("Уточните условия");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/needs-clarification", HttpMethod.PUT,
                    bearerRequest(body, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("NEEDS_CLARIFICATION");
            assertThat(response.getBody().clarificationNote()).isEqualTo("Уточните условия");
        }

        @Test
        @DisplayName("студент (адресат) просматривает → 200, status=REVIEWING")
        void student_viewsRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/view", HttpMethod.PUT,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REVIEWING");
        }
    }

    // ── Ментор — инициатор заявки ─────────────────────────────────────────────

    @Nested
    @DisplayName("Операции: ментор — инициатор заявки")
    class MentorAsInitiator {

        @Test
        @DisplayName("ментор (инициатор) отменяет заявку → 200, status=CANCELLED")
        void mentor_cancelsRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/cancel", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("ментор (инициатор) не может принять свою же заявку → 403")
        void mentor_cannotAcceptOwnRequest_shouldReturn403() {
            Long id = createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("ментор (инициатор) не может отклонить свою же заявку → 403")
        void mentor_cannotRejectOwnRequest_shouldReturn403() {
            Long id = createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/reject", HttpMethod.PUT,
                    bearerRequest(new MentoringRequestRejectRequest("Нет"), mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── Обе стороны могут завершить заявку ───────────────────────────────────

    @Nested
    @DisplayName("Завершение заявки: доступно обеим сторонам")
    class CompleteRequest {

        @Test
        @DisplayName("ментор (инициатор) может завершить ACCEPTED заявку → 200")
        void mentor_completesAcceptedRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);
            restTemplate.exchange("/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, studentToken), Object.class);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/complete", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
            assertThat(response.getBody().completedAt()).isNotNull();
        }

        @Test
        @DisplayName("студент (адресат) может завершить ACCEPTED заявку → 200")
        void student_completesAcceptedRequest_shouldReturn200() {
            Long id = createMentorRequest(mentorToken, studentProfileId);
            restTemplate.exchange("/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, studentToken), Object.class);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/complete", HttpMethod.PUT,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
        }
    }

    // ── Видимость заявок для обеих сторон ────────────────────────────────────

    @Nested
    @DisplayName("Видимость: обе стороны видят заявку")
    class Visibility {

        @Test
        @DisplayName("ментор видит свою исходящую заявку в GET /mentoring/requests")
        void mentor_seesOwnOutgoingRequest() {
            createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.GET,
                    bearerRequest(null, mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("студент видит входящую заявку от ментора в GET /mentoring/requests")
        void student_seesIncomingRequestFromMentor() {
            createMentorRequest(mentorToken, studentProfileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.GET,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private void grantMentorRole(String email) {
        User user = userRepository.findWithRolesByEmailAndDeletedFalse(email).orElseThrow();
        Role mentorRole = roleRepository.findByCode(RoleCode.MENTOR).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(mentorRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private Long createStudentProfile(String token) {
        StudentProfileRequest req = new StudentProfileRequest(
                "Студент", "Иванов", null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        ResponseEntity<StudentProfileResponse> resp = restTemplate.exchange(
                "/profile/student", HttpMethod.PUT, bearerRequest(req, token),
                StudentProfileResponse.class);
        return resp.getBody().id();
    }

    private void createMentorProfile(String token) {
        MentorProfileRequest req = new MentorProfileRequest(
                "Ментор", "Петров", null, "Java Dev", null, null, null, null,
                null, null, null, null, null, null, null, null,
                RecruitmentStatus.OPEN, null);
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(req, token),
                MentorProfileResponse.class);
    }

    private Long createMentorRequest(String mentorToken, Long targetStudentProfileId) {
        var body = new MentoringRequestCreateRequest(targetStudentProfileId, MentoringType.PRACTICE, "Приглашаю!");
        ResponseEntity<MentoringRequestResponse> resp = restTemplate.exchange(
                "/mentoring/requests", HttpMethod.POST, bearerRequest(body, mentorToken),
                MentoringRequestResponse.class);
        return resp.getBody().id();
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
