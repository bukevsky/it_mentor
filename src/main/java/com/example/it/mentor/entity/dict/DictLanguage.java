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
@Table(name = "dict_language")
public class DictLanguage extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
