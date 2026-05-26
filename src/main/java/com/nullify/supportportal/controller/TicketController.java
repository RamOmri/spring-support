package com.nullify.supportportal.controller;

import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.domain.TicketPriority;
import com.nullify.supportportal.domain.TicketStatus;
import com.nullify.supportportal.dto.AttachmentResponse;
import com.nullify.supportportal.dto.CommentResponse;
import com.nullify.supportportal.dto.CreateCommentRequest;
import com.nullify.supportportal.dto.CreateTicketRequest;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.dto.UpdateTicketStatusRequest;
import com.nullify.supportportal.dto.UserResponse;
import com.nullify.supportportal.service.AdminUserService;
import com.nullify.supportportal.service.AttachmentService;
import com.nullify.supportportal.service.CurrentUserService;
import com.nullify.supportportal.service.TicketService;

import org.springframework.security.access.AccessDeniedException;
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

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final AttachmentService attachmentService;
    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    public TicketController(TicketService ticketService,
                            AttachmentService attachmentService,
                            AdminUserService adminUserService,
                            CurrentUserService currentUserService) {
        this.ticketService = ticketService;
        this.attachmentService = attachmentService;
        this.adminUserService = adminUserService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        List<TicketResponse> tickets = ticketService.listVisibleAsResponses(auth);
        model.addAttribute("currentUser", auth.getName());
        model.addAttribute("tickets", tickets);
        return "tickets/list";
    }

    @GetMapping("/new")
    public String newTicketForm(Authentication auth, Model model) {
        model.addAttribute("currentUser", auth.getName());
        if (!model.containsAttribute("ticketForm")) {
            model.addAttribute("ticketForm",
                    new CreateTicketRequest("", "", TicketPriority.NORMAL));
        }
        model.addAttribute("priorities", TicketPriority.values());
        return "tickets/new";
    }

    @PostMapping
    public String create(@Validated @ModelAttribute("ticketForm") CreateTicketRequest form,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", auth.getName());
            model.addAttribute("priorities", TicketPriority.values());
            return "tickets/new";
        }
        TicketResponse created = ticketService.create(form, auth);
        return "redirect:/tickets/" + created.id();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable long id, Authentication auth, Model model) {
        try {
            TicketResponse ticket = ticketService.getVisibleAsResponse(id, auth);
            List<CommentResponse> comments = ticketService.listComments(id, auth);
            List<AttachmentResponse> attachments = attachmentService.listForTicket(id, auth);
            var actor = currentUserService.require(auth);
            boolean isStaff = currentUserService.isStaff(actor);
            boolean isAdmin = actor.getRole() == Role.ADMIN;
            List<UserResponse> agents = List.of();
            if (isAdmin) {
                agents = adminUserService.listAll().stream()
                        .filter(u -> u.role() == Role.AGENT || u.role() == Role.ADMIN)
                        .toList();
            }
            model.addAttribute("currentUser", auth.getName());
            model.addAttribute("ticket", ticket);
            model.addAttribute("comments", comments);
            model.addAttribute("attachments", attachments);
            model.addAttribute("commentForm", new CreateCommentRequest(""));
            model.addAttribute("statuses", TicketStatus.values());
            model.addAttribute("isStaff", isStaff);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("agents", agents);
            return "tickets/view";
        } catch (NoSuchElementException ex) {
            return "redirect:/tickets?notFound";
        } catch (AccessDeniedException ex) {
            return "redirect:/tickets?forbidden";
        }
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable long id,
                             @Validated @ModelAttribute("commentForm") CreateCommentRequest form,
                             BindingResult bindingResult,
                             Authentication auth) {
        if (bindingResult.hasErrors()) {
            return "redirect:/tickets/" + id + "?commentInvalid";
        }
        ticketService.addComment(id, form, auth);
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable long id,
                               @Validated @ModelAttribute UpdateTicketStatusRequest form,
                               Authentication auth) {
        ticketService.updateStatus(id, form.status(), auth);
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignAgent(@PathVariable long id,
                              @org.springframework.web.bind.annotation.RequestParam("agentId") long agentId,
                              Authentication auth) {
        ticketService.assignAgent(id, agentId, auth);
        return "redirect:/tickets/" + id;
    }

    @PostMapping(value = "/{id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadAttachment(@PathVariable long id,
                                   @org.springframework.web.bind.annotation.RequestParam("file")
                                   org.springframework.web.multipart.MultipartFile file,
                                   Authentication auth) throws java.io.IOException {
        attachmentService.upload(id, file, auth);
        return "redirect:/tickets/" + id;
    }
}
