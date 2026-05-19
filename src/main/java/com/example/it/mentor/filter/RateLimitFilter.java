package com.example.it.mentor.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private final LoadingCache<String, AtomicInteger> requestCounts;
    private final int maxRequests;
    private final List<String> rateLimitedPaths;

    public RateLimitFilter(
            @Value("${app.security.rate-limit.auth-per-minute:10}") int maxRequests,
            @Value("${app.security.rate-limit.paths:/auth/login,/auth/password/forgot,/auth/password/reset}") String pathsConfig) {
        this.maxRequests = maxRequests;
        this.rateLimitedPaths = Arrays.asList(pathsConfig.split(","));
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(key -> new AtomicInteger(0));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        if (!isRateLimited(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String key = resolveKey(request);
        int count = requestCounts.get(key).incrementAndGet();
        if (count > maxRequests) {
            log.warn("Rate limit превышен: key={}, count={}", key, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Слишком много запросов. Попробуйте через минуту.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String uri) {
        return rateLimitedPaths.stream().anyMatch(uri::startsWith);
    }

    private String resolveKey(HttpServletRequest request) {
        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(h -> h.split(",")[0].strip())
                .orElse(request.getRemoteAddr());
        return ip + ":" + request.getRequestURI();
    }
}
