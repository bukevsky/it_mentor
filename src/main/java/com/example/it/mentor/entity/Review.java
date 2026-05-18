package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoring_request_id", nullable = false, unique = true)
    private MentoringRequest mentoringRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(name = "mentor_user_id", nullable = false)
    private Long mentorUserId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.VISIBLE;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderated_at")
    private OffsetDateTime moderatedAt;
}
