package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для ошибок аутентификации в бизнес-логике (HTTP 401).
 *
 * <p>Отличается от {@link com.example.it.mentor.security.Http401EntryPoint},
 * который перехватывает запросы без токена на уровне фильтров Spring Security.</p>
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
