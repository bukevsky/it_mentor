package com.example.it.mentor.repository;

import com.example.it.mentor.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @EntityGraph(attributePaths = {"attachment"})
    java.util.Optional<ChatMessage> findFirstByChatIdAndDeletedFalseOrderByCreatedAtDesc(Long chatId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ChatMessage m " +
           "WHERE m.attachment.id = :fileId " +
           "AND (m.chat.studentUserId = :userId OR m.chat.mentorUserId = :userId)")
    boolean existsByAttachmentIdAndChatParticipant(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"attachment"})
    Page<ChatMessage> findByChatIdAndDeletedFalseAndIdLessThanOrderByIdDesc(Long chatId, Long beforeId, Pageable pageable);

    @EntityGraph(attributePaths = {"attachment"})
    @Query("SELECT m FROM ChatMessage m WHERE m.deleted = false " +
           "AND m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 " +
           "WHERE m2.deleted = false AND m2.chat.id IN :chatIds GROUP BY m2.chat.id)")
    List<ChatMessage> findLastMessagesByChatIds(@Param("chatIds") List<Long> chatIds);
}
