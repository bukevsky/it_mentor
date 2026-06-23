package com.example.it.mentor.service;

import com.example.it.mentor.dto.presence.PresenceResponse;
import com.example.it.mentor.dto.chat.event.PresencePayload;
import com.example.it.mentor.dto.chat.websocket.ChatEventEnvelope;
import com.example.it.mentor.entity.UserPresence;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.UserPresenceRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.websocket.ChatRealtimePublisher;
import com.example.it.mentor.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserPresenceRepository presenceRepository;
    private final ChatRepository chatRepository;
    private final ChatRealtimePublisher realtimePublisher;
    private final WebSocketSessionRegistry sessionRegistry;
    private final UserRepository userRepository;

    public void setOnline(Long userId) {
        OffsetDateTime lastSeenAt = presenceRepository.findByUserId(userId)
                .map(UserPresence::getLastSeenAt)
                .orElse(null);
        log.info("Пользователь подключился: userId={}, lastSeenAt={}, step={}",
                userId, lastSeenAt, "user_presence_online");
        broadcastPresence(userId, "online", lastSeenAt);
    }

    @Transactional
    public void setOffline(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPresence.builder().userId(userId).lastSeenAt(now).build());
        presence.setLastSeenAt(now);
        presenceRepository.save(presence);
        log.info("Пользователь отключился: userId={}, lastSeenAt={}, step={}",
                userId, now, "user_presence_offline");
        broadcastPresence(userId, "offline", now);
    }

    @Transactional(readOnly = true)
    public PresenceResponse getPresence(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден: " + userId);
        }
        boolean online = sessionRegistry.isOnline(userId);
        OffsetDateTime lastSeenAt = presenceRepository.findByUserId(userId)
                .map(UserPresence::getLastSeenAt)
                .orElse(null);
        log.debug("Статус присутствия загружен: userId={}, online={}, lastSeenAt={}, step={}",
                userId, online, lastSeenAt, "user_presence_loaded");
        return new PresenceResponse(userId, online ? "online" : "offline", lastSeenAt);
    }

    private void broadcastPresence(Long userId, String status, OffsetDateTime lastSeenAt) {
        PresencePayload payload = new PresencePayload(userId, status, lastSeenAt);
        List<Long> partnerIds = chatRepository.findAllChatPartnerIds(userId);
        Set<Long> recipients = new HashSet<>(partnerIds);
        realtimePublisher.publishToUsers(
                recipients,
                ChatEventEnvelope.of("presence.changed", null, null, payload)
        );
        log.debug("Статус присутствия отправлен партнёрам: userId={}, status={}, recipientCount={}, step={}",
                userId, status, recipients.size(), "user_presence_broadcasted");
    }
}
