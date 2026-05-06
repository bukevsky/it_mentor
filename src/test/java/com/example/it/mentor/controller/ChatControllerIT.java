package com.example.it.mentor.controller;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("ChatController IT")
class ChatControllerIT {

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void minioProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.endpoint", minioContainer::getS3URL);
        registry.add("app.storage.access-key", minioContainer::getUserName);
        registry.add("app.storage.secret-key", minioContainer::getPassword);
        registry.add("app.storage.bucket-name", () -> "itmentor-test");
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<ChatResponse>> PAGED_CHAT =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PagedResponse<ChatMessageResponse>> PAGED_MSG =
            new ParameterizedTypeReference<>() {};

    private String studentToken;
    private String mentorToken;
    private Long requestId;
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

        requestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, requestId);

        chatId = getChatByRequest(studentToken, requestId).id();
    }

    // ── acceptRequest auto-creates chat ──────────────────────────────────────

    @Test
    @DisplayName("acceptRequest — автоматически создаёт чат")
    void acceptRequest_shouldAutoCreateChat() {
        ResponseEntity<ChatResponse> response = restTemplate.exchange(
                "/chats/by-request/" + requestId, HttpMethod.GET,
                bearerRequest(null, studentToken), ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().mentoringRequestId()).isEqualTo(requestId);
    }

    // ── GET /chats ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /chats")
    class GetMyChats {

        @Test
        @DisplayName("возвращает список чатов текущего пользователя")
        void getMyChats_shouldReturnList() {
            ResponseEntity<PagedResponse<ChatResponse>> response = restTemplate.exchange(
                    "/chats", HttpMethod.GET, bearerRequest(null, studentToken), PAGED_CHAT);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isNotEmpty();
        }

        @Test
        @DisplayName("без токена → 401")
        void getMyChats_unauthenticated_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/chats", Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── GET /chats/{chatId} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /chats/{chatId}")
    class GetChat {

        @Test
        @DisplayName("участник получает чат → 200")
        void getChat_participant_shouldReturn200() {
            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    "/chats/" + chatId, HttpMethod.GET,
                    bearerRequest(null, studentToken), ChatResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(chatId);
        }

        @Test
        @DisplayName("посторонний → 403")
        void getChat_nonParticipant_shouldReturn403() {
            String otherToken = registerAndLogin("other_" + uid() + "@test.com");

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/chats/" + chatId, HttpMethod.GET,
                    bearerRequest(null, otherToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("не существует → 404")
        void getChat_notFound_shouldReturn404() {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/chats/99999", HttpMethod.GET,
                    bearerRequest(null, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── GET /chats/by-request/{requestId} ────────────────────────────────────

    @Test
    @DisplayName("GET /chats/by-request/{requestId} → 200")
    void getChatByRequest_happyPath_shouldReturn200() {
        ResponseEntity<ChatResponse> response = restTemplate.exchange(
                "/chats/by-request/" + requestId, HttpMethod.GET,
                bearerRequest(null, mentorToken), ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().mentoringRequestId()).isEqualTo(requestId);
    }

    // ── GET /chats/{chatId}/messages ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /chats/{chatId}/messages")
    class GetMessages {

        @Test
        @DisplayName("нет сообщений — пустая страница")
        void getMessages_empty_shouldReturnEmptyPage() {
            ResponseEntity<PagedResponse<ChatMessageResponse>> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_MSG);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isEmpty();
            assertThat(response.getBody().totalElements()).isZero();
        }

        @Test
        @DisplayName("без токена → 401")
        void getMessages_unauthenticated_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                    "/chats/" + chatId + "/messages", Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── POST /chats/{chatId}/messages ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /chats/{chatId}/messages")
    class SendMessage {

        @Test
        @DisplayName("текстовое сообщение → 201")
        void sendMessage_textOnly_shouldReturn201() {
            SendMessageRequest dto = new SendMessageRequest("Привет!", null);

            ResponseEntity<ChatMessageResponse> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, studentToken), ChatMessageResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().body()).isEqualTo("Привет!");
            assertThat(response.getBody().senderUserId()).isNotNull();
        }

        @Test
        @DisplayName("пустое сообщение → 422")
        void sendMessage_emptyBody_shouldReturn422() {
            SendMessageRequest dto = new SendMessageRequest(null, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("посторонний → 403")
        void sendMessage_nonParticipant_shouldReturn403() {
            String otherToken = registerAndLogin("other_" + uid() + "@test.com");
            SendMessageRequest dto = new SendMessageRequest("Hi", null);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, otherToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("отправить → получить историю — сообщение присутствует")
        void sendMessage_thenGetMessages_shouldShowMessage() {
            SendMessageRequest dto = new SendMessageRequest("Тестовое сообщение", null);
            restTemplate.exchange("/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, studentToken), ChatMessageResponse.class);

            ResponseEntity<PagedResponse<ChatMessageResponse>> history = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_MSG);

            assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(history.getBody().content()).hasSize(1);
            assertThat(history.getBody().content().get(0).body()).isEqualTo("Тестовое сообщение");
        }

        @Test
        @DisplayName("3 сообщения, size=2 → totalElements=3")
        void sendMessage_pagination_shouldReturnCorrectPage() {
            for (int i = 1; i <= 3; i++) {
                restTemplate.exchange("/chats/" + chatId + "/messages", HttpMethod.POST,
                        bearerRequest(new SendMessageRequest("Msg " + i, null), studentToken),
                        ChatMessageResponse.class);
            }

            ResponseEntity<PagedResponse<ChatMessageResponse>> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages?page=0&size=2", HttpMethod.GET,
                    bearerRequest(null, studentToken), PAGED_MSG);

            assertThat(response.getBody().totalElements()).isEqualTo(3);
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("с вложением → 201")
        void sendMessage_withAttachment_shouldReturn201() {
            Long fileId = uploadAttachment(studentToken);
            SendMessageRequest dto = new SendMessageRequest(null, fileId);

            ResponseEntity<ChatMessageResponse> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, studentToken), ChatMessageResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().attachment()).isNotNull();
            assertThat(response.getBody().attachment().fileId()).isEqualTo(fileId);
        }

        @Test
        @DisplayName("чужой файл → 403")
        void sendMessage_attachmentOwnedByOther_shouldReturn403() {
            Long fileId = uploadAttachment(mentorToken);
            SendMessageRequest dto = new SendMessageRequest(null, fileId);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/chats/" + chatId + "/messages", HttpMethod.POST,
                    bearerRequest(dto, studentToken), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── POST /files/chat-attachment ───────────────────────────────────────────

    @Test
    @DisplayName("POST /files/chat-attachment → 201")
    void uploadChatAttachment_shouldReturn201() {
        ResponseEntity<FileUploadResponse> response = uploadFileRaw(
                "/files/chat-attachment", "test.txt", "text/plain",
                "hello".getBytes(), studentToken, FileUploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isNotNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Иван", "Иванов"), Object.class);
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

    private ChatResponse getChatByRequest(String token, Long reqId) {
        ResponseEntity<ChatResponse> resp = restTemplate.exchange(
                "/chats/by-request/" + reqId, HttpMethod.GET,
                bearerRequest(null, token), ChatResponse.class);
        return resp.getBody();
    }

    private Long uploadAttachment(String token) {
        ResponseEntity<FileUploadResponse> resp = uploadFileRaw(
                "/files/chat-attachment", "test.txt", "text/plain",
                "hello".getBytes(), token, FileUploadResponse.class);
        return resp.getBody().id();
    }

    private <T> ResponseEntity<T> uploadFileRaw(
            String url, String filename, String contentType, byte[] content,
            String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        partHeaders.setContentDispositionFormData("file", filename);
        body.add("file", new HttpEntity<>(new ByteArrayResource(content), partHeaders));

        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), responseType);
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
