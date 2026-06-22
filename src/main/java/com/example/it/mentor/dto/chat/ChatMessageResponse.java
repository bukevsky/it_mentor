package com.example.it.mentor.dto.chat;

import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        Long id,
        Long chatId,
        Long senderUserId,
        UUID clientMessageId,
        String body,
        AttachmentInfo attachment,
        ChatMessageDeliveryStatus deliveryStatus,
        OffsetDateTime deliveredAt,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
