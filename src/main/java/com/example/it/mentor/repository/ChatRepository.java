package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий чатов между студентами и менторами.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * Проверяет существование чата для конкретной заявки.
     *
     * @param requestId идентификатор заявки
     * @return {@code true}, если чат уже создан
     */
    boolean existsByMentoringRequestId(Long requestId);

    /**
     * Загружает чат по идентификатору с предзагрузкой связанной заявки.
     *
     * @param chatId идентификатор чата
     * @return найденный чат
     */
    @EntityGraph(attributePaths = "mentoringRequest")
    Optional<Chat> findWithMentoringRequestById(Long chatId);

    /**
     * Ищет чат по идентификатору заявки с предзагрузкой самой заявки.
     *
     * @param requestId идентификатор заявки
     * @return найденный чат
     */
    @EntityGraph(attributePaths = "mentoringRequest")
    Optional<Chat> findByMentoringRequestId(Long requestId);

    /**
     * Возвращает страницу чатов, в которых участвует пользователь.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница чатов пользователя
     */
    @EntityGraph(attributePaths = "mentoringRequest")
    @Query("SELECT c FROM Chat c WHERE c.studentUserId = :userId OR c.mentorUserId = :userId")
    Page<Chat> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
