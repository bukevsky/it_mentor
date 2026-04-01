package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByMentoringRequestId(Long requestId);

    @Query("SELECT c FROM Chat c WHERE c.studentUserId = :userId OR c.mentorUserId = :userId")
    Page<Chat> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
