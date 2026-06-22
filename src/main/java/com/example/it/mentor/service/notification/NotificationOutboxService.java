package com.example.it.mentor.service.notification;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.notification.OutboxEntryResponse;
import com.example.it.mentor.entity.NotificationOutbox;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import com.example.it.mentor.repository.NotificationOutboxRepository;
import com.example.it.mentor.service.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxEntryProcessor entryProcessor;

    @Transactional
    public void enqueue(EmailMessage message, String eventType) {
        NotificationOutbox entry = NotificationOutbox.builder()
                .recipientEmail(message.to())
                .subject(message.subject())
                .htmlBody(message.htmlBody())
                .textBody(message.textBody())
                .eventType(eventType)
                .status(NotificationOutboxStatus.PENDING)
                .nextAttemptAt(OffsetDateTime.now())
                .build();
        outboxRepository.save(entry);
        log.info("Письмо поставлено в очередь: id={}, to={}, eventType={}, status={}, step={}",
                entry.getId(), message.to(), eventType, entry.getStatus(), "notification_outbox_enqueued");
    }

    @Scheduled(cron = "${app.notifications.outbox.scan-cron}")
    public void processBatch() {
        List<NotificationOutbox> pending = outboxRepository
                .findTop50ByStatusAndNextAttemptAtBefore(NotificationOutboxStatus.PENDING, OffsetDateTime.now());

        if (pending.isEmpty()) {
            log.debug("Очередь уведомлений пуста: step={}", "notification_outbox_empty");
            return;
        }

        log.debug("Обработка очереди уведомлений: count={}, step={}", pending.size(), "notification_outbox_batch_started");

        for (NotificationOutbox entry : pending) {
            entryProcessor.process(entry.getId());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<OutboxEntryResponse> getOutbox(NotificationOutboxStatus status, Pageable pageable) {
        Page<NotificationOutbox> page = status != null
                ? outboxRepository.findByStatus(status, pageable)
                : outboxRepository.findAll(pageable);
        log.debug("Outbox уведомлений загружен: status={}, page={}, size={}, resultCount={}, total={}, step={}",
                status, pageable.getPageNumber(), pageable.getPageSize(), page.getNumberOfElements(),
                page.getTotalElements(), "notification_outbox_loaded");
        return PagedResponse.from(page.map(this::toResponse));
    }

    private OutboxEntryResponse toResponse(NotificationOutbox e) {
        return new OutboxEntryResponse(
                e.getId(),
                e.getRecipientEmail(),
                e.getSubject(),
                e.getEventType(),
                e.getStatus(),
                e.getAttempts(),
                e.getLastError(),
                e.getNextAttemptAt(),
                e.getSentAt(),
                e.getCreatedAt()
        );
    }
}
