package com.example.it.mentor.event.request;

import com.example.it.mentor.event.NotificationEvent;

public record MentoringRequestRejectedEvent(
        Long requestId,
        Long recipientUserId
) implements NotificationEvent {
    @Override
    public String type() { return "request.rejected"; }
}
