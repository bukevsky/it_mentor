package com.example.it.mentor.service;

import com.example.it.mentor.dto.chat.ChatMessageResponse;

public record SendMessageResult(
        ChatMessageResponse message,
        Long studentUserId,
        Long mentorUserId,
        boolean duplicate
) {
}
