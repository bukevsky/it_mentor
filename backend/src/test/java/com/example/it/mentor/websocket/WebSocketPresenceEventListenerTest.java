package com.example.it.mentor.websocket;

import com.example.it.mentor.security.AppUserDetails;
import com.example.it.mentor.service.PresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketPresenceEventListener")
class WebSocketPresenceEventListenerTest {

    @InjectMocks private WebSocketPresenceEventListener listener;
    @Mock private WebSocketSessionRegistry sessionRegistry;
    @Mock private PresenceService presenceService;

    @Test
    @DisplayName("первая CONNECT-сессия переводит пользователя online")
    void firstConnectedSession_shouldSetOnline() {
        when(sessionRegistry.register(10L, "session-1")).thenReturn(true);

        listener.onConnected(connectedEvent("session-1"));

        verify(presenceService).setOnline(10L);
    }

    @Test
    @DisplayName("дополнительная CONNECT-сессия не дублирует online event")
    void additionalConnectedSession_shouldNotSetOnlineAgain() {
        when(sessionRegistry.register(10L, "session-2")).thenReturn(false);

        listener.onConnected(connectedEvent("session-2"));

        verify(presenceService, never()).setOnline(10L);
    }

    @Test
    @DisplayName("последний DISCONNECT переводит пользователя offline")
    void lastDisconnectedSession_shouldSetOffline() {
        when(sessionRegistry.unregister("session-1"))
                .thenReturn(new SessionDisconnectResult(10L, true));

        listener.onDisconnected(disconnectedEvent("session-1"));

        verify(presenceService).setOffline(10L);
    }

    private SessionConnectedEvent connectedEvent(String sessionId) {
        return new SessionConnectedEvent(this, message(sessionId), authentication());
    }

    private SessionDisconnectEvent disconnectedEvent(String sessionId) {
        return new SessionDisconnectEvent(
                this, message(sessionId), sessionId, CloseStatus.NORMAL, authentication());
    }

    private Message<byte[]> message(String sessionId) {
        return MessageBuilder.withPayload(new byte[0])
                .setHeader("simpSessionId", sessionId)
                .build();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        AppUserDetails details = new AppUserDetails(
                10L, "user@test.com", "password", true, 0L, List.of());
        return new UsernamePasswordAuthenticationToken(details, null, List.of());
    }
}
