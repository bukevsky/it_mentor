package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.audit.AuditLogResponse;
import com.example.it.mentor.entity.AdminAuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AdminAuditLog auditLog);
}
