package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.websocket.ChatCommandErrorPayload;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ChatWebSocketExceptionHandler")
class ChatWebSocketExceptionHandlerTest {

    private final ChatWebSocketExceptionHandler handler = new ChatWebSocketExceptionHandler();

    @Test
    @DisplayName("validation error сохраняет requestId команды")
    void validationError_shouldKeepRequestId() {
        UUID requestId = UUID.randomUUID();
        SendMessageCommand command = new SendMessageCommand(requestId, "x", null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(command, "command");
        bindingResult.rejectValue("body", "Size", "Слишком длинное сообщение");
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ChatEventEnvelope event = handler.handleValidation(exception);

        assertThat(event.requestId()).isEqualTo(requestId);
        assertThat(event.type()).isEqualTo("chat.command.error");
        assertThat(event.payload()).isInstanceOfSatisfying(ChatCommandErrorPayload.class, payload -> {
            assertThat(payload.code()).isEqualTo("VALIDATION_ERROR");
            assertThat(payload.fieldErrors()).containsKey("body");
        });
    }
}
