package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog extends BaseEntity {

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;
}
