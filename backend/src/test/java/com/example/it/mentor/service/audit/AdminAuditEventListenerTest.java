package com.example.it.mentor.service.audit;

import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.DictionaryOperation;
import com.example.it.mentor.entity.enums.DictionaryType;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditEventListener")
class AdminAuditEventListenerTest {

    @InjectMocks private AdminAuditEventListener listener;

    @Mock private AuditWriter auditWriter;

    @Test
    @DisplayName("onRoleChanged — делегирует в AuditWriter")
    void onRoleChanged_delegatesToWriter() {
        RoleChangedAuditEvent event = new RoleChangedAuditEvent(1L, 2L, RoleCode.STUDENT, RoleCode.MENTOR);
        listener.onRoleChanged(event);
        verify(auditWriter).write(event);
    }

    @Test
    @DisplayName("onReviewModerated — делегирует в AuditWriter")
    void onReviewModerated_delegatesToWriter() {
        ReviewModeratedAuditEvent event = new ReviewModeratedAuditEvent(1L, 100L, ReviewModerationStatus.HIDDEN);
        listener.onReviewModerated(event);
        verify(auditWriter).write(event);
    }

    @Test
    @DisplayName("onComplaintResolved — делегирует в AuditWriter")
    void onComplaintResolved_delegatesToWriter() {
        ComplaintResolvedAuditEvent event = new ComplaintResolvedAuditEvent(1L, 50L, ComplaintStatus.RESOLVED, null);
        listener.onComplaintResolved(event);
        verify(auditWriter).write(event);
    }

    @Test
    @DisplayName("onUserStatusChanged — делегирует в AuditWriter")
    void onUserStatusChanged_delegatesToAuditWriter() {
        UserStatusChangedAuditEvent event = new UserStatusChangedAuditEvent(
                1L, 5L, UserStatus.ACTIVE, UserStatus.BLOCKED);
        listener.onUserStatusChanged(event);
        verify(auditWriter).write(event);
    }

    @Test
    @DisplayName("onDictionaryChanged — делегирует в AuditWriter")
    void onDictionaryChanged_delegatesToAuditWriter() {
        DictionaryChangedAuditEvent event = new DictionaryChangedAuditEvent(
                1L, DictionaryType.CITY, 7L, DictionaryOperation.CREATE, "Москва");
        listener.onDictionaryChanged(event);
        verify(auditWriter).write(event);
    }
}
