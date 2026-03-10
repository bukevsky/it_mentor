package com.example.it.mentor.entity.dict;

import com.example.it.mentor.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dict_skill")
public class DictSkill extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
