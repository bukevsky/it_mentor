package com.example.it.mentor.dto;

public record StudentFilesResponse(
        FileResponse resume,
        int portfolioCount,
        Long avatarFileId
) {}
