package com.example.it.mentor.entity;

import com.example.it.mentor.entity.dict.DictLanguage;
import com.example.it.mentor.entity.enums.LanguageLevel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_languages")
public class StudentLanguage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private DictLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private LanguageLevel level;

    @Builder.Default
    @Column(nullable = false)
    private int position = 0;
}
