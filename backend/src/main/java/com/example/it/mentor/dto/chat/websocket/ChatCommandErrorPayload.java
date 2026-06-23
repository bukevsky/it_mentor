package com.example.it.mentor.dto.chat.websocket;

import java.util.Map;

public record ChatCommandErrorPayload(String code, String message, Map<String, String> fieldErrors) {
}
