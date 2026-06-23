package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Complaint;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    @Query("""
            SELECT c FROM Complaint c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:targetType IS NULL OR c.targetType = :targetType)
            """)
    Page<Complaint> findFiltered(
            @Param("status") ComplaintStatus status,
            @Param("targetType") ComplaintTargetType targetType,
            Pageable pageable);
}
