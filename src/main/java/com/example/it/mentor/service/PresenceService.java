package com.example.it.mentor.service;

import com.example.it.mentor.dto.presence.PresenceResponse;
import com.example.it.mentor.dto.sse.PresencePayload;
import com.example.it.mentor.entity.UserPresence;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.UserPresenceRepository;
import com.example.it.mentor.repository.UserRepository;
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
    private final ChatSseService sseService;
    private final UserRepository userRepository;

    public void setOnline(Long userId) {
        log.info("Пользователь подключился: userId={}", userId);
        OffsetDateTime lastSeenAt = presenceRepository.findByUserId(userId)
                .map(UserPresence::getLastSeenAt)
                .orElse(null);
        broadcastPresence(userId, "online", lastSeenAt);
    }

    @Transactional
    public void setOffline(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPresence.builder().userId(userId).lastSeenAt(now).build());
        presence.setLastSeenAt(now);
        presenceRepository.save(presence);
        log.info("Пользователь отключился: userId={}, lastSeenAt={}", userId, now);
        broadcastPresence(userId, "offline", now);
    }

    @Transactional(readOnly = true)
    public PresenceResponse getPresence(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден: " + userId);
        }
        boolean online = sseService.isOnline(userId);
        OffsetDateTime lastSeenAt = presenceRepository.findByUserId(userId)
                .map(UserPresence::getLastSeenAt)
                .orElse(null);
        return new PresenceResponse(userId, online ? "online" : "offline", lastSeenAt);
    }

    private void broadcastPresence(Long userId, String status, OffsetDateTime lastSeenAt) {
        PresencePayload payload = new PresencePayload(userId, status, lastSeenAt);
        List<Long> partnerIds = chatRepository.findAllChatPartnerIds(userId);
        Set<Long> recipients = new HashSet<>(partnerIds);
        sseService.pushPresenceChanged(recipients, payload);
    }
}
