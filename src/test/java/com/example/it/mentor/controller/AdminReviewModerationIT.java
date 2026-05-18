package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.dashboard.MentorStatsResponse;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.review.CreateReviewRequest;
import com.example.it.mentor.dto.review.ModerateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Review;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import com.example.it.mentor.repository.ReviewRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("PUT /admin/reviews/{id}/moderate IT")
class AdminReviewModerationIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private AdminAuditLogRepository auditLogRepository;

    private static final ParameterizedTypeReference<PagedResponse<ReviewResponse>> PAGED_REVIEW =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String mentorToken;
    private String adminToken;
    private Long mentorProfileId;
    private Long reviewId;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        String studentEmail = "mod_student_" + uid() + "@test.com";
        String mentorEmail = "mod_mentor_" + uid() + "@test.com";
        String adminEmail = "mod_admin_" + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);
        adminUserId = userRepository.findByEmailAndDeletedFalse(adminEmail).orElseThrow().getId();

        createStudentProfile(studentToken);
        mentorProfileId = createMentorProfile(mentorToken);

        Long requestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, requestId);
        completeRequest(studentToken, requestId);

        reviewId = createReview(studentToken, requestId, 5, "Отличный ментор");
    }

    // ── PUT /admin/reviews/{id}/moderate ──────────────────────────────────────

    @Test
    @DisplayName("ADMIN скрывает отзыв → 200, поля и audit обновлены")
    void moderate_byAdmin_shouldHideAndWriteAudit() {
        ResponseEntity<ReviewResponse> response = restTemplate.exchange(
                "/admin/reviews/" + reviewId + "/moderate", HttpMethod.PUT,
                bearerRequest(new ModerateReviewRequest(ReviewModerationStatus.HIDDEN), adminToken),
                ReviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().moderationStatus()).isEqualTo(ReviewModerationStatus.HIDDEN);

        Review persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getModerationStatus()).isEqualTo(ReviewModerationStatus.HIDDEN);
        assertThat(persisted.getModeratedBy()).isEqualTo(adminUserId);
        assertThat(persisted.getModeratedAt()).isNotNull();

        assertThat(auditLogRepository.findAll())
                .anyMatch(e -> e.getAction() == AuditAction.REVIEW_MODERATED
                        && reviewId.equals(e.getTargetId())
                        && adminUserId.equals(e.getAdminUserId()));
    }

    @Test
    @DisplayName("после HIDDEN — публичный список отзывов не содержит этот отзыв")
    void moderate_thenPublicList_shouldExcludeHidden() {
        hide(reviewId);

        ResponseEntity<PagedResponse<ReviewResponse>> response = restTemplate.exchange(
                "/profiles/mentors/" + mentorProfileId + "/reviews", HttpMethod.GET,
                noAuthRequest(), PAGED_REVIEW);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .noneMatch(r -> reviewId.equals(r.id()));
    }

    @Test
    @DisplayName("после HIDDEN — /mentor-stats/me не учитывает скрытый отзыв")
    void moderate_thenMentorStats_shouldExcludeHidden() {
        hide(reviewId);

        ResponseEntity<MentorStatsResponse> response = restTemplate.exchange(
                "/mentor-stats/me", HttpMethod.GET,
                bearerRequest(null, mentorToken), MentorStatsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().reviewCount()).isZero();
        assertThat(response.getBody().averageRating()).isZero();
    }

    @Test
    @DisplayName("обратный переход HIDDEN → VISIBLE возвращает отзыв в публичный список")
    void moderate_reverseToVisible_shouldReturnReviewBack() {
        hide(reviewId);
        moderate(reviewId, ReviewModerationStatus.VISIBLE);

        ResponseEntity<PagedResponse<ReviewResponse>> response = restTemplate.exchange(
                "/profiles/mentors/" + mentorProfileId + "/reviews", HttpMethod.GET,
                noAuthRequest(), PAGED_REVIEW);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .anyMatch(r -> reviewId.equals(r.id()));
    }

    @Test
    @DisplayName("STUDENT → 403")
    void moderate_byStudent_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/reviews/" + reviewId + "/moderate", HttpMethod.PUT,
                bearerRequest(new ModerateReviewRequest(ReviewModerationStatus.HIDDEN), studentToken),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("без токена → 401")
    void moderate_anonymous_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/reviews/" + reviewId + "/moderate", HttpMethod.PUT,
                new HttpEntity<>(new ModerateReviewRequest(ReviewModerationStatus.HIDDEN), headers),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("отзыв не найден → 404")
    void moderate_notFound_shouldReturn404() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/reviews/999999999/moderate", HttpMethod.PUT,
                bearerRequest(new ModerateReviewRequest(ReviewModerationStatus.HIDDEN), adminToken),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void hide(Long id) {
        moderate(id, ReviewModerationStatus.HIDDEN);
    }

    private void moderate(Long id, ReviewModerationStatus status) {
        restTemplate.exchange("/admin/reviews/" + id + "/moderate", HttpMethod.PUT,
                bearerRequest(new ModerateReviewRequest(status), adminToken), ReviewResponse.class);
    }

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

    private void grantAdminRole(String email) {
        User user = userRepository.findWithRolesByEmailAndDeletedFalse(email).orElseThrow();
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(adminRole);
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

    private Long createReview(String token, Long requestId, int rating, String comment) {
        CreateReviewRequest dto = new CreateReviewRequest(requestId, rating, comment);
        ResponseEntity<ReviewResponse> resp = restTemplate.exchange(
                "/reviews", HttpMethod.POST, bearerRequest(dto, token), ReviewResponse.class);
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

    private <T> HttpEntity<T> noAuthRequest() {
        return new HttpEntity<>(new HttpHeaders());
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
