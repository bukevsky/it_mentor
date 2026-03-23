package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для нарушений бизнес-правил, которые не покрываются Bean Validation (HTTP 422).
 *
 * <p>Примеры: попытка загрузить резюме без существующего профиля студента,
 * сброс пароля по истёкшему токену.</p>
 */
public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
