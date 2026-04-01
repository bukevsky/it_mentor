package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для случаев, когда аутентифицированный пользователь
 * не имеет прав на выполнение операции (HTTP 403).
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
