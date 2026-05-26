package com.nullify.supportportal.controller.api;

import com.nullify.supportportal.dto.EmailIngestRequest;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.service.EmailIngestService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/email")
public class ApiEmailController {

    private final EmailIngestService emailIngestService;

    public ApiEmailController(EmailIngestService emailIngestService) {
        this.emailIngestService = emailIngestService;
    }

    @PostMapping("/inbound")
    public ResponseEntity<?> inbound(@Valid @RequestBody EmailIngestRequest request) {
        try {
            TicketResponse ticket = emailIngestService.ingest(request.rawMessage());
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "failed to parse email: " + ex.getMessage()));
        }
    }
}
