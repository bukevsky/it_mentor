package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.admin.AdminUserResponse;
import com.example.it.mentor.dto.admin.AdminUsersStatsResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
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
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /admin/users IT")
class AdminUsersControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<AdminUserResponse>> PAGED_USER =
            new ParameterizedTypeReference<>() {};

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin_u_" + uid() + "@test.com";
        String studentEmail = "student_u_" + uid() + "@test.com";

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);

        studentToken = registerAndLogin(studentEmail);
    }

    @Test
    @DisplayName("GET /admin/users администратор → 200, непустой список")
    void getUsers_admin_shouldReturn200WithData() {
        ResponseEntity<PagedResponse<AdminUserResponse>> response = restTemplate.exchange(
                "/admin/users", HttpMethod.GET,
                bearerRequest(null, adminToken), PAGED_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalElements()).isPositive();
    }

    @Test
    @DisplayName("GET /admin/users не-администратор → 403")
    void getUsers_nonAdmin_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /admin/users без токена → 401")
    void getUsers_unauthenticated_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/admin/users", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /admin/users фильтр q по email → только совпадающие")
    void getUsers_filterByQ_shouldReturnMatchingUsers() {
        String unique = "unique_" + uid();
        String email = unique + "@filter.com";
        registerAndLogin(email);

        ResponseEntity<PagedResponse<AdminUserResponse>> response = restTemplate.exchange(
                "/admin/users?q=" + unique, HttpMethod.GET,
                bearerRequest(null, adminToken), PAGED_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
        assertThat(response.getBody().content().get(0).email()).containsIgnoringCase(unique);
    }

    @Test
    @DisplayName("GET /admin/users фильтр по role=STUDENT → только студенты")
    void getUsers_filterByRole_shouldReturnOnlyThatRole() {
        ResponseEntity<PagedResponse<AdminUserResponse>> response = restTemplate.exchange(
                "/admin/users?role=STUDENT", HttpMethod.GET,
                bearerRequest(null, adminToken), PAGED_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        response.getBody().content().forEach(u ->
                assertThat(u.roles()).contains("STUDENT"));
    }

    @Test
    @DisplayName("GET /admin/users невалидный sort → 200 с дефолтной сортировкой")
    void getUsers_invalidSortField_shouldReturn200WithDefault() {
        ResponseEntity<PagedResponse<AdminUserResponse>> response = restTemplate.exchange(
                "/admin/users?sort=invalidField,desc", HttpMethod.GET,
                bearerRequest(null, adminToken), PAGED_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /admin/users/stats администратор → 200, totalUsers > 0")
    void getUsersStats_admin_shouldReturn200WithStats() {
        ResponseEntity<AdminUsersStatsResponse> response = restTemplate.exchange(
                "/admin/users/stats", HttpMethod.GET,
                bearerRequest(null, adminToken), AdminUsersStatsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalUsers()).isPositive();
        assertThat(response.getBody().byRole()).isNotNull();
        assertThat(response.getBody().byStatus()).isNotNull();
    }

    @Test
    @DisplayName("GET /admin/users/stats не-администратор → 403")
    void getUsersStats_nonAdmin_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users/stats", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
