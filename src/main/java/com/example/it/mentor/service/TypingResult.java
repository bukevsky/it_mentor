package com.example.it.mentor.service;

import com.example.it.mentor.dto.chat.event.TypingPayload;

public record TypingResult(Long targetUserId, TypingPayload payload) {
}
