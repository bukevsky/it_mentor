package com.example.it.mentor.dto.chat.websocket;

import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;

import java.time.OffsetDateTime;

public record MessageStatusChangedPayload(
        Long actorUserId,
        Long upToMessageId,
        ChatMessageDeliveryStatus status,
        OffsetDateTime changedAt,
        int changedCount
) {
}
