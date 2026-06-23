package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.audit.AuditLogResponse;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.mapper.AuditLogMapper;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin: Audit", description = "Аудит-лог административных действий")
public class AdminAuditController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "action");

    private final AdminAuditLogRepository auditLogRepository;
    private final AuditLogMapper mapper;

    @GetMapping
    public PagedResponse<AuditLogResponse> getAuditLog(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] parts = sort.split(",");
        String field = ALLOWED_SORT_FIELDS.contains(parts[0]) ? parts[0] : "createdAt";
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PagedResponse.from(auditLogRepository.findFiltered(
                action, adminUserId, from, to,
                PageRequest.of(page, size, Sort.by(dir, field))
        ).map(mapper::toResponse));
    }
}
