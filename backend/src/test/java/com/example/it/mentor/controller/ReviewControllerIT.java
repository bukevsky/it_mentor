package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.review.CreateReviewRequest;
import com.example.it.mentor.dto.review.ModerateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
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
@DisplayName("ReviewController IT")
class ReviewControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<ReviewResponse>> PAGED_REVIEW =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String mentorToken;
    private Long mentorProfileId;
    private Long completedRequestId;

    @BeforeEach
    void setUp() {
        String studentEmail = "student_" + uid() + "@test.com";
        String mentorEmail  = "mentor_"  + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken  = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        createStudentProfile(studentToken);
        mentorProfileId = createMentorProfile(mentorToken);

        Long requestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, requestId);
        completeRequest(studentToken, requestId);

        completedRequestId = requestId;
    }

    // ── POST /reviews ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /reviews")
    class CreateReview {

        @Test
        @DisplayName("студент оставляет отзыв на завершённую заявку → 201")
        void createReview_happyPath_shouldReturn201() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 5, "Отличный ментор!");

            ResponseEntity<ReviewResponse> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, studentToken), ReviewResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().rating()).isEqualTo(5);
            assertThat(response.getBody().comment()).isEqualTo("Отличный ментор!");
            assertThat(response.getBody().id()).isNotNull();
        }

        @Test
        @DisplayName("без токена → 401")
        void createReview_unauthenticated_shouldReturn401() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 5, null);

            ResponseEntity<Object> response = restTemplate.postForEntity("/reviews", dto, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("ментор пытается оставить отзыв → 403")
        void createReview_byMentor_shouldReturn403() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 5, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("посторонний студент → 403")
        void createReview_outsider_shouldReturn403() {
            String outsiderToken = registerAndLogin("outsider_" + uid() + "@test.com");
            createStudentProfile(outsiderToken);
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 5, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, outsiderToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("дубликат отзыва → 409")
        void createReview_duplicate_shouldReturn409() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 4, null);
            restTemplate.exchange("/reviews", HttpMethod.POST, bearerRequest(dto, studentToken), ReviewResponse.class);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("заявка не завершена → 422")
        void createReview_requestNotCompleted_shouldReturn422() {
            String s2Email = "s2_" + uid() + "@test.com";
            String m2Email = "m2_" + uid() + "@test.com";
            String s2Token = registerAndLogin(s2Email);
            String m2Token = registerAndLogin(m2Email);
            grantMentorRole(m2Email);
            createStudentProfile(s2Token);
            Long m2ProfileId = createMentorProfile(m2Token);
            Long acceptedRequestId = createRequest(s2Token, m2ProfileId);
            acceptRequest(m2Token, acceptedRequestId);

            CreateReviewRequest dto = new CreateReviewRequest(acceptedRequestId, 5, null);
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, s2Token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("заявка не найдена → 404")
        void createReview_requestNotFound_shouldReturn404() {
            CreateReviewRequest dto = new CreateReviewRequest(999999L, 5, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("рейтинг = 0 → 400 (Bean Validation)")
        void createReview_ratingZero_shouldReturn400() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 0, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("рейтинг = 6 → 400 (Bean Validation)")
        void createReview_ratingAboveMax_shouldReturn400() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 6, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── GET /reviews/by-request/{requestId} ──────────────────────────────────

    @Nested
    @DisplayName("GET /reviews/by-request/{requestId}")
    class GetByRequest {

        @BeforeEach
        void createReview() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 3, "Норм");
            restTemplate.exchange("/reviews", HttpMethod.POST, bearerRequest(dto, studentToken), ReviewResponse.class);
        }

        @Test
        @DisplayName("студент получает свой отзыв → 200")
        void getByRequest_student_shouldReturn200() {
            ResponseEntity<ReviewResponse> response = restTemplate.exchange(
                    "/reviews/by-request/" + completedRequestId, HttpMethod.GET,
                    bearerRequest(null, studentToken), ReviewResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().rating()).isEqualTo(3);
        }

        @Test
        @DisplayName("ментор видит отзыв о себе → 200")
        void getByRequest_mentor_shouldReturn200() {
            ResponseEntity<ReviewResponse> response = restTemplate.exchange(
                    "/reviews/by-request/" + completedRequestId, HttpMethod.GET,
                    bearerRequest(null, mentorToken), ReviewResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("посторонний → 403")
        void getByRequest_outsider_shouldReturn403() {
            String otherToken = registerAndLogin("other_" + uid() + "@test.com");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews/by-request/" + completedRequestId, HttpMethod.GET,
                    bearerRequest(null, otherToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("без токена → 401")
        void getByRequest_unauthenticated_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                    "/reviews/by-request/" + completedRequestId, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── GET /profiles/mentors/{id}/reviews ────────────────────────────────────

    @Nested
    @DisplayName("GET /profiles/mentors/{id}/reviews")
    class GetMentorReviews {

        @BeforeEach
        void createReview() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 4, "Хорошо");
            restTemplate.exchange("/reviews", HttpMethod.POST, bearerRequest(dto, studentToken), ReviewResponse.class);
        }

        @Test
        @DisplayName("публичный доступ без токена → 200")
        void getMentorReviews_public_shouldReturn200() {
            ResponseEntity<PagedResponse<ReviewResponse>> response = restTemplate.exchange(
                    "/profiles/mentors/" + mentorProfileId + "/reviews", HttpMethod.GET,
                    noAuthRequest(), PAGED_REVIEW);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).hasSize(1);
            assertThat(response.getBody().content().get(0).rating()).isEqualTo(4);
        }

        @Test
        @DisplayName("профиль не найден → 404")
        void getMentorReviews_mentorNotFound_shouldReturn404() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors/999999/reviews", HttpMethod.GET,
                    noAuthRequest(), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("нет отзывов → пустая страница")
        void getMentorReviews_noReviews_shouldReturnEmpty() {
            String m3Email = "m3_" + uid() + "@test.com";
            String m3Token = registerAndLogin(m3Email);
            grantMentorRole(m3Email);
            Long m3ProfileId = createMentorProfile(m3Token);

            ResponseEntity<PagedResponse<ReviewResponse>> response = restTemplate.exchange(
                    "/profiles/mentors/" + m3ProfileId + "/reviews", HttpMethod.GET,
                    noAuthRequest(), PAGED_REVIEW);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isEmpty();
        }
    }

    // ── DELETE /reviews/{id} ──────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /reviews/{id}")
    class DeleteReview {

        private Long reviewId;

        @BeforeEach
        void createReview() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 2, "Не очень");
            ResponseEntity<ReviewResponse> resp = restTemplate.exchange(
                    "/reviews", HttpMethod.POST, bearerRequest(dto, studentToken), ReviewResponse.class);
            reviewId = resp.getBody().id();
        }

        @Test
        @DisplayName("автор удаляет свой отзыв → 204")
        void deleteReview_owner_shouldReturn204() {
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/reviews/" + reviewId, HttpMethod.DELETE,
                    bearerRequest(null, studentToken), Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("ментор пытается удалить отзыв → 403")
        void deleteReview_byMentor_shouldReturn403() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews/" + reviewId, HttpMethod.DELETE,
                    bearerRequest(null, mentorToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("без токена → 401")
        void deleteReview_unauthenticated_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews/" + reviewId, HttpMethod.DELETE,
                    noAuthRequest(), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("не найден → 404")
        void deleteReview_notFound_shouldReturn404() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/reviews/999999", HttpMethod.DELETE,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── видимость скрытого отзыва ─────────────────────────────────────────────

    @Nested
    @DisplayName("видимость скрытого отзыва")
    class HiddenReviewVisibility {

        private Long reviewId;
        private String adminToken;

        @BeforeEach
        void seedReviewAndAdmin() {
            CreateReviewRequest dto = new CreateReviewRequest(completedRequestId, 5, "Отлично");
            ResponseEntity<ReviewResponse> created = restTemplate.exchange(
                    "/reviews", HttpMethod.POST, bearerRequest(dto, studentToken), ReviewResponse.class);
            reviewId = created.getBody().id();

            String adminEmail = "rev_admin_" + uid() + "@test.com";
            adminToken = registerAndLogin(adminEmail);
            grantAdminRole(adminEmail);
            adminToken = login(adminEmail);
        }

        @Test
        @DisplayName("после HIDDEN отзыв исчезает из публичного списка")
        void hiddenReview_shouldBeExcludedFromPublicList() {
            hide(reviewId);

            ResponseEntity<PagedResponse<ReviewResponse>> response = restTemplate.exchange(
                    "/profiles/mentors/" + mentorProfileId + "/reviews", HttpMethod.GET,
                    noAuthRequest(), PAGED_REVIEW);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .noneMatch(r -> reviewId.equals(r.id()));
        }

        @Test
        @DisplayName("автор всё ещё видит свой скрытый отзыв через /reviews/by-request")
        void hiddenReview_authorCanStillFetchByRequest() {
            hide(reviewId);

            ResponseEntity<ReviewResponse> response = restTemplate.exchange(
                    "/reviews/by-request/" + completedRequestId, HttpMethod.GET,
                    bearerRequest(null, studentToken), ReviewResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().moderationStatus()).isEqualTo(ReviewModerationStatus.HIDDEN);
        }

        private void hide(Long id) {
            restTemplate.exchange("/admin/reviews/" + id + "/moderate", HttpMethod.PUT,
                    bearerRequest(new ModerateReviewRequest(ReviewModerationStatus.HIDDEN), adminToken),
                    ReviewResponse.class);
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

    private void grantAdminRole(String email) {
        User user = userRepository.findWithRolesByEmailAndDeletedFalse(email).orElseThrow();
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(adminRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
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

    private void completeRequest(String token, Long reqId) {
        restTemplate.exchange("/mentoring/requests/" + reqId + "/complete", HttpMethod.PUT,
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

    private <T> HttpEntity<T> noAuthRequest() {
        return new HttpEntity<>(new HttpHeaders());
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
