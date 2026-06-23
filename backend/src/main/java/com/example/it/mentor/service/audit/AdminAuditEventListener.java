package com.example.it.mentor.service.audit;

import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuditEventListener {

    private final AuditWriter auditWriter;

    // Sync by design: audit must be durable before HTTP response returns.
    // If audit write fails, AuditWriter swallows the exception — business operation is already committed.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(RoleChangedAuditEvent event) {
        log.debug("Audit-событие роли принято: adminId={}, targetUserId={}, oldRole={}, newRole={}, step={}",
                event.adminUserId(), event.targetUserId(), event.oldRole(), event.newRole(), "audit_role_event_received");
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewModerated(ReviewModeratedAuditEvent event) {
        log.debug("Audit-событие модерации отзыва принято: adminId={}, reviewId={}, moderationStatus={}, step={}",
                event.adminUserId(), event.reviewId(), event.newStatus(), "audit_review_event_received");
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintResolved(ComplaintResolvedAuditEvent event) {
        log.debug("Audit-событие жалобы принято: adminId={}, complaintId={}, status={}, step={}",
                event.adminUserId(), event.complaintId(), event.status(), "audit_complaint_event_received");
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserStatusChanged(UserStatusChangedAuditEvent event) {
        log.debug("Audit-событие статуса пользователя принято: adminId={}, targetUserId={}, oldStatus={}, newStatus={}, step={}",
                event.adminUserId(), event.targetUserId(), event.oldStatus(), event.newStatus(),
                "audit_user_status_event_received");
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDictionaryChanged(DictionaryChangedAuditEvent event) {
        log.debug("Audit-событие справочника принято: adminId={}, dictionaryType={}, entryId={}, operation={}, step={}",
                event.adminUserId(), event.dictionaryType(), event.entryId(), event.operation(),
                "audit_dictionary_event_received");
        auditWriter.write(event);
    }
}
