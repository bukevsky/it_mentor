package com.example.it.mentor.dto;

import java.io.InputStream;

public record FileDownloadInfo(
        InputStream stream,
        String contentType,
        String originalFilename,
        Long size
) {}
