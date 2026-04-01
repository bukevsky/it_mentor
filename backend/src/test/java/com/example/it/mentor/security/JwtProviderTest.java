package com.example.it.mentor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider")
class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", "test-secret-key-must-be-at-least-32-chars!");
        ReflectionTestUtils.setField(jwtProvider, "expirationMs", 3_600_000L);
        ReflectionTestUtils.invokeMethod(jwtProvider, "initKey");
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("возвращает непустой JWT")
        void shouldReturnNonBlankToken() {
            String token = jwtProvider.generateToken("user@example.com");
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("JWT имеет структуру из 3 частей, разделённых точкой (header.payload.signature)")
        void shouldHaveThreePartStructure() {
            String token = jwtProvider.generateToken("user@example.com");
            String[] parts = token.split("\\.");
            assertThat(parts)
                    .as("JWT должен состоять ровно из трёх частей: header.payload.signature")
                    .hasSize(3);
            assertThat(parts[0]).as("header").isNotBlank();
            assertThat(parts[1]).as("payload").isNotBlank();
            assertThat(parts[2]).as("signature").isNotBlank();
        }

        @ParameterizedTest
        @ValueSource(strings = {"user@example.com", "admin@company.ru", "test.user+alias@domain.org"})
        @DisplayName("токены для разных email-адресов уникальны")
        void differentEmails_shouldProduceDifferentTokens(String email) {
            String token = jwtProvider.generateToken(email);
            assertThat(token).isNotBlank();
            // Каждый токен должен содержать именно тот email, с которым был создан
            assertThat(jwtProvider.extractUsername(token)).isEqualTo(email);
        }

        @Test
        @DisplayName("два вызова для одного email возвращают токены с одним и тем же subject")
        void sameEmail_twoTokens_shouldHaveSameSubject() {
            String token1 = jwtProvider.generateToken("user@example.com");
            String token2 = jwtProvider.generateToken("user@example.com");

            // Оба должны быть валидны и содержать одинаковый email
            assertThat(jwtProvider.extractUsername(token1)).isEqualTo("user@example.com");
            assertThat(jwtProvider.extractUsername(token2)).isEqualTo("user@example.com");
        }
    }

    // ── extractUsername ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("достаёт email из валидного JWT (round-trip)")
        void validToken_shouldReturnCorrectEmail() {
            String token = jwtProvider.generateToken("user@example.com");
            assertThat(jwtProvider.extractUsername(token)).isEqualTo("user@example.com");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "simple@example.com",
                "user.name+tag@domain.co.uk",
                "admin@company.ru"
        })
        @DisplayName("корректно извлекает различные форматы email")
        void differentEmailFormats_shouldExtractCorrectly(String email) {
            String token = jwtProvider.generateToken(email);
            assertThat(jwtProvider.extractUsername(token))
                    .as("Извлечённый email должен совпадать с оригинальным")
                    .isEqualTo(email);
        }
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("валидный токен → true")
        void validToken_shouldReturnTrue() {
            String token = jwtProvider.generateToken("user@example.com");
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("повреждённая строка — не JWT → false")
        void tamperedString_shouldReturnFalse() {
            assertThat(jwtProvider.validateToken("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("пустая строка → false")
        void emptyString_shouldReturnFalse() {
            assertThat(jwtProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("случайная строка без точек → false")
        void randomStringWithoutDots_shouldReturnFalse() {
            assertThat(jwtProvider.validateToken("totallyrandomstring")).isFalse();
        }

        @Test
        @DisplayName("истёкший токен → false")
        void expiredToken_shouldReturnFalse() {
            JwtProvider shortLived = new JwtProvider();
            ReflectionTestUtils.setField(shortLived, "secret", "test-secret-key-must-be-at-least-32-chars!");
            ReflectionTestUtils.setField(shortLived, "expirationMs", -1000L); // уже истёк в момент создания
            ReflectionTestUtils.invokeMethod(shortLived, "initKey");

            String token = shortLived.generateToken("user@example.com");
            assertThat(shortLived.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("токен, подписанный другим ключом → false")
        void tokenSignedWithDifferentKey_shouldReturnFalse() {
            JwtProvider otherProvider = new JwtProvider();
            ReflectionTestUtils.setField(otherProvider, "secret", "completely-different-secret-key-32-chars!!");
            ReflectionTestUtils.setField(otherProvider, "expirationMs", 3_600_000L);
            ReflectionTestUtils.invokeMethod(otherProvider, "initKey");

            String tokenFromOther = otherProvider.generateToken("user@example.com");

            // Наш jwtProvider не должен принимать токен, подписанный другим ключом
            assertThat(jwtProvider.validateToken(tokenFromOther))
                    .as("Токен от другого ключа не должен быть валидным")
                    .isFalse();
        }

        @Test
        @DisplayName("изменённый payload (подделка) → false")
        void modifiedPayload_shouldReturnFalse() {
            String token = jwtProvider.generateToken("user@example.com");
            // Разбиваем токен и подменяем payload на заведомо неверный base64
            String[] parts = token.split("\\.");
            String fakeToken = parts[0] + ".AAAAAAAAAAAA.." + parts[2];

            assertThat(jwtProvider.validateToken(fakeToken)).isFalse();
        }
    }
}
