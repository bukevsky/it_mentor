package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.TypingRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
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
@DisplayName("POST /chats/{chatId}/typing IT")
class TypingControllerIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private String studentToken;
    private String mentorToken;
    private Long chatId;

    @BeforeEach
    void setUp() {
        String studentEmail = "student_" + uid() + "@test.com";
        String mentorEmail  = "mentor_"  + uid() + "@test.com";

        studentToken = registerAndLogin(studentEmail);
        mentorToken  = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);

        createStudentProfile(studentToken);
        Long mentorProfileId = createMentorProfile(mentorToken);

        Long requestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, requestId);

        chatId = getChatByRequest(studentToken, requestId).id();
    }

    @Test
    @DisplayName("участник чата отправляет typing=true → 204")
    void typing_validChatParticipant_shouldReturn204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/chats/" + chatId + "/typing", HttpMethod.POST,
                bearerRequest(new TypingRequest(true), studentToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("участник чата отправляет typing=false → 204")
    void typing_typingFalse_shouldReturn204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/chats/" + chatId + "/typing", HttpMethod.POST,
                bearerRequest(new TypingRequest(false), mentorToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("несуществующий чат → 404")
    void typing_chatNotFound_shouldReturn404() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/chats/99999999/typing", HttpMethod.POST,
                bearerRequest(new TypingRequest(true), studentToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("посторонний пользователь → 403")
    void typing_nonParticipant_shouldReturn403() {
        String outsiderToken = registerAndLogin("outsider_" + uid() + "@test.com");

        ResponseEntity<Object> response = restTemplate.exchange(
                "/chats/" + chatId + "/typing", HttpMethod.POST,
                bearerRequest(new TypingRequest(true), outsiderToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("без токена → 401")
    void typing_unauthenticated_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/chats/" + chatId + "/typing", HttpMethod.POST,
                new HttpEntity<>(new TypingRequest(true), headers), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("тело запроса без поля typing → 400")
    void typing_missingBody_shouldReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(studentToken);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/chats/" + chatId + "/typing", HttpMethod.POST,
                new HttpEntity<>("{}", headers), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
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

    private Long createRequest(String token, Long mentorProfileId) {
        MentoringRequestCreateRequest body =
                new MentoringRequestCreateRequest(mentorProfileId, MentoringType.PRACTICE, "Хочу учиться");
        ResponseEntity<MentoringRequestResponse> resp = restTemplate.exchange(
                "/mentoring/requests", HttpMethod.POST, bearerRequest(body, token),
                MentoringRequestResponse.class);
        return resp.getBody().id();
    }

    private void acceptRequest(String token, Long reqId) {
        restTemplate.exchange("/mentoring/requests/" + reqId + "/accept", HttpMethod.PUT,
                bearerRequest(null, token), MentoringRequestResponse.class);
    }

    private ChatResponse getChatByRequest(String token, Long reqId) {
        ResponseEntity<ChatResponse> resp = restTemplate.exchange(
                "/chats/by-request/" + reqId, HttpMethod.GET,
                bearerRequest(null, token), ChatResponse.class);
        return resp.getBody();
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
