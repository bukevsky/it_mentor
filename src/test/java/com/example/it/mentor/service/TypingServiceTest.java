package com.example.it.mentor.service;

import com.example.it.mentor.dto.sse.TypingPayload;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TypingService")
class TypingServiceTest {

    @InjectMocks private TypingService service;

    @Mock private ChatRepository chatRepository;
    @Mock private ChatSseService sseService;
    @Mock private UserService userService;

    private static final Long CHAT_ID = 1L;
    private static final Long STUDENT_ID = 10L;
    private static final Long MENTOR_ID = 20L;

    private User student;
    private User mentor;
    private Chat chat;

    @BeforeEach
    void setUp() {
        student = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);

        mentor = User.builder().email("mentor@test.com").build();
        ReflectionTestUtils.setField(mentor, "id", MENTOR_ID);

        chat = Chat.builder()
                .studentUserId(STUDENT_ID)
                .mentorUserId(MENTOR_ID)
                .build();
        ReflectionTestUtils.setField(chat, "id", CHAT_ID);
    }

    @Test
    @DisplayName("handleTyping от студента — целевой userId ментора, payload корректный")
    void handleTyping_studentTyping_shouldPushToMentor() {
        when(userService.getCurrentUserEntity()).thenReturn(student);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        service.handleTyping(CHAT_ID, true);

        ArgumentCaptor<Long> targetCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TypingPayload> payloadCaptor = ArgumentCaptor.forClass(TypingPayload.class);
        verify(sseService).pushTyping(targetCaptor.capture(), payloadCaptor.capture());

        assertThat(targetCaptor.getValue()).isEqualTo(MENTOR_ID);
        assertThat(payloadCaptor.getValue().userId()).isEqualTo(STUDENT_ID);
        assertThat(payloadCaptor.getValue().chatId()).isEqualTo(CHAT_ID);
        assertThat(payloadCaptor.getValue().typing()).isTrue();
    }

    @Test
    @DisplayName("handleTyping от ментора — целевой userId студента")
    void handleTyping_mentorTyping_shouldPushToStudent() {
        when(userService.getCurrentUserEntity()).thenReturn(mentor);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        service.handleTyping(CHAT_ID, true);

        ArgumentCaptor<Long> targetCaptor = ArgumentCaptor.forClass(Long.class);
        verify(sseService).pushTyping(targetCaptor.capture(), any());

        assertThat(targetCaptor.getValue()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("handleTyping с typing=false передаёт false в payload")
    void handleTyping_typingFalse_shouldPushTypingFalse() {
        when(userService.getCurrentUserEntity()).thenReturn(student);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        service.handleTyping(CHAT_ID, false);

        ArgumentCaptor<TypingPayload> captor = ArgumentCaptor.forClass(TypingPayload.class);
        verify(sseService).pushTyping(any(), captor.capture());

        assertThat(captor.getValue().typing()).isFalse();
    }

    @Test
    @DisplayName("handleTyping выбрасывает NotFoundException если чат не найден")
    void handleTyping_chatNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUserEntity()).thenReturn(student);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleTyping(CHAT_ID, true))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(CHAT_ID.toString());
    }

    @Test
    @DisplayName("handleTyping выбрасывает ForbiddenException если пользователь не участник")
    void handleTyping_nonParticipant_shouldThrowForbiddenException() {
        User outsider = User.builder().email("outsider@test.com").build();
        ReflectionTestUtils.setField(outsider, "id", 99L);
        when(userService.getCurrentUserEntity()).thenReturn(outsider);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> service.handleTyping(CHAT_ID, true))
                .isInstanceOf(ForbiddenException.class);
    }
}
