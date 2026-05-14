package com.example.it.mentor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chats")
public class Chat extends BaseEntity {

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "mentoring_request_id", nullable = false, unique = true)
    private MentoringRequest mentoringRequest;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "mentor_user_id", nullable = false)
    private Long mentorUserId;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "last_sender_user_id")
    private Long lastSenderUserId;

    @OneToMany(mappedBy = "chat", fetch = LAZY)
    @Builder.Default
    private Set<ChatMessage> messages = new HashSet<>();
}
