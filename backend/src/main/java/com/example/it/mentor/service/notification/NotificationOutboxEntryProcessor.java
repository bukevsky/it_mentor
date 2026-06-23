package com.example.it.mentor.service.notification;

import com.example.it.mentor.config.NotificationProperties;
import com.example.it.mentor.entity.NotificationOutbox;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import com.example.it.mentor.repository.NotificationOutboxRepository;
import com.example.it.mentor.service.EmailMessage;
import com.example.it.mentor.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxEntryProcessor {

    private final NotificationOutboxRepository outboxRepository;
    private final EmailService emailService;
    private final NotificationProperties notificationProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long entryId) {
        NotificationOutbox entry = outboxRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getStatus() != NotificationOutboxStatus.PENDING) {
            log.debug("Запись outbox пропущена: id={}, status={}, step={}",
                    entryId, entry == null ? null : entry.getStatus(), "notification_outbox_entry_skipped");
            return;
        }

        try {
            emailService.send(new EmailMessage(
                    entry.getRecipientEmail(),
                    entry.getSubject(),
                    entry.getHtmlBody(),
                    entry.getTextBody()
            ));
            entry.setStatus(NotificationOutboxStatus.SENT);
            entry.setSentAt(OffsetDateTime.now());
            log.info("Уведомление отправлено: id={}, to={}, eventType={}, attempts={}, sentAt={}, step={}",
                    entry.getId(), entry.getRecipientEmail(), entry.getEventType(), entry.getAttempts(),
                    entry.getSentAt(), "notification_sent");
        } catch (Exception e) {
            int attempts = entry.getAttempts() + 1;
            entry.setAttempts(attempts);
            entry.setLastError(truncate(e.getMessage(), 1000));
            if (attempts >= notificationProperties.outbox().maxAttempts()) {
                entry.setStatus(NotificationOutboxStatus.FAILED);
                log.error("Уведомление не доставлено (исчерпаны попытки): id={}, to={}, eventType={}, attempts={}, " +
                                "maxAttempts={}, step={}",
                        entry.getId(), entry.getRecipientEmail(), entry.getEventType(), attempts,
                        notificationProperties.outbox().maxAttempts(), "notification_failed", e);
            } else {
                long delaySeconds = (long) notificationProperties.outbox().retryDelaySeconds()
                        * (1L << Math.min(attempts - 1, 5));
                entry.setNextAttemptAt(OffsetDateTime.now().plusSeconds(delaySeconds));
                log.warn("Повтор уведомления запланирован: id={}, to={}, eventType={}, attempts={}, nextAt={}, step={}",
                        entry.getId(), entry.getRecipientEmail(), entry.getEventType(), attempts,
                        entry.getNextAttemptAt(), "notification_retry_scheduled");
            }
        }
        outboxRepository.save(entry);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
