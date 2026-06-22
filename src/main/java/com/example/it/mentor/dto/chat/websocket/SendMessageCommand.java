package com.example.it.mentor.dto.chat.websocket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageCommand(
        @NotNull(message = "Поле requestId обязательно") UUID requestId,
        @Size(max = 10000, message = "Текст сообщения не должен превышать 10000 символов") String body,
        Long attachmentFileId
) {
}
