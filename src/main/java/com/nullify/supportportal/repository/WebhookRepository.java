package com.nullify.supportportal.repository;

import com.nullify.supportportal.domain.Webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    List<Webhook> findByEnabledTrueAndEventType(String eventType);

    List<Webhook> findAllByOrderByCreatedAtDesc();
}
