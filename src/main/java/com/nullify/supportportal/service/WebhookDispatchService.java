package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Webhook;
import com.nullify.supportportal.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class WebhookDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebhookRepository webhookRepository;
    private final HttpClient httpClient;

    public WebhookDispatchService(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Async
    public void dispatch(String eventType, String jsonPayload) {
        List<Webhook> hooks = webhookRepository.findByEnabledTrueAndEventType(eventType);
        for (Webhook hook : hooks) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(hook.getTargetUrl()))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("X-Portal-Event", eventType)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                log.info("Webhook {} delivered to {} status={}", hook.getId(), hook.getTargetUrl(), response.statusCode());
            } catch (Exception ex) {
                log.warn("Webhook {} delivery failed: {}", hook.getId(), ex.getMessage());
            }
        }
    }
}
