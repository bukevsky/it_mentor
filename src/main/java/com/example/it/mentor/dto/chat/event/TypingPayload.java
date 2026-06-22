package com.example.it.mentor.dto.chat.event;

public record TypingPayload(Long chatId, Long userId, boolean typing) {
}
