package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestClarifyRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestRejectRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
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
import org.springframework.core.ParameterizedTypeReference;
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
@DisplayName("POST/GET/PUT /mentoring/requests IT")
class MentoringRequestControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<MentoringRequestResponse>> PAGED_TYPE =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String mentorToken;
    private Long studentProfileId;
    private Long mentorProfileId;

    @BeforeEach
    void setUp() {
        String studentEmail = "student_" + uid() + "@test.com";
        String mentorEmail  = "mentor_"  + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        studentProfileId = createStudentProfile(studentToken);
        mentorProfileId  = createMentorProfile(mentorToken);
    }

    // ── POST /mentoring/requests ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /mentoring/requests")
    class CreateRequest {

        @Test
        @DisplayName("студент создаёт заявку → 201 с корректным телом")
        void student_createRequest_shouldReturn201() {
            MentoringRequestCreateRequest body =
                    new MentoringRequestCreateRequest(mentorProfileId, MentoringType.PRACTICE, "Хочу учиться");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST, bearerRequest(body, studentToken),
                    MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo("SENT");
            assertThat(response.getBody().direction()).isEqualTo("STUDENT_TO_MENTOR");
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            MentoringRequestCreateRequest body =
                    new MentoringRequestCreateRequest(mentorProfileId, MentoringType.PRACTICE, "msg");

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    "/mentoring/requests", body, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("отсутствует goalType → 400")
        void missingGoalType_shouldReturn400() {
            String badBody = """
                    {"targetProfileId": %d, "message": "ok"}
                    """.formatted(mentorProfileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(studentToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(badBody, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST, request, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("дубликат активной заявки → 409")
        void duplicateRequest_shouldReturn409() {
            MentoringRequestCreateRequest body =
                    new MentoringRequestCreateRequest(mentorProfileId, MentoringType.PRACTICE, "msg");

            restTemplate.exchange("/mentoring/requests", HttpMethod.POST,
                    bearerRequest(body, studentToken), Object.class);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST,
                    bearerRequest(body, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("ментор закрыт → 422")
        void closedMentor_shouldReturn422() {
            // Обновляем профиль ментора — закрываем набор
            MentorProfileRequest closedRequest = new MentorProfileRequest(
                    "Mentor", "Test", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    RecruitmentStatus.CLOSED, null);
            restTemplate.exchange("/profile/mentor", HttpMethod.PUT,
                    bearerRequest(closedRequest, mentorToken), Object.class);

            MentoringRequestCreateRequest body =
                    new MentoringRequestCreateRequest(mentorProfileId, MentoringType.PRACTICE, "msg");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.POST,
                    bearerRequest(body, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    // ── GET /mentoring/requests ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /mentoring/requests")
    class GetRequests {

        @Test
        @DisplayName("студент видит свои исходящие заявки")
        void student_seesOwnOutgoing() {
            createRequest(studentToken, mentorProfileId);

            ResponseEntity<PagedResponse<MentoringRequestResponse>> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_TYPE);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isNotEmpty();
        }

        @Test
        @DisplayName("ментор видит входящие заявки")
        void mentor_seesIncoming() {
            createRequest(studentToken, mentorProfileId);

            ResponseEntity<PagedResponse<MentoringRequestResponse>> response = restTemplate.exchange(
                    "/mentoring/requests", HttpMethod.GET,
                    bearerRequest(null, mentorToken), PAGED_TYPE);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isNotEmpty();
        }

        @Test
        @DisplayName("фильтр по статусу SENT — возвращает только SENT")
        void filterByStatus_shouldReturnOnlyMatching() {
            createRequest(studentToken, mentorProfileId);

            ResponseEntity<PagedResponse<MentoringRequestResponse>> response = restTemplate.exchange(
                    "/mentoring/requests?status=SENT", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_TYPE);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allSatisfy(r -> assertThat(r.status()).isEqualTo("SENT"));
        }
    }

    // ── GET /mentoring/requests/{id} ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /mentoring/requests/{id}")
    class GetById {

        @Test
        @DisplayName("участник получает заявку → 200")
        void participant_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id, HttpMethod.GET,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(id);
        }

        @Test
        @DisplayName("не существует → 404")
        void notFound_shouldReturn404() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/99999", HttpMethod.GET,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("чужая заявка → 403")
        void otherUserRequest_shouldReturn403() {
            Long id = createRequest(studentToken, mentorProfileId);

            // Регистрируем постороннего пользователя
            String otherToken = registerAndLogin("other_" + uid() + "@test.com");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/" + id, HttpMethod.GET,
                    bearerRequest(null, otherToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── PUT /mentoring/requests/{id}/view ─────────────────────────────────────

    @Nested
    @DisplayName("PUT /{id}/view")
    class MarkAsReviewing {

        @Test
        @DisplayName("SENT → REVIEWING: 200")
        void sentToReviewing_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/view", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REVIEWING");
        }

        @Test
        @DisplayName("повторный view — идемпотентно, возвращает 200")
        void repeatedView_shouldBeIdempotent() {
            Long id = createRequest(studentToken, mentorProfileId);
            restTemplate.exchange("/mentoring/requests/" + id + "/view", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), Object.class);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/view", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REVIEWING");
        }
    }

    // ── PUT /mentoring/requests/{id}/needs-clarification ─────────────────────

    @Nested
    @DisplayName("PUT /{id}/needs-clarification")
    class NeedsClarification {

        @Test
        @DisplayName("SENT → NEEDS_CLARIFICATION: 200")
        void sentToNeedsClarification_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);
            MentoringRequestClarifyRequest body = new MentoringRequestClarifyRequest("Расскажите подробнее");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/needs-clarification", HttpMethod.PUT,
                    bearerRequest(body, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("NEEDS_CLARIFICATION");
            assertThat(response.getBody().clarificationNote()).isEqualTo("Расскажите подробнее");
        }
    }

    // ── PUT /mentoring/requests/{id}/accept ───────────────────────────────────

    @Nested
    @DisplayName("PUT /{id}/accept")
    class AcceptRequest {

        @Test
        @DisplayName("SENT → ACCEPTED: 200")
        void sentToAccepted_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("лимит ментора = 0 → 422")
        void menteeLimitZero_shouldReturn422() {
            // Обновляем профиль ментора с лимитом 0
            MentorProfileRequest limitRequest = new MentorProfileRequest(
                    "Mentor", "Test", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, 0,
                    RecruitmentStatus.OPEN, null);
            restTemplate.exchange("/profile/mentor", HttpMethod.PUT,
                    bearerRequest(limitRequest, mentorToken), Object.class);

            // Нужен новый mentorProfileId после обновления (лимит = 0)
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    // ── PUT /mentoring/requests/{id}/reject ───────────────────────────────────

    @Nested
    @DisplayName("PUT /{id}/reject")
    class RejectRequest {

        @Test
        @DisplayName("SENT → REJECTED: 200")
        void sentToRejected_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);
            MentoringRequestRejectRequest body = new MentoringRequestRejectRequest("Нет времени");

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/reject", HttpMethod.PUT,
                    bearerRequest(body, mentorToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("REJECTED");
            assertThat(response.getBody().reason()).isEqualTo("Нет времени");
        }
    }

    // ── PUT /mentoring/requests/{id}/cancel ───────────────────────────────────

    @Nested
    @DisplayName("PUT /{id}/cancel")
    class CancelRequest {

        @Test
        @DisplayName("SENT → CANCELLED: 200")
        void sentToCancelled_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/cancel", HttpMethod.PUT,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("CANCELLED");
        }
    }

    // ── PUT /mentoring/requests/{id}/complete ─────────────────────────────────

    @Nested
    @DisplayName("PUT /{id}/complete")
    class CompleteRequest {

        @Test
        @DisplayName("ACCEPTED → COMPLETED: 200")
        void acceptedToCompleted_shouldReturn200() {
            Long id = createRequest(studentToken, mentorProfileId);
            // accept first
            restTemplate.exchange("/mentoring/requests/" + id + "/accept", HttpMethod.PUT,
                    bearerRequest(null, mentorToken), Object.class);

            ResponseEntity<MentoringRequestResponse> response = restTemplate.exchange(
                    "/mentoring/requests/" + id + "/complete", HttpMethod.PUT,
                    bearerRequest(null, studentToken), MentoringRequestResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
            assertThat(response.getBody().completedAt()).isNotNull();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Иван", "Иванов"), Object.class);
        return login(email);
    }

    private String login(String email) {
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
