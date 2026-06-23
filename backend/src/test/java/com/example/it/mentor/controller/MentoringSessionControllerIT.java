package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.session.CancelSessionRequest;
import com.example.it.mentor.dto.session.CreateSessionRequest;
import com.example.it.mentor.dto.session.RescheduleSessionRequest;
import com.example.it.mentor.dto.session.SessionResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringSessionStatus;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("MentoringSessionController IT")
class MentoringSessionControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<SessionResponse>> PAGED_SESSION =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String mentorToken;
    private Long acceptedRequestId;

    @BeforeEach
    void setUp() {
        String studentEmail = "session_student_" + uid() + "@test.com";
        String mentorEmail  = "session_mentor_"  + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken  = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        createStudentProfile(studentToken);
        Long mentorProfileId = createMentorProfile(mentorToken);

        acceptedRequestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, acceptedRequestId);
    }

    @Nested
    @DisplayName("POST /sessions")
    class CreateSession {

        @Test
        @DisplayName("студент создаёт сессию на принятую заявку → 201")
        void create_byStudent_shouldReturn201() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);

            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    "/sessions", HttpMethod.POST,
                    bearerRequest(dto, studentToken), SessionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(MentoringSessionStatus.SCHEDULED);
            assertThat(response.getBody().durationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("ментор создаёт сессию на принятую заявку → 201")
        void create_byMentor_shouldReturn201() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(2), 90);

            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    "/sessions", HttpMethod.POST,
                    bearerRequest(dto, mentorToken), SessionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("без токена → 401")
        void create_unauthenticated_shouldReturn401() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);

            ResponseEntity<Object> response = restTemplate.postForEntity("/sessions", dto, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("посторонний → 403")
        void create_byOutsider_shouldReturn403() {
            String outsiderToken = registerAndLogin("outsider_" + uid() + "@test.com");
            createStudentProfile(outsiderToken);
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/sessions", HttpMethod.POST,
                    bearerRequest(dto, outsiderToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("конфликт времени → 409")
        void create_conflict_shouldReturn409() {
            OffsetDateTime scheduledAt = future(1);
            CreateSessionRequest first = new CreateSessionRequest(acceptedRequestId, scheduledAt, 60);
            restTemplate.exchange("/sessions", HttpMethod.POST, bearerRequest(first, studentToken), SessionResponse.class);

            CreateSessionRequest second = new CreateSessionRequest(acceptedRequestId, scheduledAt.plusMinutes(10), 60);
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/sessions", HttpMethod.POST,
                    bearerRequest(second, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("прошедшее время → 400 (Bean Validation @Future)")
        void create_pastTime_shouldReturn400() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId,
                    OffsetDateTime.now().minusDays(1), 60);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/sessions", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /sessions and /sessions/{id}")
    class ReadSessions {

        @Test
        @DisplayName("GET /sessions — возвращает страницу для текущего пользователя")
        void list_currentUser_shouldReturnPage() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);
            restTemplate.exchange("/sessions", HttpMethod.POST,
                    bearerRequest(dto, studentToken), SessionResponse.class);

            ResponseEntity<PagedResponse<SessionResponse>> response = restTemplate.exchange(
                    "/sessions?page=0&size=10&sort=scheduledAt,asc", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_SESSION);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(1);
        }

        @Test
        @DisplayName("GET /sessions/{id} — участник видит сессию")
        void getById_participant_shouldReturn200() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);
            ResponseEntity<SessionResponse> created = restTemplate.exchange(
                    "/sessions", HttpMethod.POST, bearerRequest(dto, studentToken), SessionResponse.class);
            Long sessionId = created.getBody().id();

            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    "/sessions/" + sessionId, HttpMethod.GET,
                    bearerRequest(null, mentorToken), SessionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("GET /sessions/{id} — посторонний → 403")
        void getById_outsider_shouldReturn403() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);
            ResponseEntity<SessionResponse> created = restTemplate.exchange(
                    "/sessions", HttpMethod.POST, bearerRequest(dto, studentToken), SessionResponse.class);
            Long sessionId = created.getBody().id();

            String outsiderToken = registerAndLogin("o_" + uid() + "@test.com");
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/sessions/" + sessionId, HttpMethod.GET,
                    bearerRequest(null, outsiderToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("PUT /sessions/{id}/...")
    class Transitions {

        private Long sessionId;

        @BeforeEach
        void create() {
            CreateSessionRequest dto = new CreateSessionRequest(acceptedRequestId, future(1), 60);
            ResponseEntity<SessionResponse> created = restTemplate.exchange(
                    "/sessions", HttpMethod.POST, bearerRequest(dto, studentToken), SessionResponse.class);
            sessionId = created.getBody().id();
        }

        @Test
        @DisplayName("reschedule студентом → RESCHEDULED")
        void reschedule_byStudent_shouldReturnRescheduled() {
            RescheduleSessionRequest dto = new RescheduleSessionRequest(future(3), 45, "Перенос");

            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    "/sessions/" + sessionId + "/reschedule", HttpMethod.PUT,
                    bearerRequest(dto, studentToken), SessionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(MentoringSessionStatus.RESCHEDULED);
            assertThat(response.getBody().durationMinutes()).isEqualTo(45);
        }

        @Test
        @DisplayName("cancel ментором → CANCELLED")
        void cancel_byMentor_shouldReturnCancelled() {
            CancelSessionRequest dto = new CancelSessionRequest("Не получится");

            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    "/sessions/" + sessionId + "/cancel", HttpMethod.PUT,
                    bearerRequest(dto, mentorToken), SessionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(MentoringSessionStatus.CANCELLED);
            assertThat(response.getBody().cancelReason()).isEqualTo("Не получится");
        }

        @Test
        @DisplayName("complete до планового времени → 422")
        void complete_beforeTime_shouldReturn422() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/sessions/" + sessionId + "/complete", HttpMethod.PUT,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OffsetDateTime future(long daysFromNow) {
        return OffsetDateTime.now().plusDays(daysFromNow).withNano(0);
    }

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

    private void createStudentProfile(String token) {
        StudentProfileRequest req = new StudentProfileRequest(
                "Студент", "Иванов", null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/student", HttpMethod.PUT,
                bearerRequest(req, token), StudentProfileResponse.class);
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

    private void acceptRequest(String token, Long reqId) {
        restTemplate.exchange("/mentoring/requests/" + reqId + "/accept", HttpMethod.PUT,
                bearerRequest(null, token), MentoringRequestResponse.class);
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
