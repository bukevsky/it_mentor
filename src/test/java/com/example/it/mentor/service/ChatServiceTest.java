package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.ChatMapper;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.StoredFileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

    @InjectMocks private ChatService service;

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private UserService userService;
    @Mock private FileStorage fileStorage;
    @Mock private StoredFileRepository storedFileRepository;
    @Mock private ChatMapper mapper;

    private User studentUser;
    private User mentorUser;
    private StudentProfile studentProfile;
    private MentorProfile mentorProfile;
    private MentoringRequest mentoringRequest;
    private Chat chat;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(studentUser, "id", 1L);

        mentorUser = User.builder().email("mentor@test.com").build();
        ReflectionTestUtils.setField(mentorUser, "id", 2L);

        studentProfile = StudentProfile.builder().user(studentUser).build();
        ReflectionTestUtils.setField(studentProfile, "id", 10L);

        mentorProfile = MentorProfile.builder().user(mentorUser).build();
        ReflectionTestUtils.setField(mentorProfile, "id", 20L);

        mentoringRequest = MentoringRequest.builder()
                .studentProfile(studentProfile)
                .mentorProfile(mentorProfile)
                .build();
        ReflectionTestUtils.setField(mentoringRequest, "id", 100L);

        chat = Chat.builder()
                .mentoringRequest(mentoringRequest)
                .studentUserId(1L)
                .mentorUserId(2L)
                .build();
        ReflectionTestUtils.setField(chat, "id", 200L);
    }

    // ── createForRequest ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createForRequest")
    class CreateForRequest {

        @Test
        @DisplayName("happyPath — корректно сохраняет чат с правильными userId")
        void createForRequest_happyPath_shouldSaveChat() {
            when(chatRepository.findByMentoringRequestId(100L)).thenReturn(Optional.empty());

            service.createForRequest(mentoringRequest);

            ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
            verify(chatRepository).save(captor.capture());
            Chat saved = captor.getValue();
            assertThat(saved.getStudentUserId()).isEqualTo(1L);
            assertThat(saved.getMentorUserId()).isEqualTo(2L);
            assertThat(saved.getMentoringRequest()).isSameAs(mentoringRequest);
        }

        @Test
        @DisplayName("duplicate — бросает ConflictException если чат уже есть")
        void createForRequest_duplicate_shouldThrowConflict() {
            when(chatRepository.findByMentoringRequestId(100L)).thenReturn(Optional.of(chat));

            assertThatThrownBy(() -> service.createForRequest(mentoringRequest))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("participant — студент получает свой чат")
        void getById_participant_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            ChatResponse expected = new ChatResponse(200L, 100L, 1L, 2L, null);
            when(mapper.toResponse(chat)).thenReturn(expected);

            ChatResponse result = service.getById(200L);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("nonParticipant — посторонний пользователь → ForbiddenException")
        void getById_nonParticipant_shouldThrowForbidden() {
            User outsider = User.builder().email("other@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));

            assertThatThrownBy(() -> service.getById(200L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("notFound — несуществующий chatId → NotFoundException")
        void getById_notFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getByRequestId ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByRequestId")
    class GetByRequestId {

        @Test
        @DisplayName("happyPath — возвращает чат по ID заявки")
        void getByRequestId_happyPath_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findByMentoringRequestId(100L)).thenReturn(Optional.of(chat));
            ChatResponse expected = new ChatResponse(200L, 100L, 1L, 2L, null);
            when(mapper.toResponse(chat)).thenReturn(expected);

            ChatResponse result = service.getByRequestId(100L);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("noChat — нет чата для заявки → NotFoundException")
        void getByRequestId_noChat_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findByMentoringRequestId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByRequestId(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getMyChats ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyChats")
    class GetMyChats {

        @Test
        @DisplayName("возвращает пагинированный список чатов текущего пользователя")
        void getMyChats_shouldReturnPaged() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            Page<Chat> page = new PageImpl<>(List.of(chat));
            when(chatRepository.findAllByUserId(eq(1L), any())).thenReturn(page);
            ChatResponse resp = new ChatResponse(200L, 100L, 1L, 2L, null);
            when(mapper.toResponse(chat)).thenReturn(resp);

            PagedResponse<ChatResponse> result = service.getMyChats(PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    // ── getMessages ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("participant — корректная пагинация")
        void getMessages_participant_shouldReturnPaged() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            ChatMessage msg = ChatMessage.builder().chat(chat).senderUserId(1L).body("Привет").build();
            Page<ChatMessage> page = new PageImpl<>(List.of(msg));
            when(messageRepository.findByChatIdAndDeletedFalseOrderByCreatedAtDesc(eq(200L), any())).thenReturn(page);
            ChatMessageResponse resp = new ChatMessageResponse(1L, 200L, 1L, "Привет", null, null);
            when(mapper.toMessageResponse(msg)).thenReturn(resp);

            PagedResponse<ChatMessageResponse> result = service.getMessages(200L, PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("nonParticipant — посторонний → ForbiddenException")
        void getMessages_nonParticipant_shouldThrowForbidden() {
            User outsider = User.builder().email("other@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));

            assertThatThrownBy(() -> service.getMessages(200L, PageRequest.of(0, 20)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("textOnly — только body — сообщение сохраняется")
        void sendMessage_textOnly_shouldSave() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            SendMessageRequest dto = new SendMessageRequest("Привет", null);
            ChatMessage saved = ChatMessage.builder().chat(chat).senderUserId(1L).body("Привет").build();
            when(messageRepository.save(any())).thenReturn(saved);
            ChatMessageResponse resp = new ChatMessageResponse(1L, 200L, 1L, "Привет", null, null);
            when(mapper.toMessageResponse(saved)).thenReturn(resp);

            ChatMessageResponse result = service.sendMessage(200L, dto);

            assertThat(result.body()).isEqualTo("Привет");
            verify(fileStorage, never()).requireOwned(any(), any());
        }

        @Test
        @DisplayName("attachmentOnly — только файл — requireOwned вызван")
        void sendMessage_attachmentOnly_shouldSave() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            StoredFile file = StoredFile.builder().originalFilename("doc.pdf").build();
            ReflectionTestUtils.setField(file, "id", 5L);
            when(storedFileRepository.findById(5L)).thenReturn(Optional.of(file));
            SendMessageRequest dto = new SendMessageRequest(null, 5L);
            ChatMessage saved = ChatMessage.builder().chat(chat).senderUserId(1L).attachment(file).build();
            when(messageRepository.save(any())).thenReturn(saved);
            ChatMessageResponse resp = new ChatMessageResponse(1L, 200L, 1L, null, null, null);
            when(mapper.toMessageResponse(saved)).thenReturn(resp);

            service.sendMessage(200L, dto);

            verify(fileStorage).requireOwned(5L, 1L);
        }

        @Test
        @DisplayName("withBothBodyAndAttachment — оба поля — сообщение сохраняется")
        void sendMessage_withBothBodyAndAttachment_shouldSave() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            StoredFile file = StoredFile.builder().originalFilename("doc.pdf").build();
            ReflectionTestUtils.setField(file, "id", 5L);
            when(storedFileRepository.findById(5L)).thenReturn(Optional.of(file));
            SendMessageRequest dto = new SendMessageRequest("Смотри файл", 5L);
            ChatMessage saved = ChatMessage.builder().chat(chat).senderUserId(1L).body("Смотри файл").attachment(file).build();
            when(messageRepository.save(any())).thenReturn(saved);
            ChatMessageResponse resp = new ChatMessageResponse(1L, 200L, 1L, "Смотри файл", null, null);
            when(mapper.toMessageResponse(saved)).thenReturn(resp);

            service.sendMessage(200L, dto);

            verify(fileStorage).requireOwned(5L, 1L);
            verify(messageRepository).save(any());
        }

        @Test
        @DisplayName("emptyBodyAndNoFile — пустое сообщение → BusinessRuleViolationException")
        void sendMessage_emptyBodyAndNoFile_shouldThrowBusiness() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            SendMessageRequest dto = new SendMessageRequest(null, null);

            assertThatThrownBy(() -> service.sendMessage(200L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("blankBody — пробельная строка → BusinessRuleViolationException")
        void sendMessage_blankBody_shouldThrowBusiness() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            SendMessageRequest dto = new SendMessageRequest("   ", null);

            assertThatThrownBy(() -> service.sendMessage(200L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("fileNotOwned — requireOwned бросает ForbiddenException")
        void sendMessage_fileNotOwned_shouldThrowForbidden() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            doThrow(new ForbiddenException("Файл принадлежит другому пользователю"))
                    .when(fileStorage).requireOwned(5L, 1L);
            SendMessageRequest dto = new SendMessageRequest(null, 5L);

            assertThatThrownBy(() -> service.sendMessage(200L, dto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("attachmentFileNotFound — findById пустой → NotFoundException")
        void sendMessage_attachmentFileNotFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            when(storedFileRepository.findById(5L)).thenReturn(Optional.empty());
            SendMessageRequest dto = new SendMessageRequest(null, 5L);

            assertThatThrownBy(() -> service.sendMessage(200L, dto))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("mentorCanSend — ментор отправляет сообщение")
        void sendMessage_mentorCanSend_shouldSave() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            SendMessageRequest dto = new SendMessageRequest("Хорошая работа!", null);
            ChatMessage saved = ChatMessage.builder().chat(chat).senderUserId(2L).body("Хорошая работа!").build();
            when(messageRepository.save(any())).thenReturn(saved);
            ChatMessageResponse resp = new ChatMessageResponse(1L, 200L, 2L, "Хорошая работа!", null, null);
            when(mapper.toMessageResponse(saved)).thenReturn(resp);

            ChatMessageResponse result = service.sendMessage(200L, dto);

            assertThat(result.senderUserId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("nonParticipant — посторонний → ForbiddenException")
        void sendMessage_nonParticipant_shouldThrowForbidden() {
            User outsider = User.builder().email("other@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(chat));
            SendMessageRequest dto = new SendMessageRequest("Привет", null);

            assertThatThrownBy(() -> service.sendMessage(200L, dto))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
