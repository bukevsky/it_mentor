package com.example.it.mentor.service;

import com.example.it.mentor.config.NotificationProperties;
import com.example.it.mentor.event.request.MentoringRequestAcceptedEvent;
import com.example.it.mentor.event.request.MentoringRequestCreatedEvent;
import com.example.it.mentor.event.review.ReviewCreatedEvent;
import com.example.it.mentor.event.session.MentoringSessionCreatedEvent;
import com.example.it.mentor.service.notification.NotificationDispatcher;
import com.example.it.mentor.service.notification.NotificationEventListener;
import com.example.it.mentor.service.notification.NotificationOutboxService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

    @InjectMocks private NotificationEventListener listener;

    @Mock private NotificationProperties notificationProperties;
    @Mock private NotificationPreferencesService preferencesService;
    @Mock private NotificationDispatcher dispatcher;
    @Mock private NotificationOutboxService outboxService;

    private final MentoringRequestCreatedEvent requestEvent = new MentoringRequestCreatedEvent(10L, 2L);

    @BeforeEach
    void enabled() {
        lenient().when(notificationProperties.enabled()).thenReturn(true);
        lenient().when(preferencesService.shouldNotify(anyLong(), anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("handle — отключение уведомлений")
    class Disabled {

        @Test
        void handle_notificationsGloballyDisabled_noop() {
            when(notificationProperties.enabled()).thenReturn(false);

            listener.onRequestCreated(requestEvent);

            verifyNoInteractions(preferencesService, dispatcher, outboxService);
        }

        @Test
        void handle_userOptedOutOfRequests_noop() {
            when(preferencesService.shouldNotify(2L, "request")).thenReturn(false);

            listener.onRequestCreated(requestEvent);

            verifyNoInteractions(dispatcher, outboxService);
        }

        @Test
        void handle_userOptedOutOfSessions_noop() {
            MentoringSessionCreatedEvent event = new MentoringSessionCreatedEvent(5L, 3L);
            when(preferencesService.shouldNotify(3L, "session")).thenReturn(false);

            listener.onSessionCreated(event);

            verifyNoInteractions(dispatcher, outboxService);
        }

        @Test
        void handle_userOptedOutOfReviews_noop() {
            ReviewCreatedEvent event = new ReviewCreatedEvent(7L, 4L);
            when(preferencesService.shouldNotify(4L, "review")).thenReturn(false);

            listener.onReviewCreated(event);

            verifyNoInteractions(dispatcher, outboxService);
        }
    }

    @Nested
    @DisplayName("handle — happy path")
    class HappyPath {

        @Test
        void handle_dispatcherBuildsMessage_enqueues() {
            EmailMessage msg = new EmailMessage("a@b.com", "Тема", "<p>html</p>", "text");
            when(dispatcher.build(requestEvent)).thenReturn(Optional.of(msg));

            listener.onRequestCreated(requestEvent);

            verify(outboxService).enqueue(msg, "request.created");
        }

        @Test
        void handle_dispatcherReturnsEmpty_noEnqueue() {
            when(dispatcher.build(requestEvent)).thenReturn(Optional.empty());

            listener.onRequestCreated(requestEvent);

            verifyNoInteractions(outboxService);
        }

        @Test
        void handle_acceptedEvent_enqueues() {
            MentoringRequestAcceptedEvent event = new MentoringRequestAcceptedEvent(10L, 2L);
            EmailMessage msg = new EmailMessage("x@y.com", "Принята", "<p/>", "plain");
            when(dispatcher.build(event)).thenReturn(Optional.of(msg));

            listener.onRequestAccepted(event);

            verify(outboxService).enqueue(msg, "request.accepted");
        }
    }
}
