package com.nullify.supportportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebhookRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "must be an http or https URL") String targetUrl,
        String eventType,
        Boolean enabled
) {}
