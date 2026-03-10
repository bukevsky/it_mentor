package com.example.it.mentor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", "test-secret-key-must-be-at-least-32-chars!");
        ReflectionTestUtils.setField(jwtProvider, "expirationMs", 3_600_000L);
        ReflectionTestUtils.invokeMethod(jwtProvider, "initKey");
    }

    @Test
    @DisplayName("generateToken: возвращает непустой JWT")
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtProvider.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername: достаёт email из валидного JWT")
    void extractUsername_shouldReturnCorrectEmail() {
        String token = jwtProvider.generateToken("user@example.com");
        assertThat(jwtProvider.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("validateToken: валидный токен → true")
    void validateToken_withValidToken_shouldReturnTrue() {
        String token = jwtProvider.generateToken("user@example.com");
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: повреждённый токен → false")
    void validateToken_withTamperedToken_shouldReturnFalse() {
        assertThat(jwtProvider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken: истёкший токен → false")
    void validateToken_withExpiredToken_shouldReturnFalse() {
        JwtProvider shortLived = new JwtProvider();
        ReflectionTestUtils.setField(shortLived, "secret", "test-secret-key-must-be-at-least-32-chars!");
        ReflectionTestUtils.setField(shortLived, "expirationMs", -1000L); // уже истёк
        ReflectionTestUtils.invokeMethod(shortLived, "initKey");

        String token = shortLived.generateToken("user@example.com");
        assertThat(shortLived.validateToken(token)).isFalse();
    }

}
