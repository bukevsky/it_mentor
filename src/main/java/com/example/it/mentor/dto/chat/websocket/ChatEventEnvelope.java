package com.example.it.mentor.dto.chat.websocket;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatEventEnvelope(
        String type,
        UUID requestId,
        Long chatId,
        OffsetDateTime occurredAt,
        Object payload
) {
    public static ChatEventEnvelope of(String type, UUID requestId, Long chatId, Object payload) {
        return new ChatEventEnvelope(type, requestId, chatId, OffsetDateTime.now(), payload);
    }
}
