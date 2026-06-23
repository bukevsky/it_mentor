package com.example.it.mentor.exception;

import com.example.it.mentor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        log.debug("Ошибка валидации [{} {}]: {}", request.getMethod(), request.getRequestURI(), details);

        return badRequest("Ошибка валидации", request, details);
    }

    /**
     * Обрабатывает нарушения @Min/@Max на @RequestParam (бросается через AOP-прокси @Validated).
     *
     * @return HTTP 400 с кодом {@code VALIDATION_ERROR}
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return badRequest("Ошибка валидации параметров запроса", request, details);
    }

    /**
     * Обрабатывает HandlerMethodValidationException (Spring 7 для @Validated параметров контроллера).
     *
     * @return HTTP 400 с кодом {@code VALIDATION_ERROR}
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
                                                                HttpServletRequest request) {
        List<String> details = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        return badRequest("Ошибка валидации параметров", request, details);
    }

    /**
     * Обрабатывает ошибки несоответствия типа параметра запроса.
     *
     * @return HTTP 400 с описанием некорректного значения
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String detail = ex.getName() + ": некорректное значение '" + ex.getValue() + "'";
        return badRequest("Ошибка валидации параметров", request, List.of(detail));
    }

    /**
     * Обрабатывает все бизнес-исключения {@link ApiException}.
     * HTTP-статус берётся из самого исключения.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        int statusValue = ex.getStatus().value();
        if (statusValue == 401 || statusValue == 403) {
            log.warn("Доступ запрещён [{}] {} {}: {}", statusValue, request.getMethod(), request.getRequestURI(), ex.getMessage());
        } else {
            log.debug("ApiException [{}] {} {}: {}", statusValue, request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(statusValue, ex.getStatus().name(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Обрабатывает конфликты при оптимистичной блокировке (concurrent update).
     *
     * @return HTTP 409 с кодом {@code CONFLICT}
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        log.warn("Конфликт оптимистичной блокировки: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "CONFLICT", "Данные были изменены другим пользователем. Повторите запрос.", request.getRequestURI()));
    }

    /**
     * Обрабатывает нарушения целостности БД (дублирование уникальных ключей и т.д.).
     *
     * @return HTTP 409 с кодом {@code CONFLICT}
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Нарушение целостности данных: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "CONFLICT", "Конфликт данных", request.getRequestURI()));
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
     * Обрабатывает обращения к несуществующим путям (DispatcherServlet не нашёл handler).
     *
     * @return HTTP 404 с кодом {@code NOT_FOUND}
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "NOT_FOUND", "Ресурс не найден", request.getRequestURI()));
    }

    /**
     * Обрабатывает отсутствие обязательного @RequestParam.
     *
     * @return HTTP 400 с кодом {@code VALIDATION_ERROR}
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        String detail = ex.getParameterName() + ": обязательный параметр отсутствует";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR",
                        "Отсутствует обязательный параметр запроса",
                        request.getRequestURI(), List.of(detail)));
    }

    /**
     * Fallback-обработчик для всех необработанных исключений.
     * Детали ошибки логируются, но не возвращаются клиенту.
     *
     * @return HTTP 500 с кодом {@code INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Необработанное исключение [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Внутренняя ошибка сервера", request.getRequestURI()));
    }

    /**
     * Формирует унифицированный ответ для ошибок валидации клиента.
     *
     * @param message пользовательское сообщение об ошибке
     * @param request исходный HTTP-запрос
     * @param details список деталей ошибки
     * @return ответ со статусом {@code 400 BAD REQUEST}
     */
    private ResponseEntity<ErrorResponse> badRequest(String message,
                                                     HttpServletRequest request,
                                                     List<String> details) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR", message, request.getRequestURI(), details));
    }
}
