package com.example.it.mentor.controller;

import com.example.it.mentor.dto.AdminRoleRequest;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
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
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("PUT /admin/users/{userId}/role IT")
class AdminControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private String adminToken;
    private String studentToken;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin_" + uid() + "@test.com";
        String targetEmail = "target_" + uid() + "@test.com";

        registerAndLogin(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);

        studentToken = registerAndLogin(targetEmail);
        targetUserId = userRepository.findByEmailAndDeletedFalse(targetEmail).orElseThrow().getId();
    }

    @Test
    @DisplayName("назначить MENTOR → 200, роль обновлена в БД")
    void assignMentor_shouldReturn200AndUpdateRole() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.MENTOR), adminToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        User updated = userRepository.findById(targetUserId).orElseThrow();
        assertThat(updated.getRoles()).anyMatch(r -> r.getCode() == RoleCode.MENTOR);
        assertThat(updated.getRoles()).noneMatch(r -> r.getCode() == RoleCode.STUDENT);
    }

    @Test
    @DisplayName("назначить STUDENT после MENTOR → 200, роль обновлена в БД")
    void assignStudent_shouldReturn200AndUpdateRole() {
        // сначала делаем пользователя ментором
        restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.MENTOR), adminToken), Void.class);

        // затем возвращаем роль студента
        ResponseEntity<Void> response = restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.STUDENT), adminToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        User updated = userRepository.findById(targetUserId).orElseThrow();
        assertThat(updated.getRoles()).anyMatch(r -> r.getCode() == RoleCode.STUDENT);
        assertThat(updated.getRoles()).noneMatch(r -> r.getCode() == RoleCode.MENTOR);
    }

    @Test
    @DisplayName("назначить ADMIN → 422")
    void assignAdmin_shouldReturn422() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.ADMIN), adminToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    @DisplayName("не-admin пользователь → 403")
    void nonAdminUser_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.MENTOR), studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("анонимный запрос → 401")
    void anonymous_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users/" + targetUserId + "/role", HttpMethod.PUT,
                new HttpEntity<>(new AdminRoleRequest(RoleCode.MENTOR), headers), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("пользователь не найден → 404")
    void userNotFound_shouldReturn404() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/users/999999999/role", HttpMethod.PUT,
                bearerRequest(new AdminRoleRequest(RoleCode.MENTOR), adminToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
        User user = userRepository.findByEmailAndDeletedFalse(email).orElseThrow();
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
