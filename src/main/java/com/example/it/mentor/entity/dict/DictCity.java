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
@Table(name = "dict_city")
public class DictCity extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String region;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
