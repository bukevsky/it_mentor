package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.EducationDegree;
import com.example.it.mentor.entity.enums.EducationForm;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_educations")
public class StudentEducation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false, length = 255)
    private String institution;

    @Column(length = 255)
    private String specialty;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private EducationDegree degree;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EducationForm educationForm;

    private Integer startYear;

    private Integer graduationYear;
}
