package com.example.it.mentor.security;

import com.example.it.mentor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик отказа в доступе для аутентифицированного пользователя без нужных прав.
 *
 * <p>Возвращает HTTP 403 с JSON-телом {@link com.example.it.mentor.dto.ErrorResponse}
 * вместо стандартного HTML-ответа Spring Security.</p>
 */
@Component
@RequiredArgsConstructor
public class Http403AccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Формирует JSON-ответ для аутентифицированного пользователя без нужных прав.
     *
     * @param request исходный HTTP-запрос
     * @param response HTTP-ответ
     * @param accessDeniedException причина отказа в доступе
     * @throws IOException если не удалось записать тело ответа
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(403, "FORBIDDEN", "Доступ запрещён", request.getRequestURI()));
    }
}
