package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Базовый класс для всех бизнес-исключений приложения.
 *
 * <p>Каждый наследник жёстко привязан к конкретному HTTP-статусу, который
 * используется в {@link GlobalExceptionHandler} при формировании ответа клиенту.</p>
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * @param message сообщение об ошибке (отображается клиенту)
     * @param status  HTTP-статус ответа
     */
    protected ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * @param message сообщение об ошибке (отображается клиенту)
     * @param status  HTTP-статус ответа
     * @param cause   исходное исключение
     */
    protected ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * @return HTTP-статус, соответствующий этому типу ошибки
     */
    public HttpStatus getStatus() {
        return status;
    }
}
