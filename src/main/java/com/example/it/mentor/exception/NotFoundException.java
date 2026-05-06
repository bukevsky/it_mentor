package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для случаев, когда запрошенный ресурс не найден (HTTP 404).
 */
public class NotFoundException extends ApiException {

    /**
     * @param message описание отсутствующего ресурса
     */
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
