package com.example.it.mentor.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> usersBySession = new ConcurrentHashMap<>();

    public synchronized boolean register(Long userId, String sessionId) {
        usersBySession.put(sessionId, userId);
        Set<String> sessions = sessionsByUser.computeIfAbsent(
                userId, ignored -> ConcurrentHashMap.newKeySet());
        sessions.add(sessionId);
        return sessions.size() == 1;
    }

    public synchronized SessionDisconnectResult unregister(String sessionId) {
        Long userId = usersBySession.remove(sessionId);
        if (userId == null) {
            return new SessionDisconnectResult(null, false);
        }

        Set<String> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return new SessionDisconnectResult(userId, true);
        }
        sessions.remove(sessionId);
        boolean lastSession = sessions.isEmpty();
        if (lastSession) {
            sessionsByUser.remove(userId, sessions);
        }
        return new SessionDisconnectResult(userId, lastSession);
    }

    public boolean isOnline(Long userId) {
        Set<String> sessions = sessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
