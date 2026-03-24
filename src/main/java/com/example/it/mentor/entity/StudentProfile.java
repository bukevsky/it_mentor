package com.example.it.mentor.entity;

import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_profiles")
@NamedEntityGraph(
        name = "StudentProfile.withDetails",
        attributeNodes = {
                @NamedAttributeNode("city"),
                @NamedAttributeNode("educations"),
                @NamedAttributeNode(value = "languages", subgraph = "languages-subgraph"),
                @NamedAttributeNode(value = "skills", subgraph = "skills-subgraph"),
                @NamedAttributeNode("employmentTypes"),
                @NamedAttributeNode("workFormats")
        },
        subgraphs = {
                @NamedSubgraph(name = "languages-subgraph", attributeNodes = @NamedAttributeNode("language")),
                @NamedSubgraph(name = "skills-subgraph", attributeNodes = @NamedAttributeNode("skill"))
        }
)
public class StudentProfile extends BaseEntity {

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

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private DictCity city;

    @Column(length = 255)
    private String desiredPosition;

    private Integer hoursPerWeek;

    private LocalDate availableFrom;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(length = 100)
    private String max;

    @ElementCollection
    @CollectionTable(name = "student_employment_types", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "employment_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<EmploymentType> employmentTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "student_work_formats", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "work_format", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<WorkFormat> workFormats = new HashSet<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentEducation> educations = new HashSet<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentLanguage> languages = new HashSet<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentSkill> skills = new HashSet<>();

    @Column(name = "resume_file_id")
    private Long resumeFileId;
}
