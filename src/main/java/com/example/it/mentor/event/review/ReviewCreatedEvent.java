package com.example.it.mentor.event.review;

import com.example.it.mentor.event.NotificationEvent;

public record ReviewCreatedEvent(
        Long reviewId,
        Long recipientUserId
) implements NotificationEvent {
    @Override
    public String type() { return "review.created"; }
}
