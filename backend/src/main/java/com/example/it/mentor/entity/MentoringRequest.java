package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.MentoringRequestDirection;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.entity.enums.MentoringType;
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
@Table(name = "mentoring_requests")
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "MentoringRequest.withProfiles",
                attributeNodes = {
                        @NamedAttributeNode("studentProfile"),
                        @NamedAttributeNode("mentorProfile")
                }
        ),
        @NamedEntityGraph(
                name = "MentoringRequest.withProfilesAndUsers",
                attributeNodes = {
                        @NamedAttributeNode(value = "studentProfile", subgraph = "student-profile-user"),
                        @NamedAttributeNode(value = "mentorProfile", subgraph = "mentor-profile-user")
                },
                subgraphs = {
                        @NamedSubgraph(name = "student-profile-user", attributeNodes = @NamedAttributeNode("user")),
                        @NamedSubgraph(name = "mentor-profile-user", attributeNodes = @NamedAttributeNode("user"))
                }
        )
})
public class MentoringRequest extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "mentor_profile_id", nullable = false)
    private MentorProfile mentorProfile;

    @Enumerated(STRING)
    @Column(length = 30, nullable = false)
    private MentoringRequestDirection direction;

    @Enumerated(STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private MentoringRequestStatus status = MentoringRequestStatus.SENT;

    @Enumerated(STRING)
    @Column(name = "goal_type", length = 30, nullable = false)
    private MentoringType goalType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "clarification_note", columnDefinition = "TEXT")
    private String clarificationNote;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column
    private OffsetDateTime respondedAt;

    @Column
    private OffsetDateTime completedAt;
}
