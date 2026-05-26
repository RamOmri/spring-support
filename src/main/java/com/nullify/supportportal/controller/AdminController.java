package com.nullify.supportportal.controller;

import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.dto.UserResponse;
import com.nullify.supportportal.dto.WebhookRequest;
import com.nullify.supportportal.dto.WebhookResponse;
import com.nullify.supportportal.service.AdminUserService;
import com.nullify.supportportal.service.BulkImportService;
import com.nullify.supportportal.service.WebhookService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService;
    private final WebhookService webhookService;
    private final BulkImportService bulkImportService;

    public AdminController(AdminUserService adminUserService,
                           WebhookService webhookService,
                           BulkImportService bulkImportService) {
        this.adminUserService = adminUserService;
        this.webhookService = webhookService;
        this.bulkImportService = bulkImportService;
    }

    @GetMapping("/users")
    public String listUsers(Authentication auth, Model model) {
        List<UserResponse> users = adminUserService.listAll();
        model.addAttribute("currentUser", auth.getName());
        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String setRole(@PathVariable long id, @RequestParam Role role) {
        adminUserService.setRole(id, role);
        return "redirect:/admin/users";
    }

    @GetMapping("/webhooks")
    public String listWebhooks(Authentication auth, Model model) {
        List<WebhookResponse> hooks = webhookService.list();
        model.addAttribute("currentUser", auth.getName());
        model.addAttribute("webhooks", hooks);
        if (!model.containsAttribute("webhookForm")) {
            model.addAttribute("webhookForm",
                    new WebhookRequest("", "", "ticket.status_changed", true));
        }
        return "admin/webhooks";
    }

    @PostMapping("/webhooks")
    public String createWebhook(@Validated @ModelAttribute("webhookForm") WebhookRequest form,
                                BindingResult bindingResult,
                                Authentication auth,
                                Model model) {
        if (bindingResult.hasErrors()) {
            return listWebhooks(auth, model);
        }
        webhookService.create(form);
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/webhooks/{id}/delete")
    public String deleteWebhook(@PathVariable long id) {
        webhookService.delete(id);
        return "redirect:/admin/webhooks";
    }

    @GetMapping("/import")
    public String importPage(Authentication auth, Model model) {
        model.addAttribute("currentUser", auth.getName());
        return "admin/import";
    }

    @PostMapping("/import/xml")
    public String importXml(@RequestParam("payload") String payload, Model model, Authentication auth) {
        try {
            List<Long> ids = bulkImportService.importTickets(payload);
            model.addAttribute("importResult", "Created " + ids.size() + " tickets (ids: " + ids + ")");
        } catch (Exception ex) {
            model.addAttribute("importError", ex.getMessage());
        }
        model.addAttribute("currentUser", auth.getName());
        return "admin/import";
    }

    @PostMapping("/import/csv")
    public String importCsv(@RequestParam("payload") String payload, Model model, Authentication auth) {
        try {
            List<Long> ids = bulkImportService.importCsv(payload);
            model.addAttribute("importResult", "Created " + ids.size() + " tickets (ids: " + ids + ")");
        } catch (Exception ex) {
            model.addAttribute("importError", ex.getMessage());
        }
        model.addAttribute("currentUser", auth.getName());
        return "admin/import";
    }
}
