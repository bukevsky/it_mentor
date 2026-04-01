package com.example.it.mentor.dto.chat;

public record AttachmentInfo(
        Long fileId,
        String originalFilename,
        String contentType,
        Long size
) {}
