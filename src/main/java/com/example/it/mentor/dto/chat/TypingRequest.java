package com.example.it.mentor.dto.chat;

import jakarta.validation.constraints.NotNull;

public record TypingRequest(
        @NotNull(message = "Поле typing обязательно")
        Boolean typing
) {}
