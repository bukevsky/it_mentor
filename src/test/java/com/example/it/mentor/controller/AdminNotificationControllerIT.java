package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /admin/notifications/outbox IT")
class AdminNotificationControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin_notif_" + uid() + "@test.com";
        String studentEmail = "student_notif_" + uid() + "@test.com";

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);
        studentToken = registerAndLogin(studentEmail);
    }

    @Test
    @DisplayName("admin → 200 с пустым контентом")
    void getOutbox_asAdmin_returns200() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/notifications/outbox", HttpMethod.GET,
                bearer(adminToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("admin с фильтром status=PENDING → 200")
    void getOutbox_withStatusFilter_returns200() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/notifications/outbox?status=PENDING", HttpMethod.GET,
                bearer(adminToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("student → 403")
    void getOutbox_asStudent_returns403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/notifications/outbox", HttpMethod.GET,
                bearer(studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("анонимный запрос → 401")
    void getOutbox_anonymous_returns401() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/admin/notifications/outbox", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
