package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.websocket.ChatCommandErrorPayload;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.dto.chat.websocket.MessageStatusCommand;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import com.example.it.mentor.dto.chat.websocket.TypingCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice(assignableTypes = ChatWebSocketController.class)
public class ChatWebSocketExceptionHandler {

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(ChatRealtimePublisher.USER_EVENTS_DESTINATION)
    public ChatEventEnvelope handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ChatEventEnvelope.of(
                "chat.command.error",
                extractRequestId(exception.getBindingResult().getTarget()),
                null,
                new ChatCommandErrorPayload(
                        "VALIDATION_ERROR", "Ошибка валидации команды", fieldErrors)
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(ChatRealtimePublisher.USER_EVENTS_DESTINATION)
    public ChatEventEnvelope handleUnexpected(Exception exception) {
        log.error("Неожиданная ошибка WebSocket-команды", exception);
        return ChatEventEnvelope.of(
                "chat.command.error",
                null,
                null,
                new ChatCommandErrorPayload(
                        "INTERNAL_ERROR", "Внутренняя ошибка сервера", Map.of())
        );
    }

    private UUID extractRequestId(Object command) {
        return switch (command) {
            case SendMessageCommand value -> value.requestId();
            case MessageStatusCommand value -> value.requestId();
            case TypingCommand value -> value.requestId();
            default -> null;
        };
    }
}
