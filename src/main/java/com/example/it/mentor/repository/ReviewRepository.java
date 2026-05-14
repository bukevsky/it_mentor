package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Репозиторий отзывов о менторах.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Проверяет, существует ли отзыв для указанной заявки.
     *
     * @param mentoringRequestId идентификатор заявки
     * @return {@code true}, если отзыв уже создан
     */
    boolean existsByMentoringRequestId(Long mentoringRequestId);

    /**
     * Ищет отзыв по заявке с предзагрузкой заявки и автора.
     *
     * @param mentoringRequestId идентификатор заявки
     * @return найденный отзыв
     */
    @EntityGraph(attributePaths = {"mentoringRequest", "reviewer"})
    Optional<Review> findByMentoringRequestId(Long mentoringRequestId);

    /**
     * Возвращает страницу отзывов по пользователю-ментору.
     *
     * @param mentorUserId идентификатор пользователя-ментора
     * @param pageable параметры пагинации
     * @return страница отзывов
     */
    @EntityGraph(attributePaths = {"mentoringRequest", "reviewer"})
    Page<Review> findByMentorUserId(Long mentorUserId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mentorUserId = :userId")
    Optional<Double> averageRatingByMentorUserId(@Param("userId") Long userId);

    long countByMentorUserId(Long mentorUserId);
}
