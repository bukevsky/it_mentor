package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.websocket.MessageStatusCommand;
import com.example.it.mentor.dto.chat.websocket.SendMessageCommand;
import com.example.it.mentor.dto.chat.websocket.TypingCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatRealtimeFacade facade;

    @MessageMapping("/chats/{chatId}/messages/send")
    public void send(@DestinationVariable Long chatId,
                     @Valid @Payload SendMessageCommand command,
                     Principal principal) {
        facade.send(principal.getName(), chatId, command);
    }

    @MessageMapping("/chats/{chatId}/messages/delivered")
    public void delivered(@DestinationVariable Long chatId,
                          @Valid @Payload MessageStatusCommand command,
                          Principal principal) {
        facade.delivered(principal.getName(), chatId, command);
    }

    @MessageMapping("/chats/{chatId}/messages/read")
    public void read(@DestinationVariable Long chatId,
                     @Valid @Payload MessageStatusCommand command,
                     Principal principal) {
        facade.read(principal.getName(), chatId, command);
    }

    @MessageMapping("/chats/{chatId}/typing")
    public void typing(@DestinationVariable Long chatId,
                       @Valid @Payload TypingCommand command,
                       Principal principal) {
        facade.typing(principal.getName(), chatId, command);
    }
}
