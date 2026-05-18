package com.example.it.mentor.service.audit;

import com.example.it.mentor.entity.AdminAuditLog;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.repository.AdminAuditLogRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditWriter")
class AuditWriterTest {

    @InjectMocks private AuditWriter auditWriter;

    @Mock private AdminAuditLogRepository auditLogRepository;
    @Spy private ObjectMapper objectMapper;

    @Test
    @DisplayName("write_roleChanged — сохраняет запись с ROLE_CHANGED")
    void write_roleChanged_persistsEntry() {
        RoleChangedAuditEvent event = new RoleChangedAuditEvent(1L, 2L, RoleCode.STUDENT, RoleCode.MENTOR);

        auditWriter.write(event);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.ROLE_CHANGED);
        assertThat(saved.getAdminUserId()).isEqualTo(1L);
        assertThat(saved.getTargetId()).isEqualTo(2L);
        assertThat(saved.getTargetType()).isEqualTo("USER");
        assertThat(saved.getPayload()).contains("STUDENT");
    }

    @Test
    @DisplayName("write_reviewModerated — сохраняет запись с REVIEW_MODERATED")
    void write_reviewModerated_persistsEntry() {
        ReviewModeratedAuditEvent event = new ReviewModeratedAuditEvent(99L, 300L, ReviewModerationStatus.HIDDEN);

        auditWriter.write(event);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.REVIEW_MODERATED);
        assertThat(saved.getTargetId()).isEqualTo(300L);
        assertThat(saved.getPayload()).contains("HIDDEN");
    }

    @Test
    @DisplayName("write_complaintResolved — сохраняет запись с COMPLAINT_RESOLVED")
    void write_complaintResolved_persistsEntry() {
        ComplaintResolvedAuditEvent event = new ComplaintResolvedAuditEvent(99L, 50L, ComplaintStatus.RESOLVED, "ок");

        auditWriter.write(event);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.COMPLAINT_RESOLVED);
        assertThat(saved.getTargetType()).isEqualTo("COMPLAINT");
    }

    @Test
    @DisplayName("write_repoFailure — исключение репозитория поглощается, не пробрасывается")
    void write_repoFailure_swallowsException() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());
        RoleChangedAuditEvent event = new RoleChangedAuditEvent(1L, 2L, RoleCode.STUDENT, RoleCode.MENTOR);

        assertThatCode(() -> auditWriter.write(event)).doesNotThrowAnyException();
    }
}
