package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.MentoringSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mentoring_sessions")
@NamedEntityGraph(
        name = "MentoringSession.withParticipants",
        attributeNodes = {
                @NamedAttributeNode("studentUser"),
                @NamedAttributeNode("mentorUser"),
                @NamedAttributeNode(value = "mentoringRequest", subgraph = "request-profiles")
        },
        subgraphs = {
                @NamedSubgraph(name = "request-profiles", attributeNodes = {
                        @NamedAttributeNode("studentProfile"),
                        @NamedAttributeNode("mentorProfile")
                })
        }
)
public class MentoringSession extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "mentoring_request_id", nullable = false)
    private MentoringRequest mentoringRequest;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "mentor_user_id", nullable = false)
    private User mentorUser;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(STRING)
    @Column(length = 32, nullable = false)
    private MentoringSessionStatus status;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "reschedule_reason", columnDefinition = "TEXT")
    private String rescheduleReason;
}
