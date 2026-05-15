package com.example.it.mentor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notifications")
public record NotificationProperties(
        boolean enabled,
        String fromName,
        String baseUrl,
        String timezone,
        Outbox outbox
) {
    public record Outbox(
            int maxAttempts,
            int retryDelaySeconds,
            String scanCron
    ) {}
}
