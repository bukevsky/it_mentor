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
     * Помечает использованными все активные токены конкретного пользователя.
     *
     * @param userId идентификатор пользователя
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateAllByUserId(Long userId);

    /**
     * Возвращает самый свежий активный токен пользователя для сравнения хеша в Java.
     * Активный — не использован, не истёк и не достиг лимита попыток.
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.user.id = :userId
              AND t.used = false
              AND t.expiresAt > :now
              AND t.attempts < :maxAttempts
            ORDER BY t.createdAt DESC
            LIMIT 1
            """)
    Optional<PasswordResetToken> findActiveByUserId(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("now") OffsetDateTime now,
            @org.springframework.data.repository.query.Param("maxAttempts") int maxAttempts);

    /**
     * Удаляет все истёкшие токены.
     *
     * @param now текущий момент времени
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(OffsetDateTime now);
}
