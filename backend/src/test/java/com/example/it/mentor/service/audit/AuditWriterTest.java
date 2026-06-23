package com.example.it.mentor.service.audit;

import com.example.it.mentor.entity.AdminAuditLog;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.entity.enums.AuditAction;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.DictionaryOperation;
import com.example.it.mentor.entity.enums.DictionaryType;
import com.example.it.mentor.entity.enums.ReviewModerationStatus;
import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.event.audit.ReviewModeratedAuditEvent;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
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
    @DisplayName("write_userStatusChanged — сохраняет запись с USER_STATUS_CHANGED и targetType USER")
    void write_userStatusChanged_persistsWithTargetTypeUser() {
        UserStatusChangedAuditEvent event = new UserStatusChangedAuditEvent(
                1L, 5L, UserStatus.ACTIVE, UserStatus.BLOCKED);

        auditWriter.write(event);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.USER_STATUS_CHANGED);
        assertThat(saved.getTargetType()).isEqualTo("USER");
        assertThat(saved.getTargetId()).isEqualTo(5L);
        assertThat(saved.getPayload()).contains("BLOCKED");
        assertThat(saved.getPayload()).contains("ACTIVE");
    }

    @Test
    @DisplayName("write_dictionaryChanged — сохраняет запись с prefixed targetType DICTIONARY_CITY")
    void write_dictionaryChanged_persistsWithTargetTypePrefix() {
        DictionaryChangedAuditEvent event = new DictionaryChangedAuditEvent(
                1L, DictionaryType.CITY, 7L, DictionaryOperation.CREATE, "Калининград");

        auditWriter.write(event);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.DICTIONARY_CHANGED);
        assertThat(saved.getTargetType()).isEqualTo("DICTIONARY_CITY");
        assertThat(saved.getTargetId()).isEqualTo(7L);
        assertThat(saved.getPayload()).contains("Калининград");
        assertThat(saved.getPayload()).contains("CREATE");
    }

    @Test
    @DisplayName("write_repoFailure — исключение репозитория поглощается, не пробрасывается")
    void write_repoFailure_swallowsException() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());
        RoleChangedAuditEvent event = new RoleChangedAuditEvent(1L, 2L, RoleCode.STUDENT, RoleCode.MENTOR);

        assertThatCode(() -> auditWriter.write(event)).doesNotThrowAnyException();
    }
}
