package com.example.it.mentor.entity;

import com.example.it.mentor.entity.dict.DictSkill;
import com.example.it.mentor.entity.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mentor_skills")
public class MentorSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private MentorProfile mentorProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private DictSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SkillLevel level;
}
