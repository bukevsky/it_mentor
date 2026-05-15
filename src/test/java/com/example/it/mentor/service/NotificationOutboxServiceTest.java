package com.example.it.mentor.service;

import com.example.it.mentor.entity.NotificationOutbox;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import com.example.it.mentor.repository.NotificationOutboxRepository;
import com.example.it.mentor.service.notification.NotificationOutboxEntryProcessor;
import com.example.it.mentor.service.notification.NotificationOutboxService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxService")
class NotificationOutboxServiceTest {

    @InjectMocks private NotificationOutboxService service;

    @Mock private NotificationOutboxRepository outboxRepository;
    @Mock private NotificationOutboxEntryProcessor entryProcessor;

    @Nested
    @DisplayName("enqueue")
    class Enqueue {

        @Test
        void enqueue_savesPendingEntry() {
            EmailMessage msg = new EmailMessage("user@example.com", "Тема", "<p>html</p>", "text");

            service.enqueue(msg, "request.created");

            ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
            verify(outboxRepository).save(captor.capture());
            NotificationOutbox saved = captor.getValue();
            assertThat(saved.getRecipientEmail()).isEqualTo("user@example.com");
            assertThat(saved.getSubject()).isEqualTo("Тема");
            assertThat(saved.getEventType()).isEqualTo("request.created");
            assertThat(saved.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
            assertThat(saved.getAttempts()).isZero();
        }
    }

    @Nested
    @DisplayName("processBatch")
    class ProcessBatch {

        @Test
        void processBatch_emptyBatch_noSideEffects() {
            when(outboxRepository.findTop50ByStatusAndNextAttemptAtBefore(any(), any()))
                    .thenReturn(List.of());

            service.processBatch();

            verifyNoInteractions(entryProcessor);
        }

        @Test
        void processBatch_callsProcessorForEachPendingEntry() {
            NotificationOutbox e1 = pendingEntry(1L);
            NotificationOutbox e2 = pendingEntry(2L);
            when(outboxRepository.findTop50ByStatusAndNextAttemptAtBefore(any(), any()))
                    .thenReturn(List.of(e1, e2));

            service.processBatch();

            verify(entryProcessor).process(1L);
            verify(entryProcessor).process(2L);
            verifyNoMoreInteractions(entryProcessor);
        }
    }

    private NotificationOutbox pendingEntry(long id) {
        NotificationOutbox entry = NotificationOutbox.builder()
                .recipientEmail("user@example.com")
                .subject("Тема")
                .htmlBody("<p>html</p>")
                .textBody("text")
                .eventType("request.created")
                .status(NotificationOutboxStatus.PENDING)
                .nextAttemptAt(OffsetDateTime.now().minusSeconds(1))
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
