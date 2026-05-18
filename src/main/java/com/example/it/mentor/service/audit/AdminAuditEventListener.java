package com.example.it.mentor.service.audit;

import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminAuditEventListener {

    private final AuditWriter auditWriter;

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
}
