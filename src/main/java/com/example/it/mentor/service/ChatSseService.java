package com.example.it.mentor.service;

import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.sse.PresencePayload;
import com.example.it.mentor.dto.sse.TypingPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatSseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Value("${app.sse.timeout-ms:300000}")
    private long sseTimeoutMs;

    public SseEmitter subscribe(Long userId, Runnable onDisconnect) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        emitters.put(userId, emitter);
        Runnable cleanup = () -> {
            emitters.remove(userId, emitter);
            if (onDisconnect != null) onDisconnect.run();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        log.debug("SSE подписка: userId={}", userId);
        return emitter;
    }

    public boolean isOnline(Long userId) {
        return emitters.containsKey(userId);
    }

    public void pushMessageCreated(Long studentUserId, Long mentorUserId, ChatMessageResponse msg) {
        pushToUser(studentUserId, "chat.message.created", msg);
        pushToUser(mentorUserId, "chat.message.created", msg);
    }

    public void pushReadEvent(Long studentUserId, Long mentorUserId, Long chatId) {
        pushToUser(studentUserId, "chat.read", Map.of("chatId", chatId));
        pushToUser(mentorUserId, "chat.read", Map.of("chatId", chatId));
    }

    public void pushTyping(Long targetUserId, TypingPayload payload) {
        pushToUser(targetUserId, "chat.typing", payload);
    }

    public void pushPresenceChanged(Set<Long> recipientIds, PresencePayload payload) {
        recipientIds.forEach(uid -> pushToUser(uid, "presence.changed", payload));
    }

    @Scheduled(fixedDelay = 15_000)
    public void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("SSE heartbeat не удался, удаляем emitter: userId={}", userId);
                emitter.complete();
                emitters.remove(userId, emitter);
            }
        });
    }

    private void pushToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("SSE push не удался, удаляем emitter: userId={}", userId);
            emitters.remove(userId, emitter);
        }
    }
}
