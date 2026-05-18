package com.example.it.mentor.repository;

import com.example.it.mentor.entity.AdminAuditLog;
import com.example.it.mentor.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:adminUserId IS NULL OR a.adminUserId = :adminUserId)
              AND a.createdAt >= COALESCE(:from, a.createdAt)
              AND a.createdAt <= COALESCE(:to, a.createdAt)
            """)
    Page<AdminAuditLog> findFiltered(
            @Param("action") AuditAction action,
            @Param("adminUserId") Long adminUserId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
