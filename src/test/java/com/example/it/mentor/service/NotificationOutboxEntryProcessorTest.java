package com.example.it.mentor.service;

import com.example.it.mentor.config.NotificationProperties;
import com.example.it.mentor.entity.NotificationOutbox;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import com.example.it.mentor.repository.NotificationOutboxRepository;
import com.example.it.mentor.service.notification.NotificationOutboxEntryProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxEntryProcessor")
class NotificationOutboxEntryProcessorTest {

    @InjectMocks private NotificationOutboxEntryProcessor processor;

    @Mock private NotificationOutboxRepository outboxRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationProperties notificationProperties;

    private NotificationProperties.Outbox outboxProps;

    @BeforeEach
    void setUp() {
        outboxProps = new NotificationProperties.Outbox(5, 60, "0 */1 * * * *");
        lenient().when(notificationProperties.outbox()).thenReturn(outboxProps);
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        void process_entryNotFound_noop() {
            when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

            processor.process(1L);

            verify(emailService, never()).send(any());
        }

        @Test
        void process_entryAlreadySent_noop() {
            NotificationOutbox entry = sentEntry();
            when(outboxRepository.findById(1L)).thenReturn(Optional.of(entry));

            processor.process(1L);

            verify(emailService, never()).send(any());
        }

        @Test
        void process_successfulSend_setsSent() {
            NotificationOutbox entry = pendingEntry();
            when(outboxRepository.findById(1L)).thenReturn(Optional.of(entry));

            processor.process(1L);

            assertThat(entry.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
            assertThat(entry.getSentAt()).isNotNull();
            verify(emailService).send(any());
            verify(outboxRepository).save(entry);
        }

        @Test
        void process_smtpFailure_incrementsAttemptsAndSchedulesRetry() {
            NotificationOutbox entry = pendingEntry();
            when(outboxRepository.findById(1L)).thenReturn(Optional.of(entry));
            doThrow(new RuntimeException("SMTP error")).when(emailService).send(any());

            processor.process(1L);

            assertThat(entry.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
            assertThat(entry.getAttempts()).isEqualTo(1);
            assertThat(entry.getLastError()).contains("SMTP error");
            assertThat(entry.getNextAttemptAt()).isAfter(OffsetDateTime.now());
            verify(outboxRepository).save(entry);
        }

        @Test
        void process_maxAttemptsExceeded_setsFailed() {
            NotificationOutbox entry = pendingEntry();
            entry.setAttempts(4);
            when(outboxRepository.findById(1L)).thenReturn(Optional.of(entry));
            doThrow(new RuntimeException("SMTP error")).when(emailService).send(any());

            processor.process(1L);

            assertThat(entry.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
            assertThat(entry.getAttempts()).isEqualTo(5);
        }
    }

    private NotificationOutbox pendingEntry() {
        NotificationOutbox entry = NotificationOutbox.builder()
                .recipientEmail("user@example.com")
                .subject("Тема")
                .htmlBody("<p>html</p>")
                .textBody("text")
                .eventType("request.created")
                .status(NotificationOutboxStatus.PENDING)
                .nextAttemptAt(OffsetDateTime.now().minusSeconds(1))
                .build();
        ReflectionTestUtils.setField(entry, "id", 1L);
        return entry;
    }

    private NotificationOutbox sentEntry() {
        NotificationOutbox entry = NotificationOutbox.builder()
                .status(NotificationOutboxStatus.SENT)
                .build();
        ReflectionTestUtils.setField(entry, "id", 1L);
        return entry;
    }
}
