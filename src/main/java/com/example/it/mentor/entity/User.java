package com.example.it.mentor.entity;

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
@Table(name = "users")
public class User extends BaseEntity {

    @Version
    private Long version;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "avatar_file_id")
    private Long avatarFileId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(RoleCode code) {
        return roles.stream().anyMatch(role -> role.getCode() == code);
    }

    public RoleCode primaryRole() {
        if (hasRole(RoleCode.MENTOR)) {
            return RoleCode.MENTOR;
        }
        if (hasRole(RoleCode.ADMIN)) {
            return RoleCode.ADMIN;
        }
        return RoleCode.STUDENT;
    }
}
