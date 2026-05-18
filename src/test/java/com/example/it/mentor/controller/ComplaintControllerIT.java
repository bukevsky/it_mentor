package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.dto.complaint.CreateComplaintRequest;
import com.example.it.mentor.dto.complaint.ResolveComplaintRequest;
import com.example.it.mentor.entity.AdminAuditLog;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("Complaints IT")
class ComplaintControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private AdminAuditLogRepository auditLogRepository;

    private static final ParameterizedTypeReference<PagedResponse<ComplaintResponse>> PAGED_COMPLAINTS =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String adminToken;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        String studentEmail = "complaint_student_" + uid() + "@test.com";
        String adminEmail = "complaint_admin_" + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);
        adminUserId = userRepository.findByEmailAndDeletedFalse(adminEmail).orElseThrow().getId();
    }

    // ── POST /complaints ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /complaints")
    class CreateComplaint {

        @Test
        @DisplayName("STUDENT создаёт жалобу → 201, поля заполнены")
        void create_byStudent_shouldReturn201() {
            CreateComplaintRequest dto = new CreateComplaintRequest(
                    ComplaintTargetType.REVIEW, 42L, "Спам в отзыве");

            ResponseEntity<ComplaintResponse> response = restTemplate.exchange(
                    "/complaints", HttpMethod.POST,
                    bearerRequest(dto, studentToken), ComplaintResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ComplaintResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.id()).isNotNull();
            assertThat(body.targetType()).isEqualTo(ComplaintTargetType.REVIEW);
            assertThat(body.targetId()).isEqualTo(42L);
            assertThat(body.reason()).isEqualTo("Спам в отзыве");
            assertThat(body.status()).isEqualTo(ComplaintStatus.OPEN);
            assertThat(body.resolvedBy()).isNull();
            assertThat(body.resolvedAt()).isNull();
        }

        @Test
        @DisplayName("без токена → 401")
        void create_anonymous_shouldReturn401() {
            CreateComplaintRequest dto = new CreateComplaintRequest(
                    ComplaintTargetType.USER, 1L, "abuse");

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    "/complaints", dto, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("пустой reason → 400")
        void create_blankReason_shouldReturn400() {
            CreateComplaintRequest dto = new CreateComplaintRequest(
                    ComplaintTargetType.REVIEW, 1L, "  ");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/complaints", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("reason > 2000 символов → 400")
        void create_reasonTooLong_shouldReturn400() {
            CreateComplaintRequest dto = new CreateComplaintRequest(
                    ComplaintTargetType.REVIEW, 1L, "x".repeat(2001));

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/complaints", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── GET /admin/complaints ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/complaints")
    class ListComplaints {

        @BeforeEach
        void seed() {
            createComplaint(ComplaintTargetType.REVIEW, 1L, "spam #1", studentToken);
            createComplaint(ComplaintTargetType.USER, 2L, "abuse #2", studentToken);
            createComplaint(ComplaintTargetType.REVIEW, 3L, "spam #3", studentToken);
        }

        @Test
        @DisplayName("ADMIN получает список → 200")
        void list_byAdmin_shouldReturn200() {
            ResponseEntity<PagedResponse<ComplaintResponse>> response = restTemplate.exchange(
                    "/admin/complaints?page=0&size=50", HttpMethod.GET,
                    bearerRequest(null, adminToken), PAGED_COMPLAINTS);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content().size()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("STUDENT → 403")
        void list_byStudent_shouldReturn403() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/admin/complaints", HttpMethod.GET,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("без токена → 401")
        void list_anonymous_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/admin/complaints", Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("фильтр targetType=REVIEW отсекает USER")
        void list_filterByTargetType_shouldReturnOnlyMatching() {
            ResponseEntity<PagedResponse<ComplaintResponse>> response = restTemplate.exchange(
                    "/admin/complaints?targetType=REVIEW&page=0&size=50", HttpMethod.GET,
                    bearerRequest(null, adminToken), PAGED_COMPLAINTS);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allMatch(c -> c.targetType() == ComplaintTargetType.REVIEW);
        }

        @Test
        @DisplayName("фильтр status=OPEN возвращает только OPEN")
        void list_filterByStatus_shouldReturnOnlyOpen() {
            ResponseEntity<PagedResponse<ComplaintResponse>> response = restTemplate.exchange(
                    "/admin/complaints?status=OPEN&page=0&size=50", HttpMethod.GET,
                    bearerRequest(null, adminToken), PAGED_COMPLAINTS);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allMatch(c -> c.status() == ComplaintStatus.OPEN);
        }

        @Test
        @DisplayName("невалидный sort-field фолбэк на createdAt,desc")
        void list_invalidSortField_shouldFallbackAndReturn200() {
            ResponseEntity<PagedResponse<ComplaintResponse>> response = restTemplate.exchange(
                    "/admin/complaints?sort=hackedField,desc&page=0&size=10", HttpMethod.GET,
                    bearerRequest(null, adminToken), PAGED_COMPLAINTS);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ── PUT /admin/complaints/{id}/resolve ────────────────────────────────────

    @Nested
    @DisplayName("PUT /admin/complaints/{id}/resolve")
    class ResolveComplaint {

        private Long openComplaintId;

        @BeforeEach
        void seed() {
            openComplaintId = createComplaint(
                    ComplaintTargetType.REVIEW, 7L, "to resolve", studentToken);
        }

        @Test
        @DisplayName("ADMIN решает жалобу → 200, audit-log создан")
        void resolve_happyPath_shouldReturn200AndWriteAudit() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(
                    ComplaintStatus.RESOLVED, "Отзыв скрыт");

            ResponseEntity<ComplaintResponse> response = restTemplate.exchange(
                    "/admin/complaints/" + openComplaintId + "/resolve", HttpMethod.PUT,
                    bearerRequest(dto, adminToken), ComplaintResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ComplaintResponse body = response.getBody();
            assertThat(body.status()).isEqualTo(ComplaintStatus.RESOLVED);
            assertThat(body.resolution()).isEqualTo("Отзыв скрыт");
            assertThat(body.resolvedBy()).isEqualTo(adminUserId);
            assertThat(body.resolvedAt()).isNotNull();

            assertThat(auditLogRepository.findAll())
                    .anyMatch(e -> e.getAction() == AuditAction.COMPLAINT_RESOLVED
                            && openComplaintId.equals(e.getTargetId())
                            && adminUserId.equals(e.getAdminUserId()));
        }

        @Test
        @DisplayName("повторный resolve уже закрытой жалобы → 409")
        void resolve_alreadyClosed_shouldReturn409() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(
                    ComplaintStatus.REJECTED, "fake");
            restTemplate.exchange("/admin/complaints/" + openComplaintId + "/resolve",
                    HttpMethod.PUT, bearerRequest(dto, adminToken), ComplaintResponse.class);

            ResponseEntity<Object> second = restTemplate.exchange(
                    "/admin/complaints/" + openComplaintId + "/resolve", HttpMethod.PUT,
                    bearerRequest(dto, adminToken), Object.class);

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("status=OPEN в DTO → 422 (бизнес-правило)")
        void resolve_statusOpen_shouldReturn422() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.OPEN, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/admin/complaints/" + openComplaintId + "/resolve", HttpMethod.PUT,
                    bearerRequest(dto, adminToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("жалоба не найдена → 404")
        void resolve_notFound_shouldReturn404() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(
                    ComplaintStatus.RESOLVED, "x");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/admin/complaints/999999999/resolve", HttpMethod.PUT,
                    bearerRequest(dto, adminToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("STUDENT → 403")
        void resolve_byStudent_shouldReturn403() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(
                    ComplaintStatus.RESOLVED, "x");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/admin/complaints/" + openComplaintId + "/resolve", HttpMethod.PUT,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Long createComplaint(ComplaintTargetType type, Long targetId,
                                 String reason, String token) {
        CreateComplaintRequest dto = new CreateComplaintRequest(type, targetId, reason);
        ResponseEntity<ComplaintResponse> resp = restTemplate.exchange(
                "/complaints", HttpMethod.POST,
                bearerRequest(dto, token), ComplaintResponse.class);
        return resp.getBody().id();
    }

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
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
