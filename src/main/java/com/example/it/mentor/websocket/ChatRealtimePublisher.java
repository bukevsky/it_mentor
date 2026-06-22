package com.example.it.mentor.websocket;

import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRealtimePublisher {

    public static final String USER_EVENTS_DESTINATION = "/queue/chat-events";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void publishToPrincipal(String principalName, ChatEventEnvelope event) {
        messagingTemplate.convertAndSendToUser(principalName, USER_EVENTS_DESTINATION, event);
    }

    public void publishToUser(Long userId, ChatEventEnvelope event) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        publishToPrincipal(user.getEmail(), event);
    }

    public void publishToUsers(Set<Long> userIds, ChatEventEnvelope event) {
        userIds.forEach(userId -> publishToUser(userId, event));
        log.debug("WebSocket событие опубликовано: type={}, chatId={}, recipientCount={}, step={}",
                event.type(), event.chatId(), userIds.size(), "websocket_event_published");
    }
}
