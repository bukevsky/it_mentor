package com.example.it.mentor.exception;

import com.example.it.mentor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Централизованный обработчик исключений для всех REST-контроллеров.
 *
 * <p>Перехватывает как бизнес-исключения ({@link ApiException} и наследников),
 * так и инфраструктурные ошибки Spring MVC (валидация, неподдерживаемый метод,
 * некорректный JSON). Все ответы возвращаются в формате {@link ErrorResponse}.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки Bean Validation ({@code @Valid} на теле запроса).
     * Формирует список нарушений в формате {@code поле: сообщение}.
     *
     * @return HTTP 400 со списком полей, не прошедших валидацию
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR", "Ошибка валидации", request.getRequestURI(), details));
    }

    /**
     * Обрабатывает все бизнес-исключения {@link ApiException}.
     * HTTP-статус берётся из самого исключения.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.debug("ApiException [{}]: {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus().value(), ex.getStatus().name(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Обрабатывает ошибки десериализации тела запроса (некорректный или отсутствующий JSON).
     *
     * @return HTTP 400 с кодом {@code MALFORMED_JSON}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "MALFORMED_JSON", "Некорректное тело запроса", request.getRequestURI()));
    }

    /**
     * Обрабатывает обращения к эндпоинтам с неподдерживаемым HTTP-методом.
     *
     * @return HTTP 405 с кодом {@code METHOD_NOT_ALLOWED}
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(405, "METHOD_NOT_ALLOWED", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Fallback-обработчик для всех необработанных исключений.
     * Детали ошибки логируются, но не возвращаются клиенту.
     *
     * @return HTTP 500 с кодом {@code INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Необработанное исключение: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Внутренняя ошибка сервера", request.getRequestURI()));
    }
}
