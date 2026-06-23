package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.dict.*;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import com.example.it.mentor.repository.DictCityRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("AdminDictionaryController IT")
class AdminDictionaryControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private AdminAuditLogRepository auditLogRepository;
    @Autowired private DictCityRepository cityRepository;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin_" + uid() + "@test.com";
        String studentEmail = "student_" + uid() + "@test.com";

        register(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);

        studentToken = registerAndLogin(studentEmail);
    }

    // ── Cities lifecycle ──────────────────────────────────────────────────────

    @Test
    @DisplayName("city_fullLifecycle_createUpdateDeleteRestore")
    void city_fullLifecycle_createUpdateDeleteRestore() {
        // CREATE
        CreateCityRequest createDto = new CreateCityRequest("Тест-Сити-" + uid(), "Область", "Россия");
        ResponseEntity<CityResponse> created = restTemplate.exchange(
                "/admin/dictionaries/cities", HttpMethod.POST,
                bearerRequest(createDto, adminToken), CityResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long cityId = created.getBody().id();
        assertThat(created.getBody().active()).isTrue();

        // public GET contains it
        List<CityResponse> publicList = getCities();
        assertThat(publicList).anyMatch(c -> c.id().equals(cityId));

        // admin GET contains it
        List<CityResponse> adminList = adminGetCities();
        assertThat(adminList).anyMatch(c -> c.id().equals(cityId) && c.active());

        // UPDATE
        UpdateCityRequest updateDto = new UpdateCityRequest(createDto.name(), "Новая обл", "Россия", true);
        ResponseEntity<CityResponse> updated = restTemplate.exchange(
                "/admin/dictionaries/cities/" + cityId, HttpMethod.PUT,
                bearerRequest(updateDto, adminToken), CityResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().region()).isEqualTo("Новая обл");

        // DELETE (soft)
        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/admin/dictionaries/cities/" + cityId, HttpMethod.DELETE,
                bearerRequest(null, adminToken), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // public GET no longer contains it
        assertThat(getCities()).noneMatch(c -> c.id().equals(cityId));
        // admin GET contains with active=false
        assertThat(adminGetCities()).anyMatch(c -> c.id().equals(cityId) && !c.active());

        // DELETE again → 409
        ResponseEntity<Object> deletedAgain = restTemplate.exchange(
                "/admin/dictionaries/cities/" + cityId, HttpMethod.DELETE,
                bearerRequest(null, adminToken), Object.class);
        assertThat(deletedAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // RESTORE
        ResponseEntity<CityResponse> restored = restTemplate.exchange(
                "/admin/dictionaries/cities/" + cityId + "/restore", HttpMethod.PUT,
                bearerRequest(null, adminToken), CityResponse.class);
        assertThat(restored.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restored.getBody().active()).isTrue();

        // public GET contains again
        assertThat(getCities()).anyMatch(c -> c.id().equals(cityId));

        // restore again → 409
        ResponseEntity<Object> restoredAgain = restTemplate.exchange(
                "/admin/dictionaries/cities/" + cityId + "/restore", HttpMethod.PUT,
                bearerRequest(null, adminToken), Object.class);
        assertThat(restoredAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // duplicate name → 409
        ResponseEntity<Object> duplicate = restTemplate.exchange(
                "/admin/dictionaries/cities", HttpMethod.POST,
                bearerRequest(createDto, adminToken), Object.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Audit log has DICTIONARY_CHANGED entries
        assertThat(auditLogRepository.findAll())
                .anyMatch(e -> e.getAction() == AuditAction.DICTIONARY_CHANGED
                        && e.getTargetId().equals(cityId));
    }

    @Test
    @DisplayName("city_anonymous_returns401")
    void city_anonymous_returns401() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/dictionaries/cities", HttpMethod.GET,
                new HttpEntity<>((HttpHeaders) null), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("city_student_returns403")
    void city_student_returns403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/dictionaries/cities", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("city_notFound_returns404")
    void city_notFound_returns404() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/admin/dictionaries/cities/999999999", HttpMethod.DELETE,
                bearerRequest(null, adminToken), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Skills lifecycle ──────────────────────────────────────────────────────

    @Test
    @DisplayName("skill_fullLifecycle_createDeleteRestore")
    void skill_fullLifecycle_createDeleteRestore() {
        CreateSkillRequest dto = new CreateSkillRequest("Skill-" + uid(), "Backend");
        ResponseEntity<SkillResponse> created = restTemplate.exchange(
                "/admin/dictionaries/skills", HttpMethod.POST,
                bearerRequest(dto, adminToken), SkillResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long skillId = created.getBody().id();

        restTemplate.exchange("/admin/dictionaries/skills/" + skillId, HttpMethod.DELETE,
                bearerRequest(null, adminToken), Void.class);

        restTemplate.exchange("/admin/dictionaries/skills/" + skillId + "/restore", HttpMethod.PUT,
                bearerRequest(null, adminToken), SkillResponse.class);

        assertThat(restTemplate.exchange("/admin/dictionaries/skills", HttpMethod.GET,
                bearerRequest(null, adminToken),
                new ParameterizedTypeReference<List<SkillResponse>>() {}).getBody())
                .anyMatch(s -> s.id().equals(skillId) && s.active());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<CityResponse> getCities() {
        return restTemplate.exchange("/dictionaries/cities", HttpMethod.GET,
                new HttpEntity<>((HttpHeaders) null),
                new ParameterizedTypeReference<List<CityResponse>>() {}).getBody();
    }

    private List<CityResponse> adminGetCities() {
        return restTemplate.exchange("/admin/dictionaries/cities", HttpMethod.GET,
                bearerRequest(null, adminToken),
                new ParameterizedTypeReference<List<CityResponse>>() {}).getBody();
    }

    private void register(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
    }

    private String registerAndLogin(String email) {
        register(email);
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
