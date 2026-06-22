package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.dto.chat.websocket.ChatCommandAckPayload;
import com.example.it.mentor.dto.chat.websocket.ChatCommandErrorPayload;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.dto.chat.websocket.MessageStatusChangedPayload;
import com.example.it.mentor.dto.chat.websocket.MessageStatusCommand;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import com.example.it.mentor.dto.chat.websocket.TypingCommand;
import com.example.it.mentor.exception.ApiException;
import com.example.it.mentor.service.ChatMessageStatusService;
import com.example.it.mentor.service.ChatService;
import com.example.it.mentor.service.MessageStatusChangeResult;
import com.example.it.mentor.service.SendMessageResult;
import com.example.it.mentor.service.TypingResult;
import com.example.it.mentor.service.TypingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRealtimeFacade {

    private final ChatService chatService;
    private final ChatMessageStatusService statusService;
    private final TypingService typingService;
    private final ChatRealtimePublisher publisher;

    public void send(String principalName, Long chatId, SendMessageCommand command) {
        try {
            SendMessageResult result = chatService.sendMessage(
                    chatId,
                    command.requestId(),
                    new SendMessageRequest(command.body(), command.attachmentFileId())
            );
            if (!result.duplicate()) {
                publisher.publishToUsers(
                        Set.of(result.studentUserId(), result.mentorUserId()),
                        ChatEventEnvelope.of(
                                "chat.message.created", command.requestId(), chatId, result.message())
                );
            }
            acknowledge(principalName, command.requestId(), chatId,
                    "SEND_MESSAGE", result.message().id(), result.duplicate());
        } catch (ApiException exception) {
            publishError(principalName, command.requestId(), chatId, exception);
        }
    }

    public void delivered(String principalName, Long chatId, MessageStatusCommand command) {
        updateStatus(principalName, chatId, command, true);
    }

    public void read(String principalName, Long chatId, MessageStatusCommand command) {
        updateStatus(principalName, chatId, command, false);
    }

    public void typing(String principalName, Long chatId, TypingCommand command) {
        try {
            TypingResult result = typingService.handleTyping(chatId, command.typing());
            publisher.publishToUser(
                    result.targetUserId(),
                    ChatEventEnvelope.of("chat.typing", command.requestId(), chatId, result.payload())
            );
            acknowledge(principalName, command.requestId(), chatId, "TYPING", null, false);
        } catch (ApiException exception) {
            publishError(principalName, command.requestId(), chatId, exception);
        }
    }

    private void updateStatus(String principalName,
                              Long chatId,
                              MessageStatusCommand command,
                              boolean delivered) {
        try {
            MessageStatusChangeResult result = delivered
                    ? statusService.markDelivered(chatId, command.upToMessageId())
                    : statusService.markRead(chatId, command.upToMessageId());
            MessageStatusChangedPayload payload = new MessageStatusChangedPayload(
                    result.actorUserId(),
                    result.upToMessageId(),
                    result.status(),
                    result.changedAt(),
                    result.changedCount()
            );
            publisher.publishToUsers(
                    Set.of(result.actorUserId(), result.targetUserId()),
                    ChatEventEnvelope.of(
                            "chat.message.status.changed", command.requestId(), chatId, payload)
            );
            acknowledge(principalName, command.requestId(), chatId,
                    delivered ? "MARK_DELIVERED" : "MARK_READ", command.upToMessageId(), false);
        } catch (ApiException exception) {
            publishError(principalName, command.requestId(), chatId, exception);
        }
    }

    private void acknowledge(String principalName,
                             UUID requestId,
                             Long chatId,
                             String command,
                             Long resourceId,
                             boolean duplicate) {
        publisher.publishToPrincipal(
                principalName,
                ChatEventEnvelope.of(
                        "chat.command.ack",
                        requestId,
                        chatId,
                        new ChatCommandAckPayload(command, resourceId, duplicate)
                )
        );
    }

    private void publishError(String principalName, UUID requestId, Long chatId, ApiException exception) {
        String code = switch (exception.getStatus()) {
            case HttpStatus.NOT_FOUND -> "NOT_FOUND";
            case HttpStatus.FORBIDDEN -> "FORBIDDEN";
            case HttpStatus.CONFLICT -> "CONFLICT";
            case HttpStatus.UNPROCESSABLE_ENTITY -> "BUSINESS_RULE_VIOLATION";
            default -> exception.getStatus().name();
        };
        publisher.publishToPrincipal(
                principalName,
                ChatEventEnvelope.of(
                        "chat.command.error",
                        requestId,
                        chatId,
                        new ChatCommandErrorPayload(code, exception.getMessage(), Map.of())
                )
        );
    }
}
