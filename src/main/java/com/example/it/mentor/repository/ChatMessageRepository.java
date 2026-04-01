package com.example.it.mentor.repository;

import com.example.it.mentor.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"attachment"})
    Page<ChatMessage> findByChatIdAndDeletedFalseOrderByCreatedAtDesc(Long chatId, Pageable pageable);
}
