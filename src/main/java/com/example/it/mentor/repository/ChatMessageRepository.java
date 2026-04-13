package com.example.it.mentor.repository;

import com.example.it.mentor.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий сообщений чатов.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Возвращает страницу не удалённых сообщений чата с предзагрузкой вложения.
     *
     * @param chatId идентификатор чата
     * @param pageable параметры пагинации
     * @return страница сообщений
     */
    @EntityGraph(attributePaths = {"attachment"})
    Page<ChatMessage> findByChatIdAndDeletedFalseOrderByCreatedAtDesc(Long chatId, Pageable pageable);
}
