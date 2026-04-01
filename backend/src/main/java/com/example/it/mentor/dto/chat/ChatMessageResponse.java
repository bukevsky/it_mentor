package com.example.it.mentor.dto.chat;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id,
        Long chatId,
        Long senderUserId,
        String body,
        AttachmentInfo attachment,
        OffsetDateTime createdAt
) {}
