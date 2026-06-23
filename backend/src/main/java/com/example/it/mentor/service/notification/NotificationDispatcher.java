package com.example.it.mentor.service.notification;

import com.example.it.mentor.config.NotificationProperties;
import com.example.it.mentor.entity.*;
import com.example.it.mentor.event.NotificationEvent;
import com.example.it.mentor.event.request.*;
import com.example.it.mentor.event.review.ReviewCreatedEvent;
import com.example.it.mentor.event.session.*;
import com.example.it.mentor.repository.*;
import com.example.it.mentor.service.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("ru"));

    private final SpringTemplateEngine emailTemplateEngine;
    private final NotificationProperties notificationProperties;
    private final UserRepository userRepository;
    private final MentoringRequestRepository requestRepository;
    private final MentoringSessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;

    public Optional<EmailMessage> build(NotificationEvent event) {
        try {
            return switch (event) {
                case MentoringRequestCreatedEvent e -> buildRequestCreated(e);
                case MentoringRequestAcceptedEvent e -> buildRequestAccepted(e);
                case MentoringRequestRejectedEvent e -> buildRequestRejected(e);
                case MentoringRequestNeedsClarificationEvent e -> buildRequestNeedsClarification(e);
                case MentoringRequestCancelledEvent e -> buildRequestCancelled(e);
                case MentoringRequestCompletedEvent e -> buildRequestCompleted(e);
                case MentoringSessionCreatedEvent e -> buildSessionCreated(e);
                case MentoringSessionRescheduledEvent e -> buildSessionRescheduled(e);
                case MentoringSessionCancelledEvent e -> buildSessionCancelled(e);
                case ReviewCreatedEvent e -> buildReviewCreated(e);
                default -> {
                    log.warn("Неизвестный тип события: eventType={}, step={}",
                            event.type(), "notification_unknown_event");
                    yield Optional.empty();
                }
            };
        } catch (Exception e) {
            log.error("Ошибка формирования письма: eventType={}, recipientUserId={}, error={}, step={}",
                    event.type(), event.recipientUserId(), e.getMessage(), "notification_message_build_failed", e);
            return Optional.empty();
        }
    }

    private Optional<EmailMessage> buildRequestCreated(MentoringRequestCreatedEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());
        String recipientName = resolveRecipientName(recipient);
        String senderName = resolveSenderName(request, recipient.getId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", recipientName);
        model.put("senderName", senderName);
        model.put("message", request.getMessage());
        model.put("actionUrl", notificationProperties.baseUrl() + "/requests/" + request.getId());
        return Optional.of(render(recipient.getEmail(), "Новый запрос на менторство",
                "mentoring_request_created", model));
    }

    private Optional<EmailMessage> buildRequestAccepted(MentoringRequestAcceptedEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());
        String mentorName = fullName(request.getMentorProfile());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("mentorName", mentorName);
        model.put("actionUrl", notificationProperties.baseUrl() + "/requests/" + request.getId());
        return Optional.of(render(recipient.getEmail(), "Заявка на менторство принята",
                "mentoring_request_accepted", model));
    }

    private Optional<EmailMessage> buildRequestRejected(MentoringRequestRejectedEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("reason", request.getReason());
        model.put("actionUrl", notificationProperties.baseUrl() + "/mentors");
        return Optional.of(render(recipient.getEmail(), "Заявка на менторство отклонена",
                "mentoring_request_rejected", model));
    }

    private Optional<EmailMessage> buildRequestNeedsClarification(MentoringRequestNeedsClarificationEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("clarificationNote", request.getClarificationNote());
        model.put("actionUrl", notificationProperties.baseUrl() + "/requests/" + request.getId());
        return Optional.of(render(recipient.getEmail(), "Требуется уточнение по заявке",
                "mentoring_request_needs_clarification", model));
    }

    private Optional<EmailMessage> buildRequestCancelled(MentoringRequestCancelledEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("actionUrl", notificationProperties.baseUrl() + "/requests");
        return Optional.of(render(recipient.getEmail(), "Заявка на менторство отменена",
                "mentoring_request_cancelled", model));
    }

    private Optional<EmailMessage> buildRequestCompleted(MentoringRequestCompletedEvent event) {
        MentoringRequest request = loadRequest(event.requestId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("actionUrl", notificationProperties.baseUrl() + "/requests/" + request.getId());
        return Optional.of(render(recipient.getEmail(), "Менторинг завершён",
                "mentoring_request_completed", model));
    }

    private Optional<EmailMessage> buildSessionCreated(MentoringSessionCreatedEvent event) {
        MentoringSession session = loadSession(event.sessionId());
        User recipient = loadUser(event.recipientUserId());
        String counterpartyName = resolveCounterpartyName(session, recipient.getId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("scheduledAt", formatDateTime(session));
        model.put("durationMinutes", session.getDurationMinutes());
        model.put("counterpartyName", counterpartyName);
        model.put("actionUrl", notificationProperties.baseUrl() + "/sessions/" + session.getId());
        return Optional.of(render(recipient.getEmail(), "Назначена сессия менторинга",
                "mentoring_session_created", model));
    }

    private Optional<EmailMessage> buildSessionRescheduled(MentoringSessionRescheduledEvent event) {
        MentoringSession session = loadSession(event.sessionId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("scheduledAt", formatDateTime(session));
        model.put("durationMinutes", session.getDurationMinutes());
        model.put("reason", session.getRescheduleReason());
        model.put("actionUrl", notificationProperties.baseUrl() + "/sessions/" + session.getId());
        return Optional.of(render(recipient.getEmail(), "Сессия менторинга перенесена",
                "mentoring_session_rescheduled", model));
    }

    private Optional<EmailMessage> buildSessionCancelled(MentoringSessionCancelledEvent event) {
        MentoringSession session = loadSession(event.sessionId());
        User recipient = loadUser(event.recipientUserId());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("reason", session.getCancelReason());
        model.put("actionUrl", notificationProperties.baseUrl() + "/sessions");
        return Optional.of(render(recipient.getEmail(), "Сессия менторинга отменена",
                "mentoring_session_cancelled", model));
    }

    private Optional<EmailMessage> buildReviewCreated(ReviewCreatedEvent event) {
        Review review = reviewRepository.findById(event.reviewId())
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден: " + event.reviewId()));
        User recipient = loadUser(event.recipientUserId());
        String reviewerName = resolveRecipientName(review.getReviewer());

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", resolveRecipientName(recipient));
        model.put("reviewerName", reviewerName);
        model.put("rating", review.getRating());
        model.put("comment", review.getComment());
        model.put("actionUrl", notificationProperties.baseUrl() + "/profile/mentor/reviews");
        return Optional.of(render(recipient.getEmail(), "Новый отзыв о вас",
                "review_created", model));
    }

    private EmailMessage render(String to, String subject, String templateName,
                                Map<String, Object> model) {
        Context ctx = new Context(new Locale("ru"));
        ctx.setVariables(model);
        String html = emailTemplateEngine.process(templateName + ".html", ctx);
        String text = emailTemplateEngine.process(templateName + ".txt", ctx);
        log.debug("Письмо сформировано по шаблону: to={}, subject={}, template={}, step={}",
                to, subject, templateName, "notification_message_built");
        return new EmailMessage(to, subject, html, text);
    }

    private MentoringRequest loadRequest(Long requestId) {
        return requestRepository.findWithProfilesById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена: " + requestId));
    }

    private MentoringSession loadSession(Long sessionId) {
        return sessionRepository.findWithParticipantsById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Сессия не найдена: " + sessionId));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
    }

    private String resolveRecipientName(User user) {
        Long userId = user.getId();
        return studentProfileRepository.findByUserId(userId)
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElseGet(() -> mentorProfileRepository.findByUserId(userId)
                        .map(p -> p.getFirstName() + " " + p.getLastName())
                        .orElse(user.getEmail()));
    }

    private String resolveSenderName(MentoringRequest request, Long recipientUserId) {
        Long mentorUserId = request.getMentorProfile().getUser().getId();
        if (recipientUserId.equals(mentorUserId)) {
            return fullName(request.getStudentProfile());
        }
        return fullName(request.getMentorProfile());
    }

    private String resolveCounterpartyName(MentoringSession session, Long recipientUserId) {
        if (recipientUserId.equals(session.getStudentUser().getId())) {
            return resolveRecipientName(session.getMentorUser());
        }
        return resolveRecipientName(session.getStudentUser());
    }

    private String fullName(StudentProfile p) { return p.getFirstName() + " " + p.getLastName(); }
    private String fullName(MentorProfile p) { return p.getFirstName() + " " + p.getLastName(); }

    private String formatDateTime(MentoringSession session) {
        return session.getScheduledAt()
                .atZoneSameInstant(ZoneId.of(notificationProperties.timezone()))
                .format(DATE_FMT);
    }
}
