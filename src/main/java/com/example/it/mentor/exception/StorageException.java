package com.example.it.mentor.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {

    public StorageException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
