package com.example.it.mentor.websocket;

import com.example.it.mentor.security.AppUserDetails;
import com.example.it.mentor.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final PresenceService presenceService;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Long userId = extractUserId(event.getUser());
        String sessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (userId == null || sessionId == null) {
            return;
        }
        if (sessionRegistry.register(userId, sessionId)) {
            presenceService.setOnline(userId);
        }
        log.debug("WebSocket сессия подключена: userId={}, sessionId={}, step={}",
                userId, sessionId, "websocket_session_connected");
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        SessionDisconnectResult result = sessionRegistry.unregister(event.getSessionId());
        if (result.userId() == null) {
            return;
        }
        if (result.lastSession()) {
            presenceService.setOffline(result.userId());
        }
        log.debug("WebSocket сессия отключена: userId={}, sessionId={}, lastSession={}, step={}",
                result.userId(), event.getSessionId(), result.lastSession(),
                "websocket_session_disconnected");
    }

    private Long extractUserId(java.security.Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AppUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
