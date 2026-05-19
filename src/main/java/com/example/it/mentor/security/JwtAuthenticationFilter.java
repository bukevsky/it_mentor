package com.example.it.mentor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр аутентификации по JWT-токену.
 *
 * <p>На каждый входящий запрос извлекает Bearer-токен из заголовка {@code Authorization},
 * валидирует его через {@link JwtProvider} и, если токен корректен, устанавливает
 * аутентификацию в {@link org.springframework.security.core.context.SecurityContext}.
 * При ошибке обработки токена запрос продолжается без аутентификации.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String USER_ID_MDC_KEY = "userId";

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Пытается аутентифицировать пользователя по JWT из заголовка {@code Authorization}.
     *
     * @param request входящий HTTP-запрос
     * @param response HTTP-ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException если следующий фильтр завершился ошибкой сервлета
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.remove(USER_ID_MDC_KEY);
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                String email = jwtProvider.extractUsername(token);
                AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled()) {
                    log.warn("JWT отклонён: пользователь {} заблокирован или удалён", email);
                } else {
                    Long jwtTokenVersion = jwtProvider.extractTokenVersion(token);
                    Long userTokenVersion = userDetails.getTokenVersion();
                    boolean tokenVersionMatches = jwtTokenVersion == null
                            ? userTokenVersion == 0L
                            : jwtTokenVersion.equals(userTokenVersion);

                    if (!tokenVersionMatches) {
                        log.warn("JWT инвалидирован: пользователь {} обновил tokenVersion (jwt={}, db={})",
                                email, jwtTokenVersion, userTokenVersion);
                    } else {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        MDC.put(USER_ID_MDC_KEY, String.valueOf(userDetails.getUserId()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось установить аутентификацию пользователя: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Извлекает Bearer-токен из заголовка {@code Authorization}.
     *
     * @param request входящий HTTP-запрос
     * @return токен без префикса или {@code null}, если заголовок отсутствует
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
