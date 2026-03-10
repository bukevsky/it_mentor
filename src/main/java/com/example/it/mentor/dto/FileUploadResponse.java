package com.example.it.mentor.dto;

import com.example.it.mentor.entity.enums.FileType;

import java.time.OffsetDateTime;

public record FileUploadResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long size,
        FileType fileType,
        OffsetDateTime uploadedAt
) {}
