package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
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
@DisplayName("GET /profiles/students IT")
class StudentCatalogControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<StudentProfileResponse>> PAGED_STUDENT =
            new ParameterizedTypeReference<>() {};

    private String mentorToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String mentorEmail  = "mentor_cat_" + uid() + "@test.com";
        String studentEmail = "student_cat_" + uid() + "@test.com";

        registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);
        mentorToken = login(mentorEmail);

        studentToken = registerAndLogin(studentEmail);
        createStudentProfile(studentToken, studentEmail);
    }

    @Test
    @DisplayName("GET /profiles/students ментор → 200, ненулевая страница")
    void search_mentor_shouldReturn200() {
        ResponseEntity<PagedResponse<StudentProfileResponse>> response = restTemplate.exchange(
                "/profiles/students", HttpMethod.GET,
                bearerRequest(null, mentorToken), PAGED_STUDENT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalElements()).isPositive();
    }

    @Test
    @DisplayName("GET /profiles/students студент → 403")
    void search_student_shouldReturn403() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/profiles/students", HttpMethod.GET,
                bearerRequest(null, studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /profiles/students без токена → 401")
    void search_unauthenticated_shouldReturn401() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/profiles/students", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /profiles/students q по имени → совпадающие результаты")
    void search_withQFilter_shouldReturnMatchingProfiles() {
        String uniqueName = "Уникальный" + uid();
        String email = "stu_q_" + uid() + "@test.com";
        String token = registerAndLogin(email);
        createStudentProfileWithName(token, uniqueName, "Тестов");

        ResponseEntity<PagedResponse<StudentProfileResponse>> response = restTemplate.exchange(
                "/profiles/students?q=" + uniqueName, HttpMethod.GET,
                bearerRequest(null, mentorToken), PAGED_STUDENT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
        assertThat(response.getBody().content().get(0).firstName()).isEqualTo(uniqueName);
    }

    @Test
    @DisplayName("GET /profiles/students с пагинацией → size ограничивает список")
    void search_withPagination_shouldRespectSizeParam() {
        ResponseEntity<PagedResponse<StudentProfileResponse>> response = restTemplate.exchange(
                "/profiles/students?size=1", HttpMethod.GET,
                bearerRequest(null, mentorToken), PAGED_STUDENT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSizeLessThanOrEqualTo(1);
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

    private void grantMentorRole(String email) {
        User user = userRepository.findWithRolesByEmailAndDeletedFalse(email).orElseThrow();
        Role mentorRole = roleRepository.findByCode(RoleCode.MENTOR).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(mentorRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private void createStudentProfile(String token, String email) {
        String firstName = "Студент" + uid();
        createStudentProfileWithName(token, firstName, "Каталог");
    }

    private void createStudentProfileWithName(String token, String firstName, String lastName) {
        StudentProfileRequest req = new StudentProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/student", HttpMethod.PUT,
                bearerRequest(req, token), StudentProfileResponse.class);
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
