package com.example.it.mentor.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreferences extends BaseEntity {

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailRequestEvents = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailSessionEvents = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailReviewEvents = true;
}
