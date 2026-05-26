package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Webhook;

import java.time.Instant;

public record WebhookResponse(
        long id,
        String name,
        String targetUrl,
        String eventType,
        boolean enabled,
        Instant createdAt
) {
    public static WebhookResponse from(Webhook w) {
        return new WebhookResponse(w.getId(), w.getName(), w.getTargetUrl(),
                w.getEventType(), w.isEnabled(), w.getCreatedAt());
    }
}
