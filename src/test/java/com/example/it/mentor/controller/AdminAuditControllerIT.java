package com.example.it.mentor.controller;

import com.example.it.mentor.dto.AdminRoleRequest;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.audit.AuditLogResponse;
import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.dto.complaint.CreateComplaintRequest;
import com.example.it.mentor.dto.complaint.ResolveComplaintRequest;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /admin/audit IT")
class AdminAuditControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<AuditLogResponse>> PAGED_AUDIT =
            new ParameterizedTypeReference<>() {};

    private String adminToken;
    private String studentToken;
    private Long adminUserId;
    private Long targetUserId;
    private Long resolvedComplaintId;
    private OffsetDateTime windowStart;

    @BeforeEach
    void setUp() {
        windowStart = OffsetDateTime.now().minusMinutes(1);

        String adminEmail = "audit_admin_" + uid() + "@test.com";
        String targetEmail = "audit_target_" + uid() + "@test.com";
        String studentEmail = "audit_student_" + uid() + "@test.com";

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);
        adminUserId = userRepository.findByEmailAndDeletedFalse(adminEmail).orElseThrow().getId();

        studentToken = registerAndLogin(studentEmail);

        registerAndLogin(targetEmail);
        targetUserId = userRepository.findByEmailAndDeletedFalse(targetEmail).orElseThrow().getId();

        // ROLE_CHANGED #1: STUDENT → MENTOR
        assignRole(targetUserId, RoleCode.MENTOR, adminToken);
        // ROLE_CHANGED #2: MENTOR → STUDENT
        assignRole(targetUserId, RoleCode.STUDENT, adminToken);

        // COMPLAINT_RESOLVED
        Long complaintId = createComplaint(studentToken);
        resolveComplaint(complaintId, ComplaintStatus.RESOLVED, adminToken);
        resolvedComplaintId = complaintId;
    }

    // ── happy paths ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN получает все записи аудита → 200, есть наши события")
    void list_byAdmin_shouldReturnSeededEvents() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?adminUserId=" + adminUserId + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var entries = response.getBody().content();
        assertThat(entries).hasSizeGreaterThanOrEqualTo(3);
        assertThat(entries).filteredOn(e -> e.action() == AuditAction.ROLE_CHANGED).hasSize(2);
        assertThat(entries).filteredOn(e -> e.action() == AuditAction.COMPLAINT_RESOLVED).hasSize(1);
    }

    @Test
    @DisplayName("фильтр action=ROLE_CHANGED отдаёт только смены ролей")
    void list_filterByAction_shouldReturnOnlyRoleChanges() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?action=ROLE_CHANGED&adminUserId=" + adminUserId + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .isNotEmpty()
                .allMatch(e -> e.action() == AuditAction.ROLE_CHANGED);
    }

    @Test
    @DisplayName("фильтр action=COMPLAINT_RESOLVED содержит запись о resolved жалобе")
    void list_filterByComplaintResolved_shouldContainOurComplaint() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?action=COMPLAINT_RESOLVED&adminUserId=" + adminUserId + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .anyMatch(e -> e.action() == AuditAction.COMPLAINT_RESOLVED
                        && resolvedComplaintId.equals(e.targetId()));
    }

    @Test
    @DisplayName("фильтр adminUserId возвращает только записи указанного админа")
    void list_filterByAdminUserId_shouldScopeToAdmin() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?adminUserId=" + adminUserId + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .isNotEmpty()
                .allMatch(e -> adminUserId.equals(e.adminUserId()));
    }

    @Test
    @DisplayName("фильтр from/to отсекает по диапазону")
    void list_filterByPeriod_shouldRespectRange() {
        OffsetDateTime to = OffsetDateTime.now().plusMinutes(1);

        ResponseEntity<PagedResponse<AuditLogResponse>> inWindow = restTemplate.exchange(
                "/admin/audit?adminUserId=" + adminUserId
                        + "&from=" + isoUtc(windowStart) + "&to=" + isoUtc(to)
                        + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);
        assertThat(inWindow.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inWindow.getBody().content()).hasSizeGreaterThanOrEqualTo(3);

        ResponseEntity<PagedResponse<AuditLogResponse>> beforeOurEvents = restTemplate.exchange(
                "/admin/audit?adminUserId=" + adminUserId
                        + "&from=" + isoUtc(windowStart.minusDays(1))
                        + "&to=" + isoUtc(windowStart.minusMinutes(30))
                        + "&page=0&size=50",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);
        assertThat(beforeOurEvents.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(beforeOurEvents.getBody().content()).isEmpty();
    }

    private String isoUtc(OffsetDateTime ts) {
        return ts.withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Test
    @DisplayName("пагинация size=1 возвращает одну запись")
    void list_paginationSizeOne_shouldReturnSingleEntry() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?adminUserId=" + adminUserId + "&page=0&size=1",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("невалидный sort-field фолбэк на createdAt,desc")
    void list_invalidSortField_shouldFallbackAndReturn200() {
        ResponseEntity<PagedResponse<AuditLogResponse>> response = restTemplate.exchange(
                "/admin/audit?sort=hackedField,desc&adminUserId=" + adminUserId
                        + "&page=0&size=10",
                HttpMethod.GET, bearerRequest(null, adminToken), PAGED_AUDIT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── security ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("STUDENT → 403")
    void list_byStudent_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/audit", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("без токена → 401")
    void list_anonymous_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/admin/audit", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void assignRole(Long userId, RoleCode role, String token) {
        restTemplate.exchange("/admin/users/" + userId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(role), token), Void.class);
    }

    private Long createComplaint(String token) {
        CreateComplaintRequest dto = new CreateComplaintRequest(
                ComplaintTargetType.REVIEW, 100L, "audit-it complaint");
        ResponseEntity<ComplaintResponse> resp = restTemplate.exchange(
                "/complaints", HttpMethod.POST,
                bearerRequest(dto, token), ComplaintResponse.class);
        return resp.getBody().id();
    }

    private void resolveComplaint(Long id, ComplaintStatus status, String token) {
        restTemplate.exchange("/admin/complaints/" + id + "/resolve", HttpMethod.PUT,
                bearerRequest(new ResolveComplaintRequest(status, "ok"), token),
                ComplaintResponse.class);
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
