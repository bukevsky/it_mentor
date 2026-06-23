package com.example.it.mentor.dto.chat;

import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @Size(max = 10000, message = "Текст сообщения не должен превышать 10000 символов")
        String body,
        Long attachmentFileId
) {}
