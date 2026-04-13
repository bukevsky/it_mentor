package com.example.it.mentor.repository;

import com.example.it.mentor.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Репозиторий одноразовых токенов для сброса пароля.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Ищет токен по пользователю и его строковому значению.
     *
     * @param userId идентификатор пользователя
     * @param token значение токена
     * @return найденный токен
     */
    Optional<PasswordResetToken> findByUserIdAndToken(Long userId, String token);

    /**
     * Помечает использованными все активные токены конкретного пользователя.
     *
     * @param userId идентификатор пользователя
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateAllByUserId(Long userId);

    /**
     * Атомарно помечает токен как использованный, если он не использован, не истёк и не превышен лимит попыток.
     * Возвращает количество обновлённых строк (0 = токен недействителен).
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.used = true
            WHERE t.user.id = :userId AND t.token = :token
              AND t.used = false AND t.expiresAt > :now AND t.attempts < :maxAttempts
            """)
    int markTokenUsed(Long userId, String token, OffsetDateTime now, int maxAttempts);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.attempts = t.attempts + 1
            WHERE t.user.id = :userId AND t.token = :token AND t.used = false
            """)
    void incrementAttempts(Long userId, String token);

    /**
     * Удаляет все истёкшие токены.
     *
     * @param now текущий момент времени
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(OffsetDateTime now);
}
