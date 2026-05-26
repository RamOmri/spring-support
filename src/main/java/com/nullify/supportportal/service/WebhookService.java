package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Webhook;
import com.nullify.supportportal.dto.WebhookRequest;
import com.nullify.supportportal.dto.WebhookResponse;
import com.nullify.supportportal.repository.WebhookRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class WebhookService {

    private final WebhookRepository webhookRepository;

    public WebhookService(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Transactional
    public WebhookResponse create(WebhookRequest request) {
        Webhook hook = new Webhook();
        hook.setName(request.name());
        hook.setTargetUrl(request.targetUrl());
        hook.setEventType(request.eventType() == null ? "ticket.status_changed" : request.eventType());
        hook.setEnabled(request.enabled() == null ? true : request.enabled());
        return WebhookResponse.from(webhookRepository.save(hook));
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> list() {
        return webhookRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(WebhookResponse::from)
                .toList();
    }

    @Transactional
    public void delete(long id) {
        Webhook hook = webhookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("webhook not found: " + id));
        webhookRepository.delete(hook);
    }
}
