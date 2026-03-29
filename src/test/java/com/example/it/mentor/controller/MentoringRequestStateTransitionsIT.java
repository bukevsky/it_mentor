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
 * Интеграционные тесты для машины состояний заявок на менторинг.
 * Проверяет:
 * - Запрещённые переходы (422 UNPROCESSABLE_CONTENT)
 * - Проверки доступа (403 FORBIDDEN)
 * - Корректные поля ответа при переходах
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("MentoringRequest: Машина состояний и контроль доступа IT")
class MentoringRequestStateTransitionsIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private String studentToken;
    private String mentorToken;
    private Long mentorProfileId;

    @BeforeEach
    void setUp() {
        String studentEmail = "st_" + uid() + "@test.com";
        String mentorEmail  = "mt_" + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken  = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        createStudentProfile(studentToken);
        mentorProfileId = createMentorProfile(mentorToken);
    }

    // ── Контроль доступа: студент не может вызывать ментор-операции ────────────

    @Nested
    @DisplayName("Контроль доступа: студент не может управлять входящими заявками")
    class StudentAccessControl {

        @Test
        @DisplayName("студент вызывает /view → 403")
        void student_callsView_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/view", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("студент вызывает /needs-clarification → 403")
        void student_callsNeedsClarification_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);
            var body = new MentoringRequestClarifyRequest("Объясни подробнее");

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/needs-clarification", body, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("студент вызывает /accept → 403")
        void student_callsAccept_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/accept", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("студент вызывает /reject → 403")
        void student_callsReject_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);
            var body = new MentoringRequestRejectRequest("Не подходит");

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/reject", body, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── Контроль доступа: ментор не может отменять чужие заявки ──────────────

    @Nested
    @DisplayName("Контроль доступа: ментор не может отменять заявки")
    class MentorAccessControl {

        @Test
        @DisplayName("ментор вызывает /cancel → 403")
        void mentor_callsCancel_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/cancel", null, mentorToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── Контроль доступа: посторонний пользователь ────────────────────────────

    @Nested
    @DisplayName("Контроль доступа: посторонний пользователь")
    class ThirdPartyAccessControl {

        @Test
        @DisplayName("посторонний вызывает GET /{id} → 403")
        void thirdParty_getsRequest_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);
            String otherToken = registerAndLogin("other_" + uid() + "@test.com");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/" + id, HttpMethod.GET,
                    bearerRequest(null, otherToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("посторонний вызывает /view → 403")
        void thirdParty_callsView_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);
            String otherToken = registerAndLogin("third_" + uid() + "@test.com");

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/view", null, otherToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("посторонний вызывает /accept → 403")
        void thirdParty_callsAccept_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);
            String otherToken = registerAndLogin("thrd_" + uid() + "@test.com");

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/accept", null, otherToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── Недопустимые переходы состояний ──────────────────────────────────────

    @Nested
    @DisplayName("Недопустимые переходы: попытки повторной обработки")
    class InvalidStateTransitions {

        @Test
        @DisplayName("принять уже REJECTED заявку → 422")
        void acceptRejectedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/reject",
                    new MentoringRequestRejectRequest("Отклонено"), mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/accept", null, mentorToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("отклонить уже ACCEPTED заявку → 422")
        void rejectAcceptedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/accept", null, mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/reject",
                    new MentoringRequestRejectRequest("Передумал"), mentorToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("попросить пояснение у REJECTED заявки → 422")
        void clarifyRejectedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/reject",
                    new MentoringRequestRejectRequest("Отклонено"), mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/needs-clarification",
                    new MentoringRequestClarifyRequest("Пояснение"), mentorToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("завершить SENT заявку (не ACCEPTED) → 422")
        void completeSentRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/complete", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("завершить REJECTED заявку → 422")
        void completeRejectedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/reject",
                    new MentoringRequestRejectRequest("Нет"), mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/complete", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("завершить CANCELLED заявку → 422")
        void completeCancelledRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/cancel", null, studentToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/complete", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("отменить ACCEPTED заявку → 422")
        void cancelAcceptedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/accept", null, mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/cancel", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("отменить REJECTED заявку → 422")
        void cancelRejectedRequest_shouldReturn422() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/reject",
                    new MentoringRequestRejectRequest("Нет"), mentorToken);

            ResponseEntity<Object> response = put("/mentoring/requests/" + id + "/cancel", null, studentToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    // ── Идемпотентные операции ────────────────────────────────────────────────

    @Nested
    @DisplayName("Идемпотентные операции")
    class IdempotentOperations {

        @Test
        @DisplayName("повторный /view для REVIEWING → 200, статус остаётся REVIEWING")
        void repeatedView_isIdempotent() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/view", null, mentorToken);

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id + "/view",
                            HttpMethod.PUT, bearerRequest(null, mentorToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REVIEWING");
        }
    }

    // ── Поля ответа при переходах состояний ──────────────────────────────────

    @Nested
    @DisplayName("Поля ответа при переходах состояний")
    class ResponseFieldsOnTransitions {

        @Test
        @DisplayName("/needs-clarification → clarificationNote заполнен, respondedAt не null")
        void needsClarification_shouldSetClarificationNoteAndRespondedAt() {
            Long id = createRequest(studentToken, mentorProfileId);
            String note = "Расскажите о вашем опыте подробнее";

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id + "/needs-clarification",
                            HttpMethod.PUT,
                            bearerRequest(new MentoringRequestClarifyRequest(note), mentorToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().clarificationNote()).isEqualTo(note);
            assertThat(response.getBody().respondedAt()).isNotNull();
        }

        @Test
        @DisplayName("/reject → reason заполнен, respondedAt не null")
        void reject_shouldSetReasonAndRespondedAt() {
            Long id = createRequest(studentToken, mentorProfileId);
            String reason = "Не подходит по уровню";

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id + "/reject",
                            HttpMethod.PUT,
                            bearerRequest(new MentoringRequestRejectRequest(reason), mentorToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().reason()).isEqualTo(reason);
            assertThat(response.getBody().respondedAt()).isNotNull();
        }

        @Test
        @DisplayName("/accept → respondedAt не null")
        void accept_shouldSetRespondedAt() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id + "/accept",
                            HttpMethod.PUT, bearerRequest(null, mentorToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().respondedAt()).isNotNull();
        }

        @Test
        @DisplayName("/complete → completedAt не null")
        void complete_shouldSetCompletedAt() {
            Long id = createRequest(studentToken, mentorProfileId);
            put("/mentoring/requests/" + id + "/accept", null, mentorToken);

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id + "/complete",
                            HttpMethod.PUT, bearerRequest(null, studentToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().completedAt()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("GET /{id} содержит student и mentor профили")
        void getById_shouldContainStudentAndMentorProfiles() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response =
                    restTemplate.exchange("/mentoring/requests/" + id,
                            HttpMethod.GET, bearerRequest(null, studentToken),
                            MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().studentProfile()).isNotNull();
            assertThat(response.getBody().mentorProfile()).isNotNull();
            assertThat(response.getBody().goalType()).isNotBlank();
            assertThat(response.getBody().message()).isNotBlank();
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
        User user = userRepository.findByEmailAndDeletedFalse(email).orElseThrow();
        Role mentorRole = roleRepository.findByCode(RoleCode.MENTOR).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(mentorRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private void createStudentProfile(String token) {
        StudentProfileRequest req = new StudentProfileRequest(
                "Студент", "Иванов", null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/student", HttpMethod.PUT, bearerRequest(req, token),
                StudentProfileResponse.class);
    }

    private Long createMentorProfile(String token) {
        MentorProfileRequest req = new MentorProfileRequest(
                "Ментор", "Петров", null, "Java Dev", null, null, null, null,
                null, null, null, null, null, null, null, null,
                RecruitmentStatus.OPEN, null);
        ResponseEntity<MentorProfileResponse> resp = restTemplate.exchange(
                "/profile/mentor", HttpMethod.PUT, bearerRequest(req, token),
                MentorProfileResponse.class);
        return resp.getBody().id();
    }

    private Long createRequest(String token, Long targetMentorProfileId) {
        MentoringRequestCreateRequest body =
                new MentoringRequestCreateRequest(targetMentorProfileId, MentoringType.PRACTICE, "Хочу учиться");
        ResponseEntity<MentoringRequestResponse> resp = restTemplate.exchange(
                "/mentoring/requests", HttpMethod.POST, bearerRequest(body, token),
                MentoringRequestResponse.class);
        return resp.getBody().id();
    }

    private ResponseEntity<Object> put(String url, Object body, String token) {
        return restTemplate.exchange(url, HttpMethod.PUT, bearerRequest(body, token), Object.class);
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
