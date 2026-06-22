package com.example.it.mentor.service;

import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;

import java.time.OffsetDateTime;

public record MessageStatusChangeResult(
        Long chatId,
        Long actorUserId,
        Long targetUserId,
        Long upToMessageId,
        ChatMessageDeliveryStatus status,
        OffsetDateTime changedAt,
        int changedCount
) {
}
