package com.example.it.mentor.dto.notification;

public record NotificationPreferencesResponse(
        boolean emailRequestEvents,
        boolean emailSessionEvents,
        boolean emailReviewEvents
) {}
