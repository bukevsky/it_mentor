package com.example.it.mentor.dto.sse;

public record TypingPayload(
        Long chatId,
        Long userId,
        boolean typing
) {}
