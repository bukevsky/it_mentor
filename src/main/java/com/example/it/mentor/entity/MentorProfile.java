package com.example.it.mentor.entity;

import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.enums.MentoringChannel;
import com.example.it.mentor.entity.enums.MentoringDuration;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mentor_profiles")
public class MentorProfile extends BaseEntity {

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 100)
    private String middleName;

    @Column(length = 255)
    private String position;

    @Column(length = 255)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private DictCity city;

    @Column(length = 30)
    private String phone;

    @Column(name = "max", length = 100)
    private String maxContact;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String expectations;

    @Column(name = "can_help_with", columnDefinition = "TEXT")
    private String canHelpWith;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MentoringType mentoringType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MentoringChannel mentoringChannel;

    @Column(length = 100)
    private String mentoringFrequency;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MentoringDuration mentoringDuration;

    private Integer menteeLimit;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private RecruitmentStatus recruitmentStatus = RecruitmentStatus.OPEN;

    @OneToMany(mappedBy = "mentorProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MentorSkill> skills = new HashSet<>();
}
