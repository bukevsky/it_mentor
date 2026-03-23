package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для ошибок при работе с объектным хранилищем (HTTP 500).
 *
 * <p>Оборачивает низкоуровневые исключения MinIO SDK, скрывая детали
 * инфраструктуры от вышестоящих слоёв.</p>
 */
public class StorageException extends ApiException {

    public StorageException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
