package com.example.it.mentor.repository;

import com.example.it.mentor.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Атомарно помечает токен как использованный, если он ещё не использован и не истёк.
     * Возвращает количество обновлённых строк (0 = токен недействителен/уже использован).
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token AND t.used = false AND t.expiresAt > :now")
    int markTokenUsed(String token, OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(OffsetDateTime now);
}
