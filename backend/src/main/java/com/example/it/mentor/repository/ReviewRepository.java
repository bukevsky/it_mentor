package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Review;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMentoringRequestId(Long mentoringRequestId);

    @EntityGraph(attributePaths = {"mentoringRequest", "reviewer"})
    Optional<Review> findByMentoringRequestId(Long mentoringRequestId);

    @EntityGraph(attributePaths = {"mentoringRequest", "reviewer"})
    @Query("SELECT r FROM Review r WHERE r.mentorUserId = :mentorUserId AND r.moderationStatus = :status")
    Page<Review> findByMentorUserId(@Param("mentorUserId") Long mentorUserId,
                                    @Param("status") ReviewModerationStatus status,
                                    Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mentorUserId = :userId AND r.moderationStatus = com.example.it.mentor.entity.enums.ReviewModerationStatus.VISIBLE")
    Optional<Double> averageRatingByMentorUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.mentorUserId = :userId AND r.moderationStatus = com.example.it.mentor.entity.enums.ReviewModerationStatus.VISIBLE")
    long countByMentorUserId(@Param("userId") Long userId);
}
