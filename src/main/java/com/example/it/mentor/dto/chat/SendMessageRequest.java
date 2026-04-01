package com.example.it.mentor.dto.chat;

public record SendMessageRequest(
        String body,
        Long attachmentFileId
) {}
