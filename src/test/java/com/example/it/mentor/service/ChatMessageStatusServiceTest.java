package com.example.it.mentor.service;

import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageStatusService")
class ChatMessageStatusServiceTest {

    @InjectMocks private ChatMessageStatusService service;

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private UserService userService;

    private Chat chat;
    private User student;

    @BeforeEach
    void setUp() {
        student = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(student, "id", 1L);

        chat = Chat.builder().studentUserId(1L).mentorUserId(2L).build();
        ReflectionTestUtils.setField(chat, "id", 10L);
    }

    @Nested
    @DisplayName("markDelivered")
    class MarkDelivered {

        @Test
        @DisplayName("получатель подтверждает входящие сообщения до указанного id")
        void incomingMessage_shouldPromoteToDelivered() {
            ChatMessage boundary = message(50L, 2L);
            when(userService.getCurrentUserEntity()).thenReturn(student);
            when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
            when(messageRepository.findById(50L)).thenReturn(Optional.of(boundary));
            when(messageRepository.markDelivered(any(), any(), any(), any())).thenReturn(3);

            MessageStatusChangeResult result = service.markDelivered(10L, 50L);

            assertThat(result.chatId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo(ChatMessageDeliveryStatus.DELIVERED);
            assertThat(result.changedCount()).isEqualTo(3);
            assertThat(result.actorUserId()).isEqualTo(1L);
            assertThat(result.targetUserId()).isEqualTo(2L);
            verify(messageRepository).markDelivered(any(), any(), any(), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("нельзя подтверждать собственное сообщение")
        void ownBoundaryMessage_shouldThrowForbidden() {
            ChatMessage boundary = message(50L, 1L);
            when(userService.getCurrentUserEntity()).thenReturn(student);
            when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
            when(messageRepository.findById(50L)).thenReturn(Optional.of(boundary));

            assertThatThrownBy(() -> service.markDelivered(10L, 50L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("собственное сообщение");

            verify(messageRepository, never()).markDelivered(any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("read повышает входящие сообщения до READ")
    void markRead_incomingMessage_shouldPromoteToRead() {
        ChatMessage boundary = message(75L, 2L);
        when(userService.getCurrentUserEntity()).thenReturn(student);
        when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(messageRepository.findById(75L)).thenReturn(Optional.of(boundary));
        when(messageRepository.markRead(any(), any(), any(), any())).thenReturn(4);

        MessageStatusChangeResult result = service.markRead(10L, 75L);

        assertThat(result.status()).isEqualTo(ChatMessageDeliveryStatus.READ);
        assertThat(result.changedCount()).isEqualTo(4);
        verify(messageRepository).markRead(any(), any(), any(), any(OffsetDateTime.class));
    }

    private ChatMessage message(Long id, Long senderId) {
        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .senderUserId(senderId)
                .body("message")
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
