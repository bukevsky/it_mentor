package com.example.it.mentor.dto.notification;

public record UpdateNotificationPreferencesRequest(
        Boolean emailRequestEvents,
        Boolean emailSessionEvents,
        Boolean emailReviewEvents
) {}
