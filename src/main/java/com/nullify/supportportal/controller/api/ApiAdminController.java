package com.nullify.supportportal.controller.api;

import com.nullify.supportportal.dto.UpdateUserRoleRequest;
import com.nullify.supportportal.dto.UserResponse;
import com.nullify.supportportal.dto.WebhookRequest;
import com.nullify.supportportal.dto.WebhookResponse;
import com.nullify.supportportal.service.AdminUserService;
import com.nullify.supportportal.service.BulkImportService;
import com.nullify.supportportal.service.WebhookService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class ApiAdminController {

    private final AdminUserService adminUserService;
    private final WebhookService webhookService;
    private final BulkImportService bulkImportService;

    public ApiAdminController(AdminUserService adminUserService,
                              WebhookService webhookService,
                              BulkImportService bulkImportService) {
        this.adminUserService = adminUserService;
        this.webhookService = webhookService;
        this.bulkImportService = bulkImportService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return adminUserService.listAll();
    }

    @PostMapping("/users/{id}/role")
    public UserResponse setRole(@PathVariable long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return adminUserService.setRole(id, request.role());
    }

    @GetMapping("/webhooks")
    public List<WebhookResponse> listWebhooks() {
        return webhookService.list();
    }

    @PostMapping("/webhooks")
    public WebhookResponse createWebhook(@Valid @RequestBody WebhookRequest request) {
        return webhookService.create(request);
    }

    @DeleteMapping("/webhooks/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable long id) {
        webhookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import/xml", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, "text/plain"})
    public ResponseEntity<?> importXml(@RequestBody String xmlPayload) {
        try {
            List<Long> ids = bulkImportService.importTickets(xmlPayload);
            return ResponseEntity.ok(Map.of("createdIds", ids));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/import/csv", consumes = {"text/csv", MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<?> importCsv(@RequestBody String csvPayload) {
        try {
            List<Long> ids = bulkImportService.importCsv(csvPayload);
            return ResponseEntity.ok(Map.of("createdIds", ids));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
