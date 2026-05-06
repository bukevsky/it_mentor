package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для конфликтов данных, например при попытке создать дубликат (HTTP 409).
 */
public class ConflictException extends ApiException {

    /**
     * @param message описание конфликта данных
     */
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
