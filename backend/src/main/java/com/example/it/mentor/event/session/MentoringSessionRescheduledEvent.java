package com.example.it.mentor.event.session;

import com.example.it.mentor.event.NotificationEvent;

public record MentoringSessionRescheduledEvent(
        Long sessionId,
        Long recipientUserId
) implements NotificationEvent {
    @Override
    public String type() { return "session.rescheduled"; }
}
