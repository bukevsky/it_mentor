package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.service.ChatMessageStatusService;
import com.example.it.mentor.service.ChatService;
import com.example.it.mentor.service.SendMessageResult;
import com.example.it.mentor.service.TypingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRealtimeFacade")
class ChatRealtimeFacadeTest {

    private static final UUID REQUEST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @InjectMocks private ChatRealtimeFacade facade;

    @Mock private ChatService chatService;
    @Mock private ChatMessageStatusService statusService;
    @Mock private TypingService typingService;
    @Mock private ChatRealtimePublisher publisher;

    @Test
    @DisplayName("новое сообщение публикуется обоим участникам и подтверждается отправителю")
    void send_newMessage_shouldPublishEventAndAck() {
        ChatMessageResponse response = messageResponse();
        when(chatService.sendMessage(eq(10L), eq(REQUEST_ID), any()))
                .thenReturn(new SendMessageResult(response, 1L, 2L, false));

        facade.send("student@test.com", 10L,
                new SendMessageCommand(REQUEST_ID, "Привет", null));

        ArgumentCaptor<ChatEventEnvelope> eventCaptor = ArgumentCaptor.forClass(ChatEventEnvelope.class);
        verify(publisher).publishToUsers(eq(Set.of(1L, 2L)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo("chat.message.created");
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(REQUEST_ID);
        assertThat(eventCaptor.getValue().payload()).isSameAs(response);

        verify(publisher).publishToPrincipal(eq("student@test.com"),
                argThatEvent("chat.command.ack", REQUEST_ID));
    }

    @Test
    @DisplayName("duplicate requestId не публикует message.created повторно")
    void send_duplicateMessage_shouldOnlyAck() {
        when(chatService.sendMessage(eq(10L), eq(REQUEST_ID), any()))
                .thenReturn(new SendMessageResult(messageResponse(), 1L, 2L, true));

        facade.send("student@test.com", 10L,
                new SendMessageCommand(REQUEST_ID, "Привет", null));

        verify(publisher, never()).publishToUsers(any(), any());
        verify(publisher).publishToPrincipal(eq("student@test.com"),
                argThatEvent("chat.command.ack", REQUEST_ID));
    }

    @Test
    @DisplayName("бизнес-ошибка преобразуется в correlated command.error")
    void send_forbidden_shouldPublishError() {
        when(chatService.sendMessage(eq(10L), eq(REQUEST_ID), any()))
                .thenThrow(new ForbiddenException("Доступ запрещён"));

        facade.send("student@test.com", 10L,
                new SendMessageCommand(REQUEST_ID, "Привет", null));

        ArgumentCaptor<ChatEventEnvelope> captor = ArgumentCaptor.forClass(ChatEventEnvelope.class);
        verify(publisher).publishToPrincipal(eq("student@test.com"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("chat.command.error");
        assertThat(captor.getValue().requestId()).isEqualTo(REQUEST_ID);
    }

    private ChatEventEnvelope argThatEvent(String type, UUID requestId) {
        return org.mockito.ArgumentMatchers.argThat(event ->
                type.equals(event.type()) && requestId.equals(event.requestId()));
    }

    private ChatMessageResponse messageResponse() {
        return new ChatMessageResponse(
                99L, 10L, 1L, REQUEST_ID, "Привет", null,
                ChatMessageDeliveryStatus.SENT, null, null, null);
    }
}
