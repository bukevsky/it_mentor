package com.example.it.mentor.dto.chat.websocket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TypingCommand(
        @NotNull(message = "Поле requestId обязательно") UUID requestId,
        @NotNull(message = "Поле typing обязательно") Boolean typing
) {
}
