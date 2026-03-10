package com.example.it.mentor.entity.enums;

import com.example.it.mentor.exception.BusinessRuleViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public enum FileType {
    RESUME("Резюме",
            Set.of("application/pdf"),
            5L * 1024 * 1024,
            "Резюме должно быть в формате PDF",
            "Размер резюме не должен превышать 5 МБ"),
    PORTFOLIO("Портфолио",
            Set.of("application/pdf", "image/jpeg", "image/png"),
            10L * 1024 * 1024,
            "Портфолио должно быть PDF, JPEG или PNG",
            "Размер файла не должен превышать 10 МБ"),
    ATTACHMENT("Вложение",
            Set.of(),
            Long.MAX_VALUE,
            null,
            null),
    AVATAR("Аватар",
            Set.of("image/jpeg", "image/png", "image/webp"),
            2L * 1024 * 1024,
            "Аватар должен быть JPEG, PNG или WebP",
            "Размер аватара не должен превышать 2 МБ");

    private final String label;
    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;
    private final String contentTypeError;
    private final String sizeError;

    FileType(String label, Set<String> allowedContentTypes, long maxSizeBytes,
             String contentTypeError, String sizeError) {
        this.label = label;
        this.allowedContentTypes = allowedContentTypes;
        this.maxSizeBytes = maxSizeBytes;
        this.contentTypeError = contentTypeError;
        this.sizeError = sizeError;
    }

    public String getLabel() {
        return label;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void validate(MultipartFile file) {
        if (!allowedContentTypes.isEmpty()
                && !allowedContentTypes.contains(file.getContentType())) {
            throw new BusinessRuleViolationException(contentTypeError);
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessRuleViolationException(sizeError);
        }
    }
}
