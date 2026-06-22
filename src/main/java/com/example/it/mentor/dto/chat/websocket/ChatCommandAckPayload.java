package com.example.it.mentor.dto.chat.websocket;

public record ChatCommandAckPayload(String command, Long resourceId, boolean duplicate) {
}
