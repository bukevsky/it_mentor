package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.dto.chat.websocket.MessageStatusCommand;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("STOMP/WebSocket chat IT")
class ChatWebSocketIT {

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void minioProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.endpoint", minioContainer::getS3URL);
        registry.add("app.storage.access-key", minioContainer::getUserName);
        registry.add("app.storage.secret-key", minioContainer::getPassword);
        registry.add("app.storage.bucket-name", () -> "itmentor-websocket-test");
    }

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private final List<StompSession> sessions = new ArrayList<>();
    private WebSocketStompClient stompClient;
    private String studentToken;
    private String mentorToken;
    private Long chatId;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        stompClient.start();

        String studentEmail = "ws_student_" + uid() + "@test.com";
        String mentorEmail = "ws_mentor_" + uid() + "@test.com";
        studentToken = registerAndLogin(studentEmail);
        mentorToken = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);
        createStudentProfile(studentToken);
        Long mentorProfileId = createMentorProfile(mentorToken);
        Long requestId = createRequest(studentToken, mentorProfileId);
        acceptRequest(mentorToken, requestId);
        chatId = getChatByRequest(studentToken, requestId).id();
    }

    @AfterEach
    void tearDown() {
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("send → delivered → read доставляется двум клиентам и сохраняет статусы")
    void sendDeliveredRead_shouldPublishAndPersistStatuses() throws Exception {
        BlockingQueue<ChatEventEnvelope> studentEvents = new LinkedBlockingQueue<>();
        BlockingQueue<ChatEventEnvelope> mentorEvents = new LinkedBlockingQueue<>();
        StompSession student = connect(studentToken, studentEvents);
        StompSession mentor = connect(mentorToken, mentorEvents);
        UUID requestId = UUID.randomUUID();

        student.send(destination("messages/send"),
                new SendMessageCommand(requestId, "Привет по WebSocket", null));

        ChatEventEnvelope studentCreated = await(studentEvents, "chat.message.created", requestId);
        ChatEventEnvelope mentorCreated = await(mentorEvents, "chat.message.created", requestId);
        ChatEventEnvelope ack = await(studentEvents, "chat.command.ack", requestId);
        Long messageId = number(payload(studentCreated).get("id"));
        assertThat(payload(mentorCreated).get("body")).isEqualTo("Привет по WebSocket");
        assertThat(payload(ack).get("duplicate")).isEqualTo(false);
        assertThat(history(mentorToken)).singleElement()
                .extracting(ChatMessageResponse::deliveryStatus)
                .isEqualTo(ChatMessageDeliveryStatus.SENT);

        UUID deliveredId = UUID.randomUUID();
        mentor.send(destination("messages/delivered"), new MessageStatusCommand(deliveredId, messageId));
        await(studentEvents, "chat.message.status.changed", deliveredId);
        await(mentorEvents, "chat.message.status.changed", deliveredId);
        await(mentorEvents, "chat.command.ack", deliveredId);
        assertThat(history(mentorToken).getFirst().deliveryStatus())
                .isEqualTo(ChatMessageDeliveryStatus.DELIVERED);

        UUID readId = UUID.randomUUID();
        mentor.send(destination("messages/read"), new MessageStatusCommand(readId, messageId));
        await(studentEvents, "chat.message.status.changed", readId);
        await(mentorEvents, "chat.message.status.changed", readId);
        await(mentorEvents, "chat.command.ack", readId);
        ChatMessageResponse read = history(mentorToken).getFirst();
        assertThat(read.deliveryStatus()).isEqualTo(ChatMessageDeliveryStatus.READ);
        assertThat(read.deliveredAt()).isNotNull();
        assertThat(read.readAt()).isNotNull();
        assertThat(getChat(mentorToken).unreadCount()).isZero();
    }

    @Test
    @DisplayName("повтор requestId не создаёт дубликат сообщения")
    void duplicateRequestId_shouldCreateOneMessage() throws Exception {
        BlockingQueue<ChatEventEnvelope> events = new LinkedBlockingQueue<>();
        StompSession student = connect(studentToken, events);
        UUID requestId = UUID.randomUUID();
        SendMessageCommand command = new SendMessageCommand(requestId, "Один раз", null);

        student.send(destination("messages/send"), command);
        await(events, "chat.message.created", requestId);
        await(events, "chat.command.ack", requestId);
        student.send(destination("messages/send"), command);
        ChatEventEnvelope duplicateAck = await(events, "chat.command.ack", requestId);

        assertThat(payload(duplicateAck).get("duplicate")).isEqualTo(true);
        assertThat(history(studentToken)).hasSize(1);
    }

    @Test
    @DisplayName("посторонний пользователь получает correlated command.error")
    void outsiderSend_shouldReceiveForbiddenError() throws Exception {
        String outsiderToken = registerAndLogin("ws_outsider_" + uid() + "@test.com");
        BlockingQueue<ChatEventEnvelope> events = new LinkedBlockingQueue<>();
        StompSession outsider = connect(outsiderToken, events);
        UUID requestId = UUID.randomUUID();

        outsider.send(destination("messages/send"),
                new SendMessageCommand(requestId, "Нельзя", null));

        ChatEventEnvelope error = await(events, "chat.command.error", requestId);
        assertThat(payload(error).get("code")).isEqualTo("FORBIDDEN");
        assertThat(history(studentToken)).isEmpty();
    }

    @Test
    @DisplayName("attachment отправляется через WebSocket после REST upload")
    void attachmentMessage_shouldPublishAttachment() throws Exception {
        Long fileId = uploadAttachment(studentToken);
        BlockingQueue<ChatEventEnvelope> events = new LinkedBlockingQueue<>();
        StompSession student = connect(studentToken, events);
        UUID requestId = UUID.randomUUID();

        student.send(destination("messages/send"),
                new SendMessageCommand(requestId, null, fileId));

        ChatEventEnvelope created = await(events, "chat.message.created", requestId);
        Map<?, ?> attachment = (Map<?, ?>) payload(created).get("attachment");
        assertThat(number(attachment.get("fileId"))).isEqualTo(fileId);
    }

    @Test
    @DisplayName("после reconnect sync возвращает пропущенные сообщения по id ASC")
    void reconnectSync_shouldReturnMessagesAfterCursor() throws Exception {
        BlockingQueue<ChatEventEnvelope> events = new LinkedBlockingQueue<>();
        StompSession student = connect(studentToken, events);
        UUID firstRequest = UUID.randomUUID();
        student.send(destination("messages/send"), new SendMessageCommand(firstRequest, "Первое", null));
        Long firstId = number(payload(await(events, "chat.message.created", firstRequest)).get("id"));
        await(events, "chat.command.ack", firstRequest);
        student.disconnect();

        BlockingQueue<ChatEventEnvelope> reconnectedEvents = new LinkedBlockingQueue<>();
        StompSession reconnected = connect(studentToken, reconnectedEvents);
        UUID secondRequest = UUID.randomUUID();
        reconnected.send(destination("messages/send"), new SendMessageCommand(secondRequest, "Второе", null));
        await(reconnectedEvents, "chat.message.created", secondRequest);
        await(reconnectedEvents, "chat.command.ack", secondRequest);

        ResponseEntity<List<ChatMessageResponse>> response = restTemplate.exchange(
                "/chats/" + chatId + "/messages/sync?afterMessageId=" + firstId + "&limit=100",
                HttpMethod.GET,
                bearerRequest(null, studentToken),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getBody())
                .extracting(ChatMessageResponse::body)
                .containsExactly("Второе");
    }

    @Test
    @DisplayName("presence остаётся online до закрытия последней из двух сессий")
    void twoSessions_shouldGoOfflineOnlyAfterLastDisconnect() throws Exception {
        BlockingQueue<ChatEventEnvelope> mentorEvents = new LinkedBlockingQueue<>();
        connect(mentorToken, mentorEvents);
        StompSession first = connect(studentToken, new LinkedBlockingQueue<>());
        ChatEventEnvelope online = await(mentorEvents, "presence.changed", null);
        assertThat(payload(online).get("status")).isEqualTo("online");

        StompSession second = connect(studentToken, new LinkedBlockingQueue<>());
        first.disconnect();
        assertThat(pollType(mentorEvents, "presence.changed", Duration.ofMillis(400))).isNull();

        second.disconnect();
        ChatEventEnvelope offline = await(mentorEvents, "presence.changed", null);
        assertThat(payload(offline).get("status")).isEqualTo("offline");
    }

    @Test
    @DisplayName("невалидный JWT отклоняет STOMP CONNECT")
    void invalidJwt_shouldRejectConnect() {
        assertThatThrownBy(() -> connect("invalid-token", new LinkedBlockingQueue<>()))
                .isInstanceOf(Exception.class);
    }

    private StompSession connect(String token,
                                 BlockingQueue<ChatEventEnvelope> events) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        StompSession session = stompClient.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        session.subscribe("/user/queue/chat-events", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatEventEnvelope.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                events.add((ChatEventEnvelope) payload);
            }
        });
        sessions.add(session);
        return session;
    }

    private ChatEventEnvelope await(BlockingQueue<ChatEventEnvelope> events,
                                    String type,
                                    UUID requestId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            ChatEventEnvelope event = events.poll(250, TimeUnit.MILLISECONDS);
            if (event != null && type.equals(event.type()) && Objects.equals(requestId, event.requestId())) {
                return event;
            }
        }
        throw new AssertionError("Не получено событие " + type + " requestId=" + requestId);
    }

    private ChatEventEnvelope pollType(BlockingQueue<ChatEventEnvelope> events,
                                       String type,
                                       Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ChatEventEnvelope event = events.poll(100, TimeUnit.MILLISECONDS);
            if (event != null && type.equals(event.type())) {
                return event;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(ChatEventEnvelope event) {
        return (Map<String, Object>) event.payload();
    }

    private Long number(Object value) {
        return ((Number) value).longValue();
    }

    private String destination(String suffix) {
        return "/app/chats/" + chatId + "/" + suffix;
    }

    private List<ChatMessageResponse> history(String token) {
        ResponseEntity<PagedResponse<ChatMessageResponse>> response = restTemplate.exchange(
                "/chats/" + chatId + "/messages",
                HttpMethod.GET,
                bearerRequest(null, token),
                new ParameterizedTypeReference<>() {});
        return response.getBody().content();
    }

    private ChatResponse getChat(String token) {
        return restTemplate.exchange(
                "/chats/" + chatId,
                HttpMethod.GET,
                bearerRequest(null, token),
                ChatResponse.class).getBody();
    }

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Иван", "Иванов"), Object.class);
        return restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class)
                .getBody().accessToken();
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
        StudentProfileRequest request = new StudentProfileRequest(
                "Студент", "Иванов", null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/student", HttpMethod.PUT,
                bearerRequest(request, token), StudentProfileResponse.class);
    }

    private Long createMentorProfile(String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                "Ментор", "Петров", null, "Java Dev", null, null, null, null,
                null, null, null, null, null, null, null, null,
                RecruitmentStatus.OPEN, null);
        return restTemplate.exchange("/profile/mentor", HttpMethod.PUT,
                bearerRequest(request, token), MentorProfileResponse.class)
                .getBody().id();
    }

    private Long createRequest(String token, Long mentorProfileId) {
        MentoringRequestCreateRequest request = new MentoringRequestCreateRequest(
                mentorProfileId, MentoringType.PRACTICE, "Хочу учиться");
        return restTemplate.exchange("/mentoring/requests", HttpMethod.POST,
                bearerRequest(request, token), MentoringRequestResponse.class)
                .getBody().id();
    }

    private void acceptRequest(String token, Long requestId) {
        restTemplate.exchange("/mentoring/requests/" + requestId + "/accept", HttpMethod.PUT,
                bearerRequest(null, token), MentoringRequestResponse.class);
    }

    private ChatResponse getChatByRequest(String token, Long requestId) {
        return restTemplate.exchange("/chats/by-request/" + requestId, HttpMethod.GET,
                bearerRequest(null, token), ChatResponse.class).getBody();
    }

    private Long uploadAttachment(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.TEXT_PLAIN);
        partHeaders.setContentDispositionFormData("file", "websocket.txt");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new ByteArrayResource("attachment".getBytes()), partHeaders));
        return restTemplate.postForEntity(
                "/files/chat-attachment", new HttpEntity<>(body, headers), FileUploadResponse.class)
                .getBody().id();
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
