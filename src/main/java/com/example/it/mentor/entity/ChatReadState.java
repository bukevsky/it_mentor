package com.example.it.mentor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_read_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}))
public class ChatReadState extends BaseEntity {

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at", nullable = false)
    private OffsetDateTime lastReadAt;
}
