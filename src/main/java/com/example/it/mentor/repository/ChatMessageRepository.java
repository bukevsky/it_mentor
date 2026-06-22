package com.example.it.mentor.repository;

import com.example.it.mentor.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Репозиторий сообщений чатов.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findBySenderUserIdAndClientMessageId(Long senderUserId, UUID clientMessageId);

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
    List<ChatMessage> findByChatIdAndDeletedFalseAndIdGreaterThanOrderByIdAsc(
            Long chatId, Long afterId, Pageable pageable);

    @EntityGraph(attributePaths = {"attachment"})
    @Query("SELECT m FROM ChatMessage m WHERE m.deleted = false " +
           "AND m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 " +
           "WHERE m2.deleted = false AND m2.chat.id IN :chatIds GROUP BY m2.chat.id)")
    List<ChatMessage> findLastMessagesByChatIds(@Param("chatIds") List<Long> chatIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatMessage m SET m.deliveryStatus = com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.DELIVERED, " +
           "m.deliveredAt = :changedAt WHERE m.chat.id = :chatId AND m.senderUserId <> :recipientUserId " +
           "AND m.id <= :upToMessageId AND m.deliveryStatus = com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.SENT")
    int markDelivered(@Param("chatId") Long chatId,
                      @Param("recipientUserId") Long recipientUserId,
                      @Param("upToMessageId") Long upToMessageId,
                      @Param("changedAt") OffsetDateTime changedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatMessage m SET m.deliveryStatus = com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.READ, " +
           "m.deliveredAt = COALESCE(m.deliveredAt, :changedAt), m.readAt = :changedAt " +
           "WHERE m.chat.id = :chatId AND m.senderUserId <> :recipientUserId " +
           "AND m.id <= :upToMessageId AND m.deliveryStatus <> com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.READ")
    int markRead(@Param("chatId") Long chatId,
                 @Param("recipientUserId") Long recipientUserId,
                 @Param("upToMessageId") Long upToMessageId,
                 @Param("changedAt") OffsetDateTime changedAt);

    @Query("SELECT m.chat.id, COUNT(m) FROM ChatMessage m " +
           "WHERE (m.chat.studentUserId = :userId OR m.chat.mentorUserId = :userId) " +
           "AND m.senderUserId <> :userId " +
           "AND m.deliveryStatus <> com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.READ " +
           "AND m.deleted = false GROUP BY m.chat.id")
    List<Object[]> countUnreadPerChatRaw(@Param("userId") Long userId);

    default Map<Long, Long> countUnreadPerChat(Long userId) {
        return countUnreadPerChatRaw(userId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chat.id = :chatId " +
           "AND m.senderUserId <> :userId " +
           "AND m.deliveryStatus <> com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus.READ " +
           "AND m.deleted = false")
    long countUnreadForChat(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
