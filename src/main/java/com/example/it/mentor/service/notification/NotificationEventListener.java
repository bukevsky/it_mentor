package com.example.it.mentor.service.notification;

import com.example.it.mentor.config.NotificationProperties;
import com.example.it.mentor.event.NotificationEvent;
import com.example.it.mentor.service.NotificationPreferencesService;
import com.example.it.mentor.event.request.*;
import com.example.it.mentor.event.review.ReviewCreatedEvent;
import com.example.it.mentor.event.session.*;
import com.example.it.mentor.service.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationProperties notificationProperties;
    private final NotificationPreferencesService preferencesService;
    private final NotificationDispatcher dispatcher;
    private final NotificationOutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestCreated(MentoringRequestCreatedEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestAccepted(MentoringRequestAcceptedEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestRejected(MentoringRequestRejectedEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestNeedsClarification(MentoringRequestNeedsClarificationEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestCancelled(MentoringRequestCancelledEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onRequestCompleted(MentoringRequestCompletedEvent event) {
        handle(event, "request");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onSessionCreated(MentoringSessionCreatedEvent event) {
        handle(event, "session");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onSessionRescheduled(MentoringSessionRescheduledEvent event) {
        handle(event, "session");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onSessionCancelled(MentoringSessionCancelledEvent event) {
        handle(event, "session");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void onReviewCreated(ReviewCreatedEvent event) {
        handle(event, "review");
    }

    private void handle(NotificationEvent event, String category) {
        if (!notificationProperties.enabled()) {
            log.debug("Уведомление пропущено: eventType={}, category={}, recipientUserId={}, reason={}, step={}",
                    event.type(), category, event.recipientUserId(), "notifications_disabled", "notification_skipped");
            return;
        }

        if (!preferencesService.shouldNotify(event.recipientUserId(), category)) {
            log.debug("Уведомление отключено пользователем: userId={}, eventType={}, category={}, step={}",
                    event.recipientUserId(), event.type(), category, "notification_skipped_by_preferences");
            return;
        }

        Optional<EmailMessage> message = dispatcher.build(event);
        message.ifPresentOrElse(
                msg -> {
                    outboxService.enqueue(msg, event.type());
                    log.debug("Уведомление поставлено в outbox: userId={}, eventType={}, category={}, step={}",
                            event.recipientUserId(), event.type(), category, "notification_enqueued");
                },
                () -> log.warn("Письмо не сформировано: eventType={}, category={}, recipientUserId={}, step={}",
                        event.type(), category, event.recipientUserId(), "notification_message_not_built")
        );
    }
}
