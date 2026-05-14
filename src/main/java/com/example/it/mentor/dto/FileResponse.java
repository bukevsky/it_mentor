package com.example.it.mentor.dto;

import java.time.OffsetDateTime;

public record FileResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long size,
        String fileType,
        String status,
        String previewUrl,
        OffsetDateTime uploadedAt
) {}
