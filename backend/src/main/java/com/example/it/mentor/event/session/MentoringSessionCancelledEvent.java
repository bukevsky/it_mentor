package com.example.it.mentor.event.session;

import com.example.it.mentor.event.NotificationEvent;

public record MentoringSessionCancelledEvent(
        Long sessionId,
        Long recipientUserId
) implements NotificationEvent {
    @Override
    public String type() { return "session.cancelled"; }
}
