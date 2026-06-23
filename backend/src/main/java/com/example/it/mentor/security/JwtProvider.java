package com.example.it.mentor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Компонент для создания, валидации и разбора JWT-токенов.
 *
 * <p>Использует алгоритм HMAC-SHA256. Секрет и время жизни токена
 * задаются через {@code app.jwt.secret} и {@code app.jwt.expiration-ms}
 * в конфигурации приложения.</p>
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key;

    /**
     * Инициализирует криптографический ключ из конфигурационного секрета.
     */
    @PostConstruct
    private void initKey() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Генерирует JWT-токен с tokenVersion claim для мгновенной инвалидации сессий.
     *
     * @param username     email пользователя
     * @param tokenVersion текущая версия токена из БД
     * @return подписанный компактный JWT-токен
     */
    public String generateToken(String username, Long tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Извлекает tokenVersion из claim 'tv'. Возвращает null для legacy токенов без этого claim.
     *
     * @param token JWT-токен
     * @return tokenVersion или null если claim отсутствует (legacy токен)
     */
    public Long extractTokenVersion(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object tv = claims.get("tv");
        if (tv == null) {
            return null;
        }
        if (tv instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("Невалидный claim tv: " + tv);
    }

    /**
     * Извлекает имя пользователя (email) из JWT-токена без предварительной валидации.
     *
     * @param token JWT-токен
     * @return subject токена (email пользователя)
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Проверяет подпись и срок действия JWT-токена.
     *
     * @param token JWT-токен
     * @return {@code true} если токен валиден, {@code false} в случае любой ошибки
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
