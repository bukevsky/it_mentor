package com.example.it.mentor.service.audit;

import com.example.it.mentor.entity.AdminAuditLog;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.event.audit.AuditEvent;
import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriter {

    private final AdminAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent event) {
        try {
            AdminAuditLog entry = toEntry(event);
            entry = auditLogRepository.save(entry);
            log.info("Audit-лог записан: auditLogId={}, action={}, targetType={}, targetId={}, adminId={}, step={}",
                    entry.getId(), entry.getAction(), entry.getTargetType(), entry.getTargetId(), entry.getAdminUserId(),
                    "audit_log_written");
        } catch (Exception e) {
            log.error("Ошибка записи аудит-лога: event={}, error={}, step={}",
                    event.getClass().getSimpleName(), e.getMessage(), "audit_log_write_failed", e);
        }
    }

    private AdminAuditLog toEntry(AuditEvent event) {
        return switch (event) {
            case RoleChangedAuditEvent e -> AdminAuditLog.builder()
                    .adminUserId(e.adminUserId())
                    .action(AuditAction.ROLE_CHANGED)
                    .targetType("USER")
                    .targetId(e.targetUserId())
                    .payload(serialize(e))
                    .build();
            case ReviewModeratedAuditEvent e -> AdminAuditLog.builder()
                    .adminUserId(e.adminUserId())
                    .action(AuditAction.REVIEW_MODERATED)
                    .targetType("REVIEW")
                    .targetId(e.reviewId())
                    .payload(serialize(e))
                    .build();
            case ComplaintResolvedAuditEvent e -> AdminAuditLog.builder()
                    .adminUserId(e.adminUserId())
                    .action(AuditAction.COMPLAINT_RESOLVED)
                    .targetType("COMPLAINT")
                    .targetId(e.complaintId())
                    .payload(serialize(e))
                    .build();
            case UserStatusChangedAuditEvent e -> AdminAuditLog.builder()
                    .adminUserId(e.adminUserId())
                    .action(AuditAction.USER_STATUS_CHANGED)
                    .targetType("USER")
                    .targetId(e.targetUserId())
                    .payload(serialize(e))
                    .build();
            case DictionaryChangedAuditEvent e -> AdminAuditLog.builder()
                    .adminUserId(e.adminUserId())
                    .action(AuditAction.DICTIONARY_CHANGED)
                    .targetType("DICTIONARY_" + e.dictionaryType().name())
                    .targetId(e.entryId())
                    .payload(serialize(e))
                    .build();
        };
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            log.warn("Не удалось сериализовать payload: error={}, step={}",
                    e.getMessage(), "audit_payload_serialization_failed");
            return null;
        }
    }
}
