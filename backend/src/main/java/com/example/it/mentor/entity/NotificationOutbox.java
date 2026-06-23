package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox extends BaseEntity {

    @Version
    private Long version;

    @Column(nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String htmlBody;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String textBody;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private OffsetDateTime nextAttemptAt;

    private OffsetDateTime sentAt;
}
