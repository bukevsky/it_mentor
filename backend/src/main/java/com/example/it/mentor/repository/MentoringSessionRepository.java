package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentoringSession;
import com.example.it.mentor.entity.enums.MentoringSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий сессий менторинга.
 */
@Repository
public interface MentoringSessionRepository extends JpaRepository<MentoringSession, Long> {

    /**
     * Возвращает страницу сессий, в которых участвует пользователь как студент или ментор.
     *
     * @param studentUserId идентификатор пользователя-студента
     * @param mentorUserId идентификатор пользователя-ментора
     * @param pageable параметры пагинации
     * @return страница сессий
     */
    @EntityGraph(value = "MentoringSession.withParticipants")
    Page<MentoringSession> findByStudentUserIdOrMentorUserId(Long studentUserId, Long mentorUserId, Pageable pageable);

    /**
     * Подгружает сессию с участниками для проверки доступа.
     *
     * @param id идентификатор сессии
     * @return найденная сессия
     */
    @EntityGraph(value = "MentoringSession.withParticipants")
    Optional<MentoringSession> findWithParticipantsById(Long id);

    /**
     * Возвращает сессии ментора в заданном окне времени и определённых статусах.
     *
     * @param mentorUserId идентификатор пользователя-ментора
     * @param statuses набор статусов
     * @param from начало окна
     * @param to конец окна
     * @return список пересекающихся сессий
     */
    List<MentoringSession> findByMentorUserIdAndStatusInAndScheduledAtBetween(
            Long mentorUserId, Collection<MentoringSessionStatus> statuses, OffsetDateTime from, OffsetDateTime to);

    /**
     * Возвращает сессии студента в заданном окне времени и определённых статусах.
     *
     * @param studentUserId идентификатор пользователя-студента
     * @param statuses набор статусов
     * @param from начало окна
     * @param to конец окна
     * @return список пересекающихся сессий
     */
    List<MentoringSession> findByStudentUserIdAndStatusInAndScheduledAtBetween(
            Long studentUserId, Collection<MentoringSessionStatus> statuses, OffsetDateTime from, OffsetDateTime to);

    /**
     * Возвращает ближайшую запланированную сессию для пользователя.
     *
     * @param userId идентификатор пользователя
     * @param now текущий момент времени
     * @return ближайшая SCHEDULED или RESCHEDULED сессия
     */
    @Query("""
            SELECT s FROM MentoringSession s
            WHERE (s.studentUser.id = :userId OR s.mentorUser.id = :userId)
              AND s.status IN (com.example.it.mentor.entity.enums.MentoringSessionStatus.SCHEDULED,
                               com.example.it.mentor.entity.enums.MentoringSessionStatus.RESCHEDULED)
              AND s.scheduledAt >= :now
            ORDER BY s.scheduledAt ASC
            """)
    List<MentoringSession> findNextForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now, Pageable pageable);
}
