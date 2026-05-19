package com.example.it.mentor.service.audit;

import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminAuditEventListener {

    private final AuditWriter auditWriter;

    // Sync by design: audit must be durable before HTTP response returns.
    // If audit write fails, AuditWriter swallows the exception — business operation is already committed.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(RoleChangedAuditEvent event) {
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewModerated(ReviewModeratedAuditEvent event) {
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintResolved(ComplaintResolvedAuditEvent event) {
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserStatusChanged(UserStatusChangedAuditEvent event) {
        auditWriter.write(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDictionaryChanged(DictionaryChangedAuditEvent event) {
        auditWriter.write(event);
    }
}
