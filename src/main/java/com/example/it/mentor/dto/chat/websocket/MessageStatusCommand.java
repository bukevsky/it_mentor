package com.example.it.mentor.dto.chat.websocket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record MessageStatusCommand(
        @NotNull(message = "Поле requestId обязательно") UUID requestId,
        @NotNull(message = "Поле upToMessageId обязательно")
        @Positive(message = "upToMessageId должен быть положительным") Long upToMessageId
) {
}
