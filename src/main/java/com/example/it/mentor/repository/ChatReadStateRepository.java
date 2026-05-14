package com.example.it.mentor.repository;

import com.example.it.mentor.entity.ChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ChatReadStateRepository extends JpaRepository<ChatReadState, Long> {

    Optional<ChatReadState> findByChatIdAndUserId(Long chatId, Long userId);

    @Query("SELECT m.chat.id, COUNT(m) FROM ChatMessage m " +
           "LEFT JOIN ChatReadState rs ON rs.chatId = m.chat.id AND rs.userId = :userId " +
           "WHERE (m.chat.studentUserId = :userId OR m.chat.mentorUserId = :userId) " +
           "AND m.senderUserId <> :userId " +
           "AND (rs IS NULL OR m.createdAt > rs.lastReadAt) " +
           "AND m.deleted = false " +
           "GROUP BY m.chat.id")
    List<Object[]> countUnreadPerChatRaw(@Param("userId") Long userId);

    default Map<Long, Long> countUnreadPerChat(Long userId) {
        return countUnreadPerChatRaw(userId).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "LEFT JOIN ChatReadState rs ON rs.chatId = m.chat.id AND rs.userId = :userId " +
           "WHERE m.chat.id = :chatId " +
           "AND m.senderUserId <> :userId " +
           "AND (rs IS NULL OR m.createdAt > rs.lastReadAt) " +
           "AND m.deleted = false")
    long countUnreadForChat(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
