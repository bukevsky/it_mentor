package com.example.it.mentor.security;

import com.example.it.mentor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Обработчик попытки доступа к защищённому ресурсу без аутентификации.
 *
 * <p>Возвращает HTTP 401 с JSON-телом {@link com.example.it.mentor.dto.ErrorResponse}
 * вместо стандартного HTML-ответа Spring Security.</p>
 */
@Component
@RequiredArgsConstructor
public class Http401EntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Формирует JSON-ответ для неаутентифицированного доступа к защищённому ресурсу.
     *
     * @param request исходный HTTP-запрос
     * @param response HTTP-ответ
     * @param authException причина отказа в аутентификации
     * @throws IOException если не удалось записать тело ответа
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(401, "UNAUTHORIZED", "Требуется аутентификация", request.getRequestURI()));
    }
}
